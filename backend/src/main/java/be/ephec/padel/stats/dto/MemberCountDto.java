package be.ephec.padel.stats.dto;

import be.ephec.padel.members.MemberType;

public record MemberCountDto(MemberType type, String label, long count) {
}
