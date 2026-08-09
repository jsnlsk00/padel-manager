package be.ephec.padel.sites.dto;

import be.ephec.padel.sites.Site;

import java.time.LocalTime;
import java.util.List;

public record SiteDto(
        Long id,
        String name,
        String address,
        LocalTime openingTime,
        LocalTime closingTime,
        List<CourtDto> courts
) {

    public static SiteDto from(Site site) {
        return new SiteDto(
                site.getId(),
                site.getName(),
                site.getAddress(),
                site.getOpeningTime(),
                site.getClosingTime(),
                site.getCourts().stream().map(CourtDto::from).toList());
    }
}
