package com.example.germanteaching.auth.repository;

import com.example.germanteaching.auth.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.jdbc.Sql;

import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(
    includeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = UserRepository.class
    )
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserRepositoryTest{
    
    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Ensure databse is reachable and contains seeded user")
    public void testDatabaseConnectionAndSeedData(){
        //Retrieve all users from the containerized database
        List<User> allUsers = userRepository.findAll();
        
        // The seeded container DB has at least alice, bob, carol
        assertThat(allUsers).isNotEmpty();
        assertThat(userRepository.existsByUsername("alice")).isTrue();
        assertThat(userRepository.existsByUsername("bob")).isTrue();
        assertThat(userRepository.existsByUsername("carol")).isTrue();
    }

    @Test
    @DisplayName("Persist a new user and verify retrieval by username and email")
    @Sql(statements = "TRUNCATE TABLE users RESTART IDENTITY CASCADE", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
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
    @Sql(statements = "TRUNCATE TABLE users RESTART IDENTITY CASCADE", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
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