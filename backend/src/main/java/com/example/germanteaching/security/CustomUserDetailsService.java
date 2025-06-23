package com.example.germanteaching.security;

import com.example.germanteaching.auth.entity.User;
import com.example.germanteaching.security.CustomUserDetails;
import com.example.germanteaching.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepo;

    @Autowired
    public CustomUserDetailsService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {
        User user = userRepo.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with username: " + username
                    ));

        return new CustomUserDetails(
            user.getUserId(),
            user.getUsername(),
            user.getPasswordHash(),
            Collections.emptyList(),
            user.getXp(),
            user.getLernCoins(),
            user.getCurrentStreakDays()
        );
    }
}