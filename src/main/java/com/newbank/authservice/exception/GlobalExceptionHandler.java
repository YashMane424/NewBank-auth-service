package com.newbank.authservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthenticationException(
            AuthenticationException ex,
            WebRequest request) {
        log.error("Authentication error: {}", ex.getMessage());
        return new ResponseEntity<>(
                ApiError.builder()
                        .status(HttpStatus.UNAUTHORIZED.value())
                        .error("AuthenticationError")
                        .message(ex.getMessage())
                        .path(request.getDescription(false).replace("uri=", ""))
                        .timestamp(LocalDateTime.now())
                        .build(),
                HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleUserAlreadyExistsException(
            UserAlreadyExistsException ex,
            WebRequest request) {
        log.error("User already exists error: {}", ex.getMessage());
        return new ResponseEntity<>(
                ApiError.builder()
                        .status(HttpStatus.CONFLICT.value())
                        .error("UserAlreadyExists")
                        .message(ex.getMessage())
                        .path(request.getDescription(false).replace("uri=", ""))
                        .timestamp(LocalDateTime.now())
                        .build(),
                HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex,
            WebRequest request) {
        String message = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.error("Validation error: {}", message);
        return new ResponseEntity<>(
                ApiError.builder()
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error("ValidationError")
                        .message(message)
                        .path(request.getDescription(false).replace("uri=", ""))
                        .timestamp(LocalDateTime.now())
                        .build(),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneralException(
            Exception ex,
            WebRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(
                ApiError.builder()
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .error("InternalServerError")
                        .message("An unexpected error occurred")
                        .path(request.getDescription(false).replace("uri=", ""))
                        .timestamp(LocalDateTime.now())
                        .build(),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
