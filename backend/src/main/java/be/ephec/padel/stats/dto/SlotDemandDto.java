package be.ephec.padel.stats.dto;

import java.time.LocalTime;

public record SlotDemandDto(LocalTime startTime, long matches) {
}
