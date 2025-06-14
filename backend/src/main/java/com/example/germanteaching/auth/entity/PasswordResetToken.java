package com.example.germanteaching.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column (name = "id")
    private Long id;

    @Column (name = "token", nullable =false, length = 255)
    private String token;

    /**
     * Relationship to User. We enforce one active token per user via a unique constraint on user_id if desired.
     * If you want strictly one token per user at database level, you can add `unique = true` on JoinColumn.
     * Otherwise, you can allow multiple historical tokens but delete old ones in service logic.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column (name = "used", nullable = false)
    private boolean used = false;
    
    @Column (name = "created_date", nullable = false)
    private Instant createDate;

    // When this token expires. Map TIMESTAMPTZ to Instant.
    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;
    
    // Constructor
    public PasswordResetToken(){}
    
    public PasswordResetToken(User user, String token, Instant expiryDate) {
        this.user = user;
        this.token = token;
        this.expiryDate = expiryDate;
        this.createDate = Instant.now();
        this.used = false;
    }
    
    // Utility methods
    /**
     * Check if this token has expired based on current time.
     * @return true, if the token has expired
    */
    public boolean isExpired() {
        return Instant.now().isAfter(this.expiryDate);
    }
    
    /**
     * Check if this token is valid (not used and not expired).
     * @return true, if the token can still be used
     */
    public boolean isValid() {
        return !this.used && !isExpired();
    }


    // Getters and Setters
    public Long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }
    
    public Instant getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Instant createDate) {
        this.createDate = createDate;
    }
    
    public Instant getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Instant expiryDate) {
        this.expiryDate = expiryDate;
    }
}
