package paymentengine.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.security.auth.login.AccountNotFoundException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({InsufficientBalanceException.class, InactiveAccountException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponseDTO> handleBusinessRulesExceptions(RuntimeException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                "Unprocessable Entity",
                ex.getMessage(),
                Collections.emptyList(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(422).body(error);
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotFoundException(AccountNotFoundException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                "Not Found",
                ex.getMessage(),
                Collections.emptyList(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(404).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.toList());


    ErrorResponseDTO error = new ErrorResponseDTO(
            "Bad Request",
            "Validation failed for one or more fields",
            details,
            LocalDateTime.now()
    );

        return ResponseEntity.status(400).body(error);
    }

}
