package com.sungjujjang.entry.Global.Errors;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidException(MethodArgumentNotValidException e) {
        ErrorCode statusCode = ErrorCode.NOT_VALID_DTO_ERR;
        ErrorResponse response = ErrorResponse.errorCodeFrom(statusCode,
                e.getBindingResult().getFieldErrors().getFirst().getDefaultMessage());
        return new ResponseEntity<>(response, HttpStatusCode.valueOf(statusCode.getErrorCode()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorCode statusCode = e.getErrorCode();
        ErrorResponse response = ErrorResponse.errorCodeFrom(statusCode, e.getDescription());
        return new ResponseEntity<>(response, HttpStatusCode.valueOf(statusCode.getErrorCode()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        ErrorResponse response = ErrorResponse.errorCodeFrom(ErrorCode.INTERNAL_SERVER_ERR, "Server Error raised.");
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
