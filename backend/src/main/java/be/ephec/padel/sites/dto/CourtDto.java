package be.ephec.padel.sites.dto;

import be.ephec.padel.sites.Court;

public record CourtDto(Long id, int number) {

    public static CourtDto from(Court court) {
        return new CourtDto(court.getId(), court.getNumber());
    }
}
