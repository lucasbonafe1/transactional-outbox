package com.example.orders.controller;

import com.example.orders.exception.IdempotencyKeyReuseException;
import com.example.orders.exception.InvalidIdempotencyKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidIdempotencyKeyException.class)
    public ProblemDetail invalidKey(InvalidIdempotencyKeyException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(IdempotencyKeyReuseException.class)
    public ProblemDetail keyReuse(IdempotencyKeyReuseException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(422), e.getMessage());
    }
}
