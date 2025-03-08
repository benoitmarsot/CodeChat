package com.unbumpkin.codechat.controller;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.Rollback;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unbumpkin.codechat.dto.auth.AuthRequest;
import com.unbumpkin.codechat.dto.auth.RegisterRequest;
import com.unbumpkin.codechat.model.User;
import com.unbumpkin.codechat.repository.UserRepository;

@SpringBootTest(webEnvironment = RANDOM_PORT)
// Todo: Doesn't seems to work with the controller, for now using cleanup method
// @Transactional
// @TestExecutionListeners(
//     value = { TransactionalTestExecutionListener.class },
//     mergeMode = MergeMode.MERGE_WITH_DEFAULTS
// )
@Rollback

class AuthControllerIntegrationTests {
    @LocalServerPort
    private int port;  

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "Password1@2$@";

    private static final Logger logger = LoggerFactory.getLogger(AuthControllerIntegrationTests.class);

    @AfterEach
    void cleanup() {
        userRepository.deleteByEmail(TEST_EMAIL);
    }
    @Test
    void testSuccessfulRegistrationAndLogin() throws Exception {
        RegisterRequest registrationRequest = new RegisterRequest(
            "Test User",
            TEST_EMAIL,
            TEST_PASSWORD,
            User.Role.USER
        );
    
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        String requestJson = objectMapper.writeValueAsString(registrationRequest);
        logger.debug("Request JSON: {}", requestJson);
        
        HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(
            "/api/v1/auth/register",
            entity,
            String.class
        );
        
        logger.debug("Response status: {}", response.getStatusCode());
        logger.debug("Response body: {}", response.getBody());
    
        assertEquals(OK, response.getStatusCode(), 
            String.format("Expected 200 OK but got %s. Response body: %s", 
                response.getStatusCode(), response.getBody())
        );
        
        AuthRequest loginRequest = new AuthRequest(TEST_EMAIL, TEST_PASSWORD);
        requestJson = objectMapper.writeValueAsString(loginRequest);

        HttpEntity<String> loginEntity = new HttpEntity<>(requestJson, headers);
        ResponseEntity<Map<String,String>> loginResponse = restTemplate.exchange("/api/v1/auth/login", HttpMethod.POST, loginEntity, new ParameterizedTypeReference<Map<String,String>>() {});
        Map<String,String> responseBody = loginResponse.getBody();
        assertNotNull(responseBody, "Response body should not be null");
        assertTrue(responseBody.containsKey("token"));
        assertTrue(responseBody.containsKey("userId"));
    }

    @Test
    void testDuplicateEmailRegistration() throws Exception {
        RegisterRequest request = new RegisterRequest(
            "Test User",
            TEST_EMAIL,
            TEST_PASSWORD,
            User.Role.USER
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(request), headers);

        // First registration
        ResponseEntity<String> response = restTemplate.exchange("/api/v1/auth/register", HttpMethod.POST, entity, String.class);
        assertEquals(OK, response.getStatusCode());

        // Second registration with same email
        response = restTemplate.exchange("/api/v1/auth/register", HttpMethod.POST, entity, String.class);
        assertEquals(CONFLICT, response.getStatusCode());
    }

    @Test
    void testLoginWithInvalidCredentials() throws Exception {
        AuthRequest loginRequest = new AuthRequest(TEST_EMAIL, "WrongPassword1@");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(loginRequest), headers);

        ResponseEntity<String> response = restTemplate.exchange("/api/v1/auth/login", HttpMethod.POST, entity, String.class);
        assertEquals(UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testRegistrationWithInvalidPassword() throws Exception {
        RegisterRequest request = new RegisterRequest(
            "Test User",
            TEST_EMAIL,
            "weakpass", // Invalid password
            User.Role.USER
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(request), headers);

        ResponseEntity<String> response = restTemplate.exchange("/api/v1/auth/register", HttpMethod.POST, entity, String.class);
        assertEquals(BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testRegistrationWithInvalidEmail() throws Exception {
        RegisterRequest request = new RegisterRequest(
            "Test User",
            "invalid-email", // Invalid email
            "Password1@",
            User.Role.USER
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(request), headers);

        ResponseEntity<String> response = restTemplate.exchange("/api/v1/auth/register", HttpMethod.POST, entity, String.class);
        assertEquals(BAD_REQUEST, response.getStatusCode());
    }
}