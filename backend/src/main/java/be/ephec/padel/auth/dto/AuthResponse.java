package be.ephec.padel.auth.dto;

import be.ephec.padel.members.MemberType;
import be.ephec.padel.members.Role;

import java.util.List;

public record AuthResponse(
        String token,
        String refreshToken,
        String matricule,
        String firstName,
        String lastName,
        MemberType memberType,
        Long homeSiteId,
        Long adminSiteId,
        List<Role> roles
) {
}
