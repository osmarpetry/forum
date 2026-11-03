package com.forum.forum.api;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.boot.admin.client.enabled=false")
@AutoConfigureMockMvc
class TopicsOwnerTest {

    private static final AtomicInteger COUNTER = new AtomicInteger();
    private static final String PASSWORD = "secret1";
    private static final String UPDATE = "{\"title\":\"Hijacked title\",\"message\":\"hijacked long message\"}";

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired TopicRepository topics;
    @Autowired AnswerRepository answers;
    @Autowired CourseRepository courses;
    @Autowired PasswordEncoder encoder;

    private User newUser() {
        User u = new User();
        u.setName("Owner " + COUNTER.incrementAndGet());
        u.setEmail("owner" + COUNTER.get() + "@test.io");
        u.setPassword(encoder.encode(PASSWORD));
        return users.save(u);
    }

    private Topic newTopic(User author) {
        Topic t = new Topic("Topic title", "topic message long", courses.findAll().get(0));
        t.setAuthor(author);
        return topics.save(t);
    }

    private String token(User user) throws Exception {
        String body = mvc.perform(post("/auth").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + user.getEmail() + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + body.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }

    @Test
    void nonAuthorCannotUpdate() throws Exception {
        Topic topic = newTopic(newUser());

        mvc.perform(put("/topics/" + topic.getId()).header("Authorization", token(newUser()))
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATE))
                .andExpect(status().isForbidden());

        assertEquals("Topic title", topics.findById(topic.getId()).orElseThrow().getTitle());
    }

    @Test
    void nonAuthorCannotDelete() throws Exception {
        User author = newUser();
        Topic topic = newTopic(author);
        Answer a = new Answer();
        a.setMessage("a reply that must survive");
        a.setTopic(topic);
        a.setAuthor(author);
        Long answerId = answers.save(a).getId();

        mvc.perform(delete("/topics/" + topic.getId()).header("Authorization", token(newUser())))
                .andExpect(status().isForbidden());

        assertTrue(topics.findById(topic.getId()).isPresent());
        assertTrue(answers.findById(answerId).isPresent());
    }

    @Test
    void authorCanUpdateAndDelete() throws Exception {
        User author = newUser();
        Topic topic = newTopic(author);
        String token = token(author);

        mvc.perform(put("/topics/" + topic.getId()).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATE))
                .andExpect(status().isOk());
        assertEquals("Hijacked title", topics.findById(topic.getId()).orElseThrow().getTitle());

        mvc.perform(delete("/topics/" + topic.getId()).header("Authorization", token))
                .andExpect(status().isOk());
        assertTrue(topics.findById(topic.getId()).isEmpty());
    }

    @Test
    void missingTopicGives404() throws Exception {
        mvc.perform(delete("/topics/999999").header("Authorization", token(newUser())))
                .andExpect(status().isNotFound());
    }

    @Test
    void registerSetsTheAuthor() throws Exception {
        User author = newUser();
        String location = mvc.perform(post("/topics").header("Authorization", token(author))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Api topic\",\"message\":\"valid long message\",\"courseName\":\"Spring Boot\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");

        mvc.perform(get(location)).andExpect(status().isOk())
                .andExpect(jsonPath("$.authorName").value(author.getName()));
    }

    @Test
    void detailOfTopicWithoutAuthorIsOk() throws Exception {
        Topic t = topics.save(new Topic("No author topic", "Created before authors were set", courses.findByName("Spring Boot")));
        mvc.perform(get("/topics/" + t.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("No author topic"))
                .andExpect(jsonPath("$.authorName").doesNotExist());
    }
}
