package com.example.germanteaching.auth.repository;

import com.example.germanteaching.auth.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void whenFindByUsername_thenReturnUser() {
        // given
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("testuser@example.com");
        user.setPasswordHash("password");
        entityManager.persist(user);
        entityManager.flush();

        // when
        Optional<User> found = userRepository.findByUsername(user.getUsername());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo(user.getUsername());
    }

    @Test
    public void whenFindByEmail_thenReturnUser() {
        // given
        User user = new User();
        user.setUsername("testuser2");
        user.setEmail("testuser2@example.com");
        user.setPasswordHash("password");
        entityManager.persist(user);
        entityManager.flush();

        // when
        Optional<User> found = userRepository.findByEmail(user.getEmail());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo(user.getEmail());
    }

    @Test
    public void whenExistsByUsername_thenReturnTrue() {
        // given
        User user = new User();
        user.setUsername("testuser3");
        user.setEmail("testuser3@example.com");
        user.setPasswordHash("password");
        entityManager.persist(user);
        entityManager.flush();

        // when
        boolean exists = userRepository.existsByUsername(user.getUsername());

        // then
        assertThat(exists).isTrue();
    }

    @Test
    public void whenExistsByEmail_thenReturnTrue() {
        // given
        User user = new User();
        user.setUsername("testuser4");
        user.setEmail("testuser4@example.com");
        user.setPasswordHash("password");
        entityManager.persist(user);
        entityManager.flush();

        // when
        boolean exists = userRepository.existsByEmail(user.getEmail());

        // then
        assertThat(exists).isTrue();
    }

    @Test
    public void whenFindByUsernameOrEmail_withUsername_thenReturnUser() {
        // given
        User user = new User();
        user.setUsername("testuser5");
        user.setEmail("testuser5@example.com");
        user.setPasswordHash("password");
        entityManager.persist(user);
        entityManager.flush();

        // when
        Optional<User> found = userRepository.findByUsernameOrEmail(user.getUsername());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo(user.getUsername());
    }

    @Test
    public void whenFindByUsernameOrEmail_withEmail_thenReturnUser() {
        // given
        User user = new User();
        user.setUsername("testuser6");
        user.setEmail("testuser6@example.com");
        user.setPasswordHash("password");
        entityManager.persist(user);
        entityManager.flush();

        // when
        Optional<User> found = userRepository.findByUsernameOrEmail(user.getEmail());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo(user.getEmail());
    }
}