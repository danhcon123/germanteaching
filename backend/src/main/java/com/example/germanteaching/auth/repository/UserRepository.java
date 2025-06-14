package com.example.germanteaching.auth.repository;

import com.example.germanteaching.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;


public interface UserRepository extends JpaRepository <User, Integer> {

    /**
     * Find a user by their username.
     * 
     * @param username the username to search for
     * @return an Optional containing the User if found, or empty if not found
     */
    Optional<User> findByUsername(String username);

    /**
     * Find a user by their email.
     * 
     * @param email the email to search for
     * @return an Optional containing the User if found, or empty if not found
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Check if user name already exists (useful for registration)
     */
    boolean existsByUsername(String username);
    
    /**
     * Check if email already exists (useful for registration)
     */
    boolean existsByEmail(String email);

    /**
     * Find by username or email (useful for login flexibility)
     */
    @Query("SELECT u FROM User u WHERE u.username = :identifier OR u.email = :identifier")
    Optional<User> findByUsernameOrEmail(@Param("identifier") String identifier);
}
