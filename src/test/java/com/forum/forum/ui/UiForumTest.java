package com.forum.forum.ui;

import com.forum.forum.model.Answer;
import com.forum.forum.model.Topic;
import com.forum.forum.model.User;
import com.forum.forum.repository.AnswerRepository;
import com.forum.forum.repository.CourseRepository;
import com.forum.forum.repository.TopicRepository;
import com.forum.forum.repository.UserRepository;
import com.forum.forum.model.TopicStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.boot.admin.client.enabled=false")
@AutoConfigureMockMvc
class UiForumTest {

    private static final AtomicInteger COUNTER = new AtomicInteger();
    private static final String PASSWORD = "secret1";

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired TopicRepository topics;
    @Autowired AnswerRepository answers;
    @Autowired CourseRepository courses;
    @Autowired PasswordEncoder encoder;

    private User newUser() {
        User u = new User();
        u.setName("User " + COUNTER.incrementAndGet());
        u.setEmail("forum" + COUNTER.get() + "@test.io");
        u.setPassword(encoder.encode(PASSWORD));
        return users.save(u);
    }

    private Topic newTopic(User author) {
        Topic t = new Topic("Topic title", "topic message long", courses.findAll().get(0));
        t.setAuthor(author);
        return topics.save(t);
    }

    private Answer newAnswer(Topic topic, User author, String message) {
        Answer a = new Answer();
        a.setMessage(message);
        a.setTopic(topic);
        a.setAuthor(author);
        return answers.save(a);
    }

    @Test
    void deleteTopicWithRepliesThroughUi() throws Exception {
        User author = newUser();
        Topic topic = newTopic(author);
        newAnswer(topic, author, "a reply that must go with the topic");

        mvc.perform(delete("/ui/topics/" + topic.getId()).with(csrf()).with(user(author)))
                .andExpect(status().isOk())
                .andExpect(header().string("HX-Redirect", "/ui/topics"));
    }

