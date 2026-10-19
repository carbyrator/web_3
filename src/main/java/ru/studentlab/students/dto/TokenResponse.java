package ru.studentlab.students.dto;

public class TokenResponse {
    public String accessToken;
    public String refreshToken;
    public String tokenType = "Bearer";
    public long expiresIn;

    public TokenResponse(String accessToken, String refreshToken, long expiresIn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
    }
}
