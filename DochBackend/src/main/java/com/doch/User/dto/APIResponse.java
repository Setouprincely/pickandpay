package com.doch.User.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class APIResponse <T>{
    private boolean success;       // Indicates if the operation was successful
    private int statusCode;        // The HTTP status code
    private String message;        // A human-readable message
    private String errorType;
    private T data;
    private LocalDateTime timestamp;
}