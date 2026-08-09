package be.ephec.padel.sites.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateClosureRequest(
        @NotNull(message = "La date est obligatoire") LocalDate closedOn,
        @NotBlank(message = "Le motif est obligatoire") String reason
) {
}
