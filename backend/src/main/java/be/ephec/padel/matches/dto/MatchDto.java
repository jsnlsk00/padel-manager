package be.ephec.padel.matches.dto;

import be.ephec.padel.matches.MatchBooking;
import be.ephec.padel.matches.MatchStatus;
import be.ephec.padel.matches.MatchVisibility;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record MatchDto(
        Long id,
        Long siteId,
        String siteName,
        Long courtId,
        int courtNumber,
        LocalDateTime startTime,
        LocalDateTime endTime,
        MatchVisibility visibility,
        MatchStatus status,
        BigDecimal price,
        Long organizerId,
        String organizerName,
        List<ParticipationDto> participants,
        long paidParticipantsCount,
        int freeSlots
) {

    public static MatchDto from(MatchBooking match) {
        List<ParticipationDto> participants = match.getParticipations().stream()
                .map(p -> new ParticipationDto(
                        p.getPlayer().getId(),
                        p.getPlayer().getMatricule(),
                        p.getPlayer().getFullName(),
                        p.isPaid()))
                .toList();

        return new MatchDto(
                match.getId(),
                match.getCourt().getSite().getId(),
                match.getCourt().getSite().getName(),
                match.getCourt().getId(),
                match.getCourt().getNumber(),
                match.getStartTime(),
                match.getEndTime(),
                match.getVisibility(),
                match.getStatus(),
                match.getPrice(),
                match.getOrganizer().getId(),
                match.getOrganizer().getFullName(),
                participants,
                match.paidCount(),
                match.freeSlots());
    }
}
