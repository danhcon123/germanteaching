package com.example.germanteaching.common.exception;

import com.example.germanteaching.common.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;
/**
 * Global error handler that converts every exception into a unified (@link ApiResponse)
 */
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /*------------------------------------------------------------------
     *  Bean-validation (@Valid) errors → 400
     *------------------------------------------------------------------*/
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid (
        MethodArgumentNotValidException ex,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request) {

        String errorMessage = ex.getBindingResult()
                                .getFieldErrors().stream()
                                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                                .collect(Collectors.joining("; "));
        
        ApiResponse<Void> response = ApiResponse.badRequest("Validation failed: " + errorMessage);
        // preserve content-negotiation & default headers
        return handleExceptionInternal(ex, response, headers, status, request);
    }

    /*------------------------------------------------------------------
     *  Custom domain exception → 400
     *------------------------------------------------------------------*/
    @ExceptionHandler(SomeCustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustom(SomeCustomException ex) {
        ApiResponse<Void> response = ApiResponse.badRequest(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /*------------------------------------------------------------------
     *  DB uniqueness / FK violations → 409
     *------------------------------------------------------------------*/
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(DataIntegrityViolationException ex){
        ApiResponse<Void> body = ApiResponse.error(HttpStatus.CONFLICT, "Resource conflict: " +
                                ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    /*------------------------------------------------------------------
     *  Fallback – everything else → 500
     *------------------------------------------------------------------*/
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAll(Exception ex) {
        log.error("Unhandled exception", ex);               // ← logs stack-trace cleanly
        ApiResponse<Void> body = ApiResponse.serverError("An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
