package ee.ut.melih.notificationservice.controller;

import ee.ut.melih.notificationservice.dto.ErrorResponse;
import ee.ut.melih.notificationservice.exception.DispatchException;
import ee.ut.melih.notificationservice.exception.NotificationNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(NotificationNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(NotificationNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    String message =
        ex.getBindingResult().getFieldErrors().stream()
            .map(err -> err.getField() + ": " + err.getDefaultMessage())
            .findFirst()
            .orElse("Invalid request");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ErrorResponse.of(400, "Bad Request", message));
  }

  @ExceptionHandler(MissingRequestHeaderException.class)
  public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ErrorResponse.of(400, "Bad Request", ex.getMessage()));
  }

  @ExceptionHandler(DispatchException.class)
  public ResponseEntity<ErrorResponse> handleDispatchFailure(DispatchException ex) {
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
        .body(ErrorResponse.of(502, "Bad Gateway", ex.getMessage()));
  }
}
