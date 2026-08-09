package be.ephec.padel.common.dto;

import java.time.LocalDateTime;
import java.util.Map;

/** Corps d'erreur uniforme renvoye par le GlobalExceptionHandler. */
public record ApiError(
        int status,
        String error,
        String message,
        String path,
        LocalDateTime timestamp,
        Map<String, String> fieldErrors
) {
    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(status, error, message, path, LocalDateTime.now(), null);
    }

    public static ApiError validation(String path, Map<String, String> fieldErrors) {
        return new ApiError(400, "Bad Request", "Donnees invalides", path,
                LocalDateTime.now(), fieldErrors);
    }
}
