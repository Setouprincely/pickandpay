package com.doch.common.exceptions;

import com.doch.User.dto.APIResponse;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    private static ResponseEntity<APIResponse<Object>> body(HttpStatus status, String errorType, String message) {
        APIResponse<Object> response = new APIResponse<>();
        response.setSuccess(false);
        response.setStatusCode(status.value());
        response.setMessage(message);
        response.setErrorType(errorType);
        response.setData(null);
        response.setTimestamp(LocalDateTime.now());
        return new ResponseEntity<>(response, status);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<APIResponse<Object>> handleBusinessException(BusinessException ex) {
        return body(HttpStatus.BAD_REQUEST, "BusinessError", ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<APIResponse<Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return body(HttpStatus.NOT_FOUND, "NotFoundError", ex.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<APIResponse<Object>> handleNoResource(NoResourceFoundException ex) {
        return body(HttpStatus.NOT_FOUND, "NotFoundError", "Endpoint not found.");
    }

    @ExceptionHandler({ValidationException.class, ConstraintViolationException.class})
    public ResponseEntity<APIResponse<Object>> handleValidationException(Exception ex) {
        return body(HttpStatus.BAD_REQUEST, "ValidationError", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<APIResponse<Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().isEmpty()
                ? (ex.getBindingResult().getAllErrors().isEmpty() ? "Invalid request." : ex.getBindingResult().getAllErrors().get(0).getDefaultMessage())
                : ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return body(HttpStatus.BAD_REQUEST, "ValidationError", message);
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MissingServletRequestPartException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<APIResponse<Object>> handleBadParameters(Exception ex) {
        return body(HttpStatus.BAD_REQUEST, "ValidationError", ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<APIResponse<Object>> handleUnreadable(HttpMessageNotReadableException ex) {
        return body(HttpStatus.BAD_REQUEST, "ValidationError", "Malformed request body.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<APIResponse<Object>> handleMethod(HttpRequestMethodNotSupportedException ex) {
        return body(HttpStatus.METHOD_NOT_ALLOWED, "MethodNotAllowed", ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<APIResponse<Object>> handleUploadSize(MaxUploadSizeExceededException ex) {
        return body(HttpStatus.PAYLOAD_TOO_LARGE, "ValidationError", "The uploaded file is too large (max 10 MB per request).");
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<APIResponse<Object>> handleMultipart(MultipartException ex) {
        log.warn("Rejected multipart request: {}", ex.getMostSpecificCause().toString());
        return body(HttpStatus.BAD_REQUEST, "ValidationError", "The upload could not be processed (too many fields or files, or a malformed form).");
    }

    @ExceptionHandler({OptimisticLockingFailureException.class, PessimisticLockingFailureException.class})
    public ResponseEntity<APIResponse<Object>> handleConcurrentUpdate(Exception ex) {
        log.warn("Concurrent modification rejected: {}", ex.getMessage());
        return body(HttpStatus.CONFLICT, "ConcurrentModification",
                "This record was changed by someone else at the same time. Refresh and try again.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<APIResponse<Object>> handleIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return body(HttpStatus.CONFLICT, "DataConflict", "The operation conflicts with existing data (duplicate or invalid reference).");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<APIResponse<Object>> handleAuthenticationException(AuthenticationException ex) {
        return body(HttpStatus.UNAUTHORIZED, "AuthenticationError", ex.getMessage());
    }

    @ExceptionHandler(TokenException.class)
    public ResponseEntity<APIResponse<Object>> handleTokenException(TokenException ex) {
        return body(HttpStatus.BAD_REQUEST, "TokenError", ex.getMessage());
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<APIResponse<Object>> handleAccessDenied(Exception ex) {
        String message = ex.getMessage() != null && !ex.getMessage().startsWith("Access Denied")
                && !ex.getMessage().equals("Access Denied.")
                ? ex.getMessage() : "Access denied: you do not have permission to perform this action.";
        return body(HttpStatus.FORBIDDEN, "AccessDenied", message);
    }

    /** Last resort: never leak exception internals to clients; log them with a correlation reference. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIResponse<Object>> handleGenericException(Exception ex) {
        String reference = UUID.randomUUID().toString().substring(0, 8);
        log.error("Unhandled error [ref {}]", reference, ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "ServerError",
                "An unexpected error occurred. Please try again or contact support with reference " + reference + ".");
    }
}
