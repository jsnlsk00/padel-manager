package be.ephec.padel.stats.dto;

import java.math.BigDecimal;

public record SiteStatsDto(
        Long siteId,
        String siteName,
        int courts,
        long matches,
        int occupancyRate,
        BigDecimal revenue
) {
}
