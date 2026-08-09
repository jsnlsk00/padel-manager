package be.ephec.padel.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(

        @NotBlank(message = "Le matricule est obligatoire")
        @Pattern(regexp = "^[GSLgsl][0-9]{4,5}$",
                message = "Format attendu : G, S ou L suivi de 4 ou 5 chiffres")
        String matricule,

        @NotBlank(message = "Le mot de passe est obligatoire")
        String password
) {
}
