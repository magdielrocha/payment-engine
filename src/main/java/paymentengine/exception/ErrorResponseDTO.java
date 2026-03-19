package paymentengine.exception;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponseDTO(
        String error,
        String message,
        List<String> details,
        LocalDateTime timestamp
) {}
