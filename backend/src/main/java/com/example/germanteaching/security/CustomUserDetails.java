package com.example.germanteaching.security;

import com.example.germanteaching.auth.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;

public class CustomUserDetails implements UserDetails {
    private final Integer id;
    private final String username;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final Integer xp;
    private final Integer lernCoins;
    private final Integer currentStreakDays;
    
    public CustomUserDetails(
        Integer id,
        String username,
        String password,
        Collection<? extends GrantedAuthority> authorities,
        Integer xp,
        Integer lernCoins,
        Integer currentStreakDays
    ){
        this.id = id;
        this.username = username;
        this.password = password;
        this.authorities = authorities;
        this.xp = xp;
        this.lernCoins = lernCoins;
        this.currentStreakDays = currentStreakDays;
    }
    
    public Integer getUserId() {
        return id;
    }

    @Override public String getUsername() {
        return username;
    }
    @Override public String getPassword() {
        return password;
    }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }
    @Override public boolean isAccountNonExpired() {
        return true; // Assuming account never expires
    }
    @Override public boolean isAccountNonLocked() {
        return true; // Assuming account is never locked
    }
    @Override public boolean isCredentialsNonExpired() {
        return true; // Assuming credentials never expire
    }
    @Override public boolean isEnabled() {
        return true; // Assuming account is always enabled
    }
    
    public Integer getXp() {
        return xp;
    }
    public Integer getLernCoins() {
        return lernCoins;
    }
    public Integer getCurrentStreakDays() {
        return currentStreakDays;
    }
}
