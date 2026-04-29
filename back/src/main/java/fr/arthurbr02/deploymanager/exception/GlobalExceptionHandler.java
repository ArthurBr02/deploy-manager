package fr.arthurbr02.deploymanager.exception;

import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Void> handleBrokenPipe(IOException e) {
        if ("Broken pipe".equalsIgnoreCase(e.getMessage())) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<?> handleForbidden(ForbiddenException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntime(RuntimeException e) {
        String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        List<String> errors = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.toList());
        return ResponseEntity.badRequest().body(Map.of("error", "Validation échouée", "details", errors));
    }
}
