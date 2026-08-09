package be.ephec.padel.matches.dto;

import be.ephec.padel.matches.MatchVisibility;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record CreateMatchRequest(

        @NotNull(message = "Le terrain est obligatoire")
        Long courtId,

        @NotNull(message = "L'heure de debut est obligatoire")
        LocalDateTime startTime,

        @NotNull(message = "La visibilite est obligatoire")
        MatchVisibility visibility,

        /** Uniquement pour un match prive : les 3 autres joueurs ajoutes par l'organisateur. */
        @Size(max = 3, message = "Un match compte 4 joueurs : au plus 3 invites")
        List<String> privatePlayersMatricules
) {
}
