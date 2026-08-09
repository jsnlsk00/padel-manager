package be.ephec.padel.stats.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UnpaidDto(
        Long matchId,
        LocalDateTime startTime,
        String siteName,
        String matricule,
        String fullName,
        String reason,
        BigDecimal amount
) {
}
