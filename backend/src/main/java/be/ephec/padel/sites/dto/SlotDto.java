package be.ephec.padel.sites.dto;

import be.ephec.padel.matches.dto.MatchDto;

import java.time.LocalDateTime;

/** Une case du planning : le creneau theorique et le match qui l'occupe, s'il existe. */
public record SlotDto(LocalDateTime startTime, Long courtId, int courtNumber, MatchDto match) {
}