    @Test
    void deleteTopicWithRepliesThroughApi() throws Exception {
        User author = newUser();
        Topic topic = newTopic(author);
        newAnswer(topic, author, "a reply that must go with the topic");
        String body = mvc.perform(post("/auth").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + author.getEmail() + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = body.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

        mvc.perform(delete("/topics/" + topic.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void replyWhenLoggedInShowsTheMessageAndOpensTheTopic() throws Exception {
        User author = newUser();
        User replier = newUser();
        Topic topic = newTopic(author);

        mvc.perform(post("/ui/topics/" + topic.getId() + "/answers").with(csrf()).with(user(replier))
                        .param("message", "FIRST-REPLY-123 is the answer"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("FIRST-REPLY-123")));

        assertEquals(TopicStatus.NOT_RESOLVED, topics.findById(topic.getId()).orElseThrow().getStatus());
    }

    @Test
    void replyThatIsTooShortGives422AndError() throws Exception {
        User author = newUser();
        Topic topic = newTopic(author);

        mvc.perform(post("/ui/topics/" + topic.getId() + "/answers").with(csrf()).with(user(author))
                        .param("message", "short"))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("Message must have at least 10 characters")));
    }

    @Test
    void replyWithoutLoginGoesToLogin() throws Exception {
        Topic topic = newTopic(newUser());

        mvc.perform(post("/ui/topics/" + topic.getId() + "/answers").with(csrf())
                        .param("message", "a reply from nobody"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/ui/login"));
    }

    @Test
    void replyToMissingTopicGives404() throws Exception {
        mvc.perform(post("/ui/topics/999999/answers").with(csrf()).with(user(newUser()))
                        .param("message", "a reply to nothing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void authorMarksSolutionAndTopicIsResolved() throws Exception {
        User author = newUser();
        User replier = newUser();
        Topic topic = newTopic(author);
        Answer answer = newAnswer(topic, replier, "the good answer");

        mvc.perform(post("/ui/answers/" + answer.getId() + "/solution").with(csrf()).with(user(author)))
                .andExpect(status().isOk());

        assertEquals(TopicStatus.RESOLVED, topics.findById(topic.getId()).orElseThrow().getStatus());
        mvc.perform(get("/topics/" + topic.getId()))
                .andExpect(content().string(containsString("RESOLVED")));
    }

    @Test
    void anotherUserCannotMarkTheSolution() throws Exception {
        User author = newUser();
        User other = newUser();
        Topic topic = newTopic(author);
        Answer answer = newAnswer(topic, other, "my own answer");

        mvc.perform(post("/ui/answers/" + answer.getId() + "/solution").with(csrf()).with(user(other)))
                .andExpect(status().isForbidden());

        assertEquals(TopicStatus.NOT_ANSWERED, topics.findById(topic.getId()).orElseThrow().getStatus());
    }

    @Test
    void markingAMissingAnswerGives404() throws Exception {
        mvc.perform(post("/ui/answers/999999/solution").with(csrf()).with(user(newUser())))
                .andExpect(status().isNotFound());
    }

    @Test
    void signUpThenLogIn() throws Exception {
        mvc.perform(get("/ui/signup")).andExpect(status().isOk());

        mvc.perform(post("/ui/signup").with(csrf())
                        .param("name", "New Person").param("email", "new.person@test.io").param("password", "secret1"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/ui/login?registered"));

        mvc.perform(formLogin("/ui/login").user("new.person@test.io").password("secret1"))
                .andExpect(redirectedUrl("/ui/topics"));
    }

    @Test
    void signUpWithAnExistingEmailGives422() throws Exception {
        mvc.perform(post("/ui/signup").with(csrf())
                        .param("name", "Copy Cat").param("email", "aluno@email.com").param("password", "secret1"))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("Email is already registered")));
    }

    @Test
    void signUpWithAPasswordOver72BytesGives422() throws Exception {
        mvc.perform(post("/ui/signup").with(csrf())
                        .param("name", "Long Pass").param("email", "long.pass@test.io").param("password", "a".repeat(80)))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("Password must have at most 72 bytes")));
        mvc.perform(post("/ui/signup").with(csrf())
                        .param("name", "Long Pass").param("email", "long.pass2@test.io").param("password", "é".repeat(40)))
                .andExpect(status().is(422));
    }

    @Test
    void signUpWithBadDataGives422() throws Exception {
        mvc.perform(post("/ui/signup").with(csrf())
                        .param("name", "").param("email", "not-an-email").param("password", "123"))
                .andExpect(status().is(422));
    }

    @Test
    void onlyTheAuthorCanOpenEditOrChangeOrDeleteATopic() throws Exception {
        User author = newUser();
        User other = newUser();
        Topic topic = newTopic(author);
        String url = "/ui/topics/" + topic.getId();

        mvc.perform(get(url + "/edit").with(user(other))).andExpect(status().isForbidden());
        mvc.perform(put(url).with(csrf()).with(user(other))
                .param("title", "Hijacked title").param("message", "hijacked long message"))
                .andExpect(status().isForbidden());
        mvc.perform(delete(url).with(csrf()).with(user(other))).andExpect(status().isForbidden());
        assertEquals("Topic title", topics.findById(topic.getId()).orElseThrow().getTitle());

        mvc.perform(get(url + "/edit").with(user(author))).andExpect(status().isOk());
        mvc.perform(put(url).with(csrf()).with(user(author))
                .param("title", "Author title").param("message", "author long message"))
                .andExpect(status().isOk());
        mvc.perform(delete(url).with(csrf()).with(user(author)))
                .andExpect(status().isOk())
                .andExpect(header().string("HX-Redirect", "/ui/topics"));
    }

    @Test
    void aTopicWithoutAuthorHasNoOwner() throws Exception {
        Topic topic = newTopic(null);

        mvc.perform(delete("/ui/topics/" + topic.getId()).with(csrf()).with(user(newUser())))
                .andExpect(status().isForbidden());
    }

    @Test
    void detailShowsEditAndDeleteOnlyToTheAuthor() throws Exception {
        User author = newUser();
        Topic topic = newTopic(author);
        String url = "/ui/topics/" + topic.getId();

        mvc.perform(get(url).with(user(author)))
                .andExpect(content().string(containsString("hx-delete")));
        mvc.perform(get(url).with(user(newUser())))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("hx-delete"))));
    }
}
