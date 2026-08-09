package be.ephec.padel.matches.dto;

public record ParticipationDto(
        Long playerId,
        String matricule,
        String fullName,
        boolean paid
) {
}
