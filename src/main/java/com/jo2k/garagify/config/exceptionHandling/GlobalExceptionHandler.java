package com.jo2k.garagify.config.exceptionHandling;

import com.jo2k.garagify.common.exception.InvalidBorrowException;
import com.jo2k.garagify.common.exception.InvalidLendException;
import com.jo2k.garagify.common.exception.InvalidTokenException;
import com.jo2k.garagify.common.exception.ParkingNotFoundException;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Hidden
class GlobalExceptionHandler {

    @ExceptionHandler(InvalidTokenException.class)
    ResponseEntity<ErrorResponse> handleInvalidTokenException(InvalidTokenException exception) {
        return new ResponseEntity<>(new ErrorResponse(exception.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException exception) {
        return new ResponseEntity<>(new ErrorResponse(exception.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidBorrowException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(InvalidBorrowException exception) {
        return new ResponseEntity<>(new ErrorResponse(exception.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidLendException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(InvalidLendException exception) {
        return new ResponseEntity<>(new ErrorResponse(exception.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ParkingNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(ParkingNotFoundException exception) {
        return new ResponseEntity<>(new ErrorResponse(exception.getMessage()), HttpStatus.GONE);
    }
}
