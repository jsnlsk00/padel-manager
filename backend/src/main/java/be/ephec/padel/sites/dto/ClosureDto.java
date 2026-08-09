package be.ephec.padel.sites.dto;

import be.ephec.padel.sites.SiteClosure;

import java.time.LocalDate;

public record ClosureDto(Long id, LocalDate closedOn, String reason, boolean global) {

    public static ClosureDto from(SiteClosure closure) {
        return new ClosureDto(closure.getId(), closure.getClosedOn(),
                closure.getReason(), closure.isGlobal());
    }
}
