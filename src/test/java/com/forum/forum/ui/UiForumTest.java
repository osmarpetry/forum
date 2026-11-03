package com.forum.forum.ui;

import com.forum.forum.model.Answer;
import com.forum.forum.model.Topic;
import com.forum.forum.model.User;
import com.forum.forum.repository.AnswerRepository;
import com.forum.forum.repository.CourseRepository;
import com.forum.forum.repository.TopicRepository;
import com.forum.forum.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
}
