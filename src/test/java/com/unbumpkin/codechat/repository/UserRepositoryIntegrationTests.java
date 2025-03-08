package com.unbumpkin.codechat.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.unbumpkin.codechat.model.User;
import com.unbumpkin.codechat.util.ValidationUtil;

@SpringBootTest
@Transactional
class UserRepositoryIntegrationTests {

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void init() {
        testUser = new User(0, "Test User", "test@user.com", "Password1@", User.Role.USER);
    }


    @Test
    void testCreateValidateAndFindUser() {
        User testUser = new User(0, "Test User", "test@user.com", "Password1@", User.Role.USER);
        User createdUser = userRepository.create(testUser);
        ValidationUtil.validate(testUser);
        assertTrue(createdUser.userid() > 0);
        assertEquals(testUser.name(), createdUser.name());
        assertEquals(testUser.email(), createdUser.email());
    }

    @Test
    void testFindByEmail() {
        userRepository.create(testUser);

        User foundUser = userRepository.findByEmail(testUser.email());
        
        assertEquals(testUser.name(), foundUser.name());
        assertEquals(testUser.email(), foundUser.email());
    }

    @Test
    void testFindByEmailNotFound() {
        User foundUser = userRepository.findByEmail("nonexistent@email.com");
        assertTrue(foundUser==null);
    }

    @Test
    void testDeleteById() {
        User createdUser = userRepository.create(testUser);
        assertTrue(createdUser!=null);
        
        userRepository.deleteById(createdUser.userid());
        
        User deletedUser = userRepository.findById(createdUser.userid());
        assertTrue(deletedUser==null);
    }

    @Test
    void testDeleteByEmail() {
        userRepository.create(testUser);
        
        userRepository.deleteByEmail(testUser.email());
        
        User deletedUser = userRepository.findByEmail(testUser.email());
        assertTrue(deletedUser==null);
    }

    @Test
    void testExistsById() {
        User testUser = new User(0, "Test User", "test@user.com", "Password1@", User.Role.USER);
        User createdUser = userRepository.create(testUser);
        assertTrue(createdUser!=null);
        
        assertTrue(userRepository.existsById(createdUser.userid()));
        assertFalse(userRepository.existsById(-1));
    }

    @Test
    void testExistsByEmail() {
        userRepository.create(testUser);
        
        assertTrue(userRepository.existsByEmail(testUser.email()));
        assertFalse(userRepository.existsByEmail("nonexistent@email.com"));
    }

    @Test
    void testFindAll() {
        User user1 = new User(0, "User 1", "user1@test.com", "Password1@", User.Role.USER);
        User user2 = new User(0, "User 2", "user2@test.com", "Password1@", User.Role.ADMIN);
        
        int cnt = userRepository.count();
        userRepository.create(user1);
        userRepository.create(user2);
        
        List<User> users = (List<User>) userRepository.findAll();
        
        assertEquals(cnt+2, users.size());
        assertTrue(users.stream().anyMatch(u -> u.email().equals(user1.email())));
        assertTrue(users.stream().anyMatch(u -> u.email().equals(user2.email())));
    }

    @Test
    void testCount() {
        int cnt = userRepository.count();
        
        userRepository.create(testUser);
        assertEquals(cnt+1, userRepository.count());
        
        User anotherUser = new User(0, "Another User", "another@test.com", "Password1@", User.Role.USER);
        userRepository.create(anotherUser);
        assertEquals(cnt+2, userRepository.count());
    }

    @Test
    void testCreateUserWithAdminRole() {
        User adminUser = new User(0, "Admin User", "admin@test.com", "Password1@", User.Role.ADMIN);
        User createdUser = userRepository.create(adminUser);
        
        assertTrue(createdUser!=null);
        assertEquals(User.Role.ADMIN, createdUser.role());
        assertTrue(createdUser.isAdmin());
    }
}