package com.hazem.worklink.exceptions;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Gestion : Email déjà existant
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // Gestion : Ressource non trouvée
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleResourceNotFound(ResourceNotFoundException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // Gestion : Solde de points insuffisant
    @ExceptionHandler(InsufficientPointsException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientPoints(InsufficientPointsException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("message", ex.getMessage());
        error.put("code", "INSUFFICIENT_POINTS");
        error.put("required", ex.getRequired());
        error.put("available", ex.getAvailable());
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(error);
    }

    // Gestion : Compte banni
    @ExceptionHandler(UserBannedException.class)
    public ResponseEntity<Map<String, String>> handleUserBanned(UserBannedException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage());
        error.put("banReason", ex.getBanReason() != null ? ex.getBanReason() : "");
        error.put("banDuration", ex.getBanDuration() != null ? ex.getBanDuration() : "");
        error.put("userId", ex.getUserId() != null ? ex.getUserId() : "");
        error.put("userType", ex.getUserType() != null ? ex.getUserType() : "");
        String banEndDate = computeBanEndDate(ex.getBanStartDate(), ex.getBanDuration());
        error.put("banEndDate", banEndDate != null ? banEndDate : "");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    private String computeBanEndDate(LocalDateTime startDate, String banDuration) {
        if (startDate == null || banDuration == null || banDuration.isBlank()) return null;
        LocalDateTime endDate;
        switch (banDuration) {
            case "1 Day"    -> endDate = startDate.plusDays(1);
            case "3 Days"   -> endDate = startDate.plusDays(3);
            case "7 Days"   -> endDate = startDate.plusDays(7);
            case "14 Days"  -> endDate = startDate.plusDays(14);
            case "30 Days"  -> endDate = startDate.plusDays(30);
            default         -> { return null; }
        }
        return endDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH));
    }

    // Gestion : Mauvais identifiants (login incorrect)
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Email ou mot de passe incorrect");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    // Gestion : Erreurs de validation (@NotBlank, @Email, etc.)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    // Gestion : Erreurs métier (règles applicatives)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // Gestion : Toutes les autres exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", "Une erreur s'est produite: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}