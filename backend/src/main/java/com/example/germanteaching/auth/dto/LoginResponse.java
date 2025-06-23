package com.example.germanteaching.auth.dto;

/**
 * DTO for user login responses.
 */
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long expiresIn;
    private Integer id;
    private String username;
    private Integer xp;
    private Integer lernCoins;
    private Integer currentStreakDays;

    public LoginResponse(){}

    public LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn,
        Integer id,
        String username,
        Integer xp,
        Integer lernCoins,
        Integer currentStreakDays
    ) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
        this.expiresIn    = expiresIn;
        this.id = id;
        this.username = username;
        this.xp = xp;
        this.lernCoins = lernCoins;
        this.currentStreakDays = currentStreakDays;
    }

    // Getters and Setters
    public String getAccessToken(){
        return accessToken;
    }

    public void setAccessToken(String accessToken){
        this.accessToken = accessToken;
    }

    public String getRefreshToken(){
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken){
        this.refreshToken = refreshToken;
    }

    public String getTokenType(){
        return tokenType;
    }

    public void setTokenType(String tokenType){
        this.tokenType = tokenType;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

      public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
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
}    
