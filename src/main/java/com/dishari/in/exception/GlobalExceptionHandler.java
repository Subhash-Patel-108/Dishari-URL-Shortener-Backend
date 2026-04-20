package com.dishari.in.exception;

import com.dishari.in.web.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    ///-----BAD_REQUEST
    @ExceptionHandler({
            IllegalArgumentException.class ,
            BadCredentialsException.class,
            InvalidVerificationToken.class ,
            MethodArgumentNotValidException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception exception , WebRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST , exception , request) ;
    }

    ///------ UNAUTHORIZED :401
    @ExceptionHandler({
            EmailNotVerifiedException.class,
            LockedException.class,
    })
    public ResponseEntity<ErrorResponse> handleUnauthorized(Exception exception , WebRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED , exception , request) ;
    }

    ///------NOT_FOUND : 404
    @ExceptionHandler({
            UsernameNotFoundException.class ,
            RefreshTokenNotFoundException.class ,
            UserNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(Exception exception , WebRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, exception , request) ;
    }

    //--------CONFLICT : 409
    @ExceptionHandler({
            EmailAlreadyExistException.class ,
            SocialLoginRequiredException.class ,
            RedisOperationException.class ,
            ExternalAuthenticationException.class
    })
    public ResponseEntity<ErrorResponse> handleConflict(Exception exception , WebRequest request) {
        return buildResponse(HttpStatus.CONFLICT , exception , request) ;
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
                .timeStamp(LocalDateTime.now())
                .message("An internal database error occurred. Please try again later.")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .path(getPath(request))
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    //----- Common Method to build ErrorResponse
    private ResponseEntity<ErrorResponse> buildResponse (HttpStatus status , Exception exception , WebRequest request) {
        ErrorResponse response = ErrorResponse.builder()
                .timeStamp(LocalDateTime.now())
                .message(exception.getMessage())
                .error(exception.getMessage())
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
