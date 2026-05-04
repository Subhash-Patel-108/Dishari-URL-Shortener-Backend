package com.dishari.in.exception;

import com.dishari.in.web.dto.response.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.nio.file.AccessDeniedException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    ///-----BAD_REQUEST  : 400
    @ExceptionHandler({
            IllegalArgumentException.class ,
            BadCredentialsException.class,
            InvalidVerificationToken.class ,
            InvalidEnumValueException.class ,
            InvalidSortFieldException.class ,
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception exception , WebRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST , exception , request) ;
    }

    ///------ UNAUTHORIZED :401
    @ExceptionHandler({
            EmailNotVerifiedException.class,
            LockedException.class,
            UnauthorizedException.class ,
            UserNotOwnException.class
    })
    public ResponseEntity<ErrorResponse> handleUnauthorized(Exception exception , WebRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED , exception , request) ;
    }

    ///------ PAYMENT_REQUIRED : 402
    @ExceptionHandler({
            PlanUpgradeRequiredException.class ,
            PlanExpiredException.class
    })
    public ResponseEntity<ErrorResponse> handlePaymentRequired(Exception ex, WebRequest request) {
        return buildResponse(HttpStatus.PAYMENT_REQUIRED , ex , request) ;
    }

    ///------FORBIDDEN : 403
    @ExceptionHandler({
            AccessDeniedException.class ,
            AuthorizationDeniedException.class ,
    })
    public ResponseEntity<ErrorResponse> handleForbidden(Exception exception , WebRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN , exception , request) ;
    }

    ///------NOT_FOUND : 404
    @ExceptionHandler({
            UsernameNotFoundException.class ,
            RefreshTokenNotFoundException.class ,
            UserNotFoundException.class ,
            UrlNotFoundException.class ,
    })
    public ResponseEntity<ErrorResponse> handleNotFound(Exception exception , WebRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, exception , request) ;
    }

    //--------CONFLICT : 409
    @ExceptionHandler({
            EmailAlreadyExistException.class ,
            SocialLoginRequiredException.class ,
            RedisOperationException.class ,
            ExternalAuthenticationException.class ,
            SlugAlreadyTakenException.class ,
    })
    public ResponseEntity<ErrorResponse> handleConflict(Exception exception , WebRequest request) {
        return buildResponse(HttpStatus.CONFLICT , exception , request) ;
    }


    //Method to handle @NotNull , @NotEmpty , @Size validation error
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        // Collect all field errors into a single string or list
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((msg1, msg2) -> msg1 + ", " + msg2)
                .orElse("Invalid request parameters");

        // Construct your existing ErrorResponse class
        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message("Validation failed")
                .error(details)
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error) ;
    }

    //Method to handle @Pattern validation error
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {

        // Extract the specific violation messages
        String details = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .reduce((msg1, msg2) -> msg1 + ", " + msg2)
                .orElse("Validation failed");

        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message("Constraint violation")
                .error(details)
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();

        return  ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error) ;
    }

    //---------Too Many Request : 429
    @ExceptionHandler({
            TooManyRequestException.class
    })
    public ResponseEntity<ErrorResponse> handleTooManyRequest(Exception exception , WebRequest request) {
        return buildResponse(HttpStatus.TOO_MANY_REQUESTS , exception , request) ;
    }

    // 500 - DATABASE / INTERNAL ERRORS
    @ExceptionHandler({
            DataAccessException.class,
            TransientDataAccessException.class
    })
    public ResponseEntity<ErrorResponse> handleDatabaseError(Exception ex, WebRequest request) {
        log.error("DATABASE_ERROR at {}: {}", getPath(request), ex.getMessage());
        // Mask the real DB error from the user for security
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .message("An internal database error occurred. Please try again later.")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .path(getPath(request))
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    //General exceptions
    public ResponseEntity<ErrorResponse> handleDateTimeParseException(Exception ex , WebRequest request) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .message("Invalid date format. Use ISO-8601: 2024-01-15T00:00:00Z")
                        .error("INVALID_DATE_FORMAT")
                        .status(400)
                        .path(request.getDescription(false).replace("uri=" , ""))
                        .timestamp(Instant.now().toString())
                        .build());
    }

    //----- Common Method to build ErrorResponse
    private ResponseEntity<ErrorResponse> buildResponse (HttpStatus status , Exception exception , WebRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .message(exception.getMessage())
                .error(status.getReasonPhrase())
                .path(getPath(request))
                .status(status.value())
                .build() ;

        return ResponseEntity.status(status).body(response);
    }

    //-----Method to extract the path from WebRequest
    private String getPath(WebRequest request) {
        return request.getDescription(false).replace("uri=" , "") ;
    }
}
