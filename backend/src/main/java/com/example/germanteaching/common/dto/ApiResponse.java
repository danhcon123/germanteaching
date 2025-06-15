package com.example.germanteaching.common.dto;

import org.springframework.http.HttpStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

/**
 * Generic API response wrapper that standardized all API Responses.
 * @param <T> The type of data being returned 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse <T>(
    int status, // HTTP status code (200, 400, 500, etc.)
    boolean success, // Quick sucess/failure flag
    String message, // Human-readable message
    T data, // The actual data being returned, can be null if not applicable
    LocalDateTime timestamp // Timestamp of the response
) {    
    // Constructor auto-sets timestamp
    public ApiResponse(int status, boolean sucess, String message, T data){
        this(status, sucess, message, data, LocalDateTime.now());
    }

    // SUCCESS FACTORYS (200)
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(HttpStatus.OK.value(), true, "Request was successful", data);
    }
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(HttpStatus.OK.value(), true, message, data);
    }
    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(HttpStatus.OK.value(), true, message, null);
    }
    
    // ERROR FACTORYS
    public static ApiResponse<Void> error(HttpStatus status, String message) {
        return new ApiResponse<>(status.value(), false, message, null);
    }
    public static ApiResponse<Void> error(int statusCode, String message) {
        return new ApiResponse<>(statusCode, false, message, null);
    }
    
    // COMMON SHORTCUTS
    public static ApiResponse<Void> badRequest(String message) {
        return error(HttpStatus.BAD_REQUEST, message);
    }
    public static ApiResponse<Void> unauthorized(String message) {
        return error(HttpStatus.UNAUTHORIZED, message);
    }
    public static ApiResponse<Void> forbidden(String message) {
        return error(HttpStatus.FORBIDDEN, message);
    }
    public static ApiResponse<Void> notFound(String message) {
        return error(HttpStatus.NOT_FOUND, message);
    }
    public static ApiResponse<Void> serverError(String message) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    // UTILITIES
    public boolean isSuccessful(){
        return status >= 200 && status < 300;
    }
    public boolean isClientError(){
        return status >= 400 && status < 500;
    }
    public boolean isServerError(){
        return status >= 500 && status < 600;
    }
    public HttpStatus getHttpStatus() {
        return HttpStatus.valueOf(status);
    }
}
