package com.forum.forum;

import de.codecentric.boot.admin.client.registration.ApplicationRegistrator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ForumApplicationTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private TestRestTemplate rest;

	@Test
	void registersOnSpringBootAdmin() {
		assertNotNull(context.getBean(ApplicationRegistrator.class));
	}

	@Test
	void servesSwaggerDocs() {
		assertEquals(HttpStatus.OK, rest.getForEntity("/v2/api-docs", String.class).getStatusCode());
	}

	@Test
	void listsTopics() {
		assertEquals(HttpStatus.OK, rest.getForEntity("/topics", String.class).getStatusCode());
	}

	@Test
	void logsInWithSeedUser() {
		Map<String, String> login = new HashMap<>();
		login.put("email", "aluno@email.com");
		login.put("password", "123456");

		ResponseEntity<String> response = rest.postForEntity("/auth", login, String.class);

		assertEquals(HttpStatus.OK, response.getStatusCode());
	}

}
