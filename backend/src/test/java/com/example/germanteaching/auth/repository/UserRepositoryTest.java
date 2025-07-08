package com.example.germanteaching.auth.repository;

import com.example.germanteaching.auth.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest{
    
    @Autowired
    private UserRepository userRepository;
    
    @BeforeEach
    void cleanState(TestInfo testInfo) {
        // if the test is tagged "seeded", skip the clean
        if (testInfo.getTags().contains("seeded")) {
            return;
        }
        userRepository.deleteAll();
    }

        /**
     * Mark the one test that needs to see the real seed data.
     */
    @Test
    @Tag("seeded")
    @DisplayName("Ensure database is reachable and contains seeded user")
    public void testDatabaseConnectionAndSeedData(){
        //Retrieve all users from the containerized database
        List<User> allUsers = userRepository.findAll();
        // because cleanSlate() is rolled back after the previous test,
        // this first run against a fresh container sees your real seed data.
        // The seeded container DB has at least alice, bob, carol
        assertThat(allUsers).isNotEmpty();
        assertThat(userRepository.existsByUsername("alice")).isTrue();
        assertThat(userRepository.existsByUsername("bob")).isTrue();
        assertThat(userRepository.existsByUsername("carol")).isTrue();
    }

    @Test
    @DisplayName("Persist a new user and verify retrieval by username and email")
    public void testFindAndExistsByUsernameAndEmail() {
        User user = new User();
        user.setUsername ("testuser");
        user.setEmail("test@example.com");
        user.setPasswordHash("hashedpass");
        User saved = userRepository.save(user);

        // when: we query by username
        // Verify retrieval by email
        Optional<User> byUsername = userRepository.findByUsername("testuser");
        assertThat(byUsername).isPresent().hasValueSatisfying(u -> assertThat(u.getEmail()).isEqualTo("test@example.com"));


        Optional<User> byEmail = userRepository.findByEmail("test@example.com");
        assertThat(byEmail).isPresent().contains(saved);

        // Verify existence checks
        assertThat(userRepository.existsByUsername("testuser")).isTrue();
        assertThat(userRepository.existsByEmail("test@example.com")).isTrue();
    }

    @Test
    @DisplayName("Custom findByUsernameOrEmail query works for both username and email")
    public void testFindByUsernameOrEmail() {
        // Assuming no user 'nouser' exists
        User temp = new User();
        temp.setUsername("mixuser");
        temp.setEmail("mix@example.com");
        temp.setPasswordHash("pass");
        userRepository.save(temp);

        // Query by username
        Optional<User> byName = userRepository.findByUsernameOrEmail("mixuser");
        assertThat(byName).isPresent().contains(temp);

        // Query by email
        Optional<User> byEmail = userRepository.findByUsernameOrEmail("mix@example.com");
        assertThat(byEmail).isPresent().contains(temp);
    }    
}