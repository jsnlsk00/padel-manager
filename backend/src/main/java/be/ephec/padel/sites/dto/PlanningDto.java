package be.ephec.padel.sites.dto;

import java.time.LocalDate;
import java.util.List;

public record PlanningDto(
        Long siteId,
        String siteName,
        LocalDate day,
        boolean closed,
        String closureReason,
        List<CourtDto> courts,
        List<SlotDto> slots
) {
}
