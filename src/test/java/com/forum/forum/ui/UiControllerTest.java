package com.forum.forum.ui;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.boot.admin.client.enabled=false")
@AutoConfigureMockMvc
class UiControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void listPageShowsSeedTopics() throws Exception {
        mvc.perform(get("/ui/topics"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<html")))
                .andExpect(content().string(containsString("Error to create the project")));
    }

    @Test
    void listFragmentHasNoLayoutForHtmx() throws Exception {
        mvc.perform(get("/ui/topics").header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"topics\"")))
                .andExpect(content().string(not(containsString("<html"))));
    }

    @Test
    void newFormNeedsLogin() throws Exception {
        mvc.perform(get("/ui/topics/new"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/ui/login"));
    }

    @Test
    @WithUserDetails("aluno@email.com")
    void createWithValidDataRedirectsWithHtmxHeader() throws Exception {
        mvc.perform(post("/ui/topics").with(csrf())
                        .param("title", "Valid title").param("message", "valid long message")
                        .param("courseName", "Spring Boot"))
                .andExpect(status().isOk())
                .andExpect(header().string("HX-Redirect", startsWith("/ui/topics/")));
    }

    @Test
    @WithUserDetails("aluno@email.com")
    void createWithoutCsrfIsForbidden() throws Exception {
        mvc.perform(post("/ui/topics")
                        .param("title", "Valid title").param("message", "valid long message")
                        .param("courseName", "Spring Boot"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("aluno@email.com")
    void createWithShortTitleGives422AndError() throws Exception {
        mvc.perform(post("/ui/topics").with(csrf())
                        .param("title", "abc").param("message", "valid long message")
                        .param("courseName", "Spring Boot"))
                .andExpect(status().is(422))
                .andExpect(content().string(containsString("length must be between 5")));
    }

    @Test
    @WithUserDetails("aluno@email.com")
    void deleteRemovesTopicAndEvictsApiCache() throws Exception {
        String redirect = mvc.perform(post("/ui/topics").with(csrf())
                        .param("title", "Delete me please").param("message", "valid long message")
                        .param("courseName", "Spring Boot"))
                .andReturn().getResponse().getHeader("HX-Redirect");
        String id = redirect.substring(redirect.lastIndexOf('/') + 1);

        mvc.perform(get("/topics")).andExpect(content().string(containsString("Delete me please")));
        mvc.perform(delete("/ui/topics/" + id).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("HX-Redirect", "/ui/topics"));
        mvc.perform(get("/topics")).andExpect(content().string(not(containsString("Delete me please"))));
    }

    @Test
    void apiStillWorksAndStaysStateless() throws Exception {
        mvc.perform(get("/topics"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        mvc.perform(post("/topics").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Location"));
    }
}
