package be.ephec.padel.members.dto;

import be.ephec.padel.members.Member;
import be.ephec.padel.members.MemberType;
import be.ephec.padel.members.Role;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MemberDto(
        Long id,
        String matricule,
        String firstName,
        String lastName,
        String email,
        MemberType type,
        int bookingWindowDays,
        Long homeSiteId,
        String homeSiteName,
        Long adminSiteId,
        BigDecimal balanceDue,
        LocalDate bannedUntil,
        List<Role> roles
) {

    public static MemberDto from(Member member) {
        return new MemberDto(
                member.getId(),
                member.getMatricule(),
                member.getFirstName(),
                member.getLastName(),
                member.getEmail(),
                member.getType(),
                member.getType().getBookingWindowDays(),
                member.getHomeSite() == null ? null : member.getHomeSite().getId(),
                member.getHomeSite() == null ? null : member.getHomeSite().getName(),
                member.getAdminSite() == null ? null : member.getAdminSite().getId(),
                member.getBalanceDue(),
                member.getBannedUntil(),
                List.copyOf(member.getRoles()));
    }
}
