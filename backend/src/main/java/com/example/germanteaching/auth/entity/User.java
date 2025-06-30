package com.example.germanteaching.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;
    
    @Column(name = "username", nullable = false, unique = true, length = 255)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;
    
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
    
    @Column(name = "xp", nullable = false)
    private Integer xp = 0;
    
    @Column(name = "lern_coins", nullable = false)
    private Integer lernCoins = 0;

    @Column(name = "current_streak_days", nullable = false)
    private Integer currentStreakDays = 0;

    @Column(name = "highest_streak_achieved", nullable = false)
    private Integer highestStreakAchieved = 0;

    @Column(name = "last_streak_activity_date")
    private LocalDate lastStreakActivityDate;

    @Column(name = "streak_freezes_owned", nullable = false)
    private Integer streakFreezesOwned = 0;

    // let the DB default to NOW(), and mark updatable=false so JPA does not override it
    //After insert, when entity is reloaded, JPA can read the DB-generated value.
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    // lastLoginAt is nullable and updated by application when user logs in.
    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    // Add the active field
    @Column(name = "active", nullable = false)
    private boolean active = true; // Default to true for new users
    
    @Column(name = "updated_at")
    private Instant updatedAt;

    public User() {
    // Default constructor for JPA
    }

    public User(String username, String email, String passwordHash) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        // xp, lernCoins, streak fields default to 0 via field initializers
    }
    
    // GETTERs and SETTERs
    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
    
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
    
    public Integer getXp() {
        return xp;
    }

    public void setXp(Integer xp) {
        this.xp = xp;
    }

    public Integer getLernCoins() {
        return lernCoins;
    }
    
    public void setLernCoins(Integer lernCoins) {
        this.lernCoins = lernCoins;
    }

    public Integer getCurrentStreakDays() {
        return currentStreakDays;
    }
    
    public void setCurrentStreakDays(Integer currentStreakDays) {
        this.currentStreakDays = currentStreakDays;
    }

    public Integer getHighestStreakAchieved() {
        return highestStreakAchieved;
    }

    public void setHighestStreakAchieved(Integer highestStreakAchieved) {
        this.highestStreakAchieved = highestStreakAchieved;
    }

    public LocalDate getLastStreakActivityDate() {
        return lastStreakActivityDate;
    }
    
    public void setLastStreakActivityDate(LocalDate lastStreakActivityDate) {
        this.lastStreakActivityDate = lastStreakActivityDate;
    }

    public Integer getStreakFreezesOwned() {
        return streakFreezesOwned;
    }

    public void setStreakFreezesOwned(Integer streakFreezesOwned) {
        this.streakFreezesOwned = streakFreezesOwned;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }
    
    public boolean isActive() {
    return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
    // Method to activate user
    public void activate (){
        this.active = true;
    }
    // Method to deactivate user
    public void deactivate (){
        this.active = false;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        active = true;
    }
        
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

 
