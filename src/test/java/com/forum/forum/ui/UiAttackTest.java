package com.forum.forum.ui;

import com.forum.forum.model.Topic;
import com.forum.forum.repository.CourseRepository;
import com.forum.forum.repository.TopicRepository;
import com.forum.forum.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.boot.admin.client.enabled=false")
@AutoConfigureMockMvc
class UiAttackTest {
    @Autowired MockMvc mvc;
    @Autowired TopicRepository topics;
    @Autowired CourseRepository courses;
    @Autowired UserRepository users;

    private String newTopicId() {
        Topic topic = new Topic("Attack topic", "valid long message", courses.findAll().get(0));
        topic.setAuthor(users.findByEmail("aluno@email.com").orElseThrow());
        return String.valueOf(topics.save(topic).getId());
    }

    private String createTopicAsCurrentUser() throws Exception {
        String redirect = mvc.perform(post("/ui/topics").with(csrf())
                .param("title", "Attack topic").param("message", "valid long message")
                .param("courseName", "Spring Boot")).andExpect(status().isOk())
                .andReturn().getResponse().getHeader("HX-Redirect");
        return redirect.substring(redirect.lastIndexOf('/') + 1);
    }

    @Test
    void sessionCookieDoesNotAuthenticateJsonApi() throws Exception {
        MvcResult login = mvc.perform(formLogin("/ui/login").user("aluno@email.com").password("123456"))
                .andExpect(redirectedUrl("/ui/topics")).andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(get("/ui/topics/new").session(session)).andExpect(status().isOk());
        mvc.perform(post("/topics").session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Session attack\",\"message\":\"valid long message\",\"courseName\":\"Spring Boot\"}"))
                .andExpect(status().isForbidden());
        String id = newTopicId();
        mvc.perform(delete("/topics/" + id).session(session)).andExpect(status().isForbidden());
    }

    @Test
    void anonymousWritesGoToLogin() throws Exception {
        String id = newTopicId();
        mvc.perform(delete("/ui/topics/" + id).with(csrf())).andExpect(status().isFound()).andExpect(redirectedUrl("/ui/login"));
        mvc.perform(put("/ui/topics/" + id).with(csrf()).param("title","Valid title").param("message","valid long message"))
                .andExpect(status().isFound());
        mvc.perform(get("/ui/topics/" + id + "/edit")).andExpect(status().isFound());
        mvc.perform(get("/topics/" + id)).andExpect(status().isOk());
    }

    @Test
    @WithUserDetails("aluno@email.com")
    void putAndDeleteNeedCsrf() throws Exception {
        String id = newTopicId();
        mvc.perform(put("/ui/topics/" + id).param("title","Valid title").param("message","valid long message"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/ui/topics/" + id)).andExpect(status().isForbidden());
        mvc.perform(post("/ui/logout")).andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("aluno@email.com")
    void updateStatusesAndCache() throws Exception {
        String id = createTopicAsCurrentUser();
        mvc.perform(put("/ui/topics/9999").with(csrf()).param("title","Valid title").param("message","valid long message"))
                .andExpect(status().isNotFound());
        mvc.perform(put("/ui/topics/9999").with(csrf()).param("title","abc").param("message","x"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/ui/topics/9999/edit")).andExpect(status().isNotFound());
        mvc.perform(get("/ui/topics/9999")).andExpect(status().isNotFound());
        mvc.perform(delete("/ui/topics/9999").with(csrf())).andExpect(status().isNotFound());
        mvc.perform(put("/ui/topics/" + id).with(csrf()).param("title","abc").param("message","valid long message"))
                .andExpect(status().is(422));
        mvc.perform(get("/topics")).andExpect(status().isOk());
        mvc.perform(put("/ui/topics/" + id).with(csrf()).param("title","Edited by ui").param("message","valid long message"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Edited by ui")));
        mvc.perform(get("/topics/" + id)).andExpect(jsonPath("$.title").value("Edited by ui"));
    }

    @Test
    @WithUserDetails("aluno@email.com")
    void createSetsAuthorAndEscapes() throws Exception {
        String r = mvc.perform(post("/ui/topics").with(csrf())
                .param("title","<script>alert(1)</script>").param("message","<img src=x onerror=alert(1)>")
                .param("courseName","Spring Boot")).andExpect(status().isOk()).andReturn().getResponse().getHeader("HX-Redirect");
        String id = r.substring(r.lastIndexOf('/') + 1);
        mvc.perform(get("/topics/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.authorName").exists());
        mvc.perform(get("/ui/topics/" + id)).andExpect(content().string(not(containsString("<script>alert"))))
                .andExpect(content().string(not(containsString("<img src=x"))))
                .andExpect(content().string(containsString("&lt;script&gt;")));
        mvc.perform(get("/ui/topics")).andExpect(content().string(not(containsString("<img src=x"))));
    }
}
