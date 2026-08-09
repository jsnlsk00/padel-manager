package be.ephec.padel.stats.dto;

import java.math.BigDecimal;
import java.util.List;

/** Tableau de bord administrateur, pour un site ou pour l'ensemble du reseau. */
public record StatsDto(
        Long siteId,
        String scope,
        long totalMatches,
        long publicMatches,
        long privateMatches,
        long cancelledMatches,
        long playedMatches,
        BigDecimal revenue,
        BigDecimal outstanding,
        int occupancyRate,
        long totalMembers,
        List<MemberCountDto> membersByType,
        List<SiteStatsDto> perSite,
        List<SlotDemandDto> slotDemand,
        List<UnpaidDto> unpaid
) {
}
