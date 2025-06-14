package com.example.germanteaching.auth.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "users")
public class User {

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
    @Column(name = "created_at", nullable = false, updatable = false,
             columnDefinition = "TIMESTAMP WITH TIME ZON DEFAULT now()")
    private Instant createdAt;
    
    // lastLoginAt is nullable and updated by application when user logs in.
    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    public User() {
    // Default constructor for JPA
    }

    public User(String username, String email, String passwordHash) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        // xp, lernCoins, streak fields default to 0 via field initializers
        // createdAt defaults to NOW() via DB
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
    // No setter for createdAt; it's set by DB

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    @PrePersist
    public void prePersist(){
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}

 
