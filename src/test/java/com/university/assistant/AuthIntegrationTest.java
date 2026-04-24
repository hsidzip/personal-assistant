package com.university.assistant;

import com.university.assistant.dto.LoginRequest;
import com.university.assistant.dto.RegisterRequest;
import com.university.assistant.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class AuthIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll(); // Очищаем базу перед каждым тестом
    }

    @Test
    void shouldRegisterAndLoginSuccessfully() {
        // 1. Регистрация
        RegisterRequest register = new RegisterRequest();
        register.setUsername("test_user");
        register.setPassword("password123");
        register.setEmail("test@mail.com");

        ResponseEntity<String> regResponse = restTemplate.postForEntity("/api/auth/register", register, String.class);
        Assertions.assertEquals(HttpStatus.OK, regResponse.getStatusCode());
        Assertions.assertTrue(userRepository.findByUsername("test_user").isPresent());

        // 2. Логин
        LoginRequest login = new LoginRequest();
        login.setUsername("test_user");
        login.setPassword("password123");

        ResponseEntity<String> loginResponse = restTemplate.postForEntity("/api/auth/login", login, String.class);
        
        Assertions.assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        Assertions.assertNotNull(loginResponse.getBody());
        Assertions.assertTrue(loginResponse.getBody().contains("token"));
    }
}