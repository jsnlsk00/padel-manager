package be.ephec.padel.matches;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<MatchBooking, Long> {

    boolean existsByCourtIdAndStartTime(Long courtId, LocalDateTime startTime);

    Optional<MatchBooking> findByCourtIdAndStartTime(Long courtId, LocalDateTime startTime);

    @Query("""
            select m from MatchBooking m
            where m.court.site.id = :siteId
              and m.startTime >= :from and m.startTime < :to
            order by m.startTime asc, m.court.number asc
            """)
    List<MatchBooking> findBySiteBetween(@Param("siteId") Long siteId,
                                        @Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to);

    @Query("""
            select m from MatchBooking m
            where m.visibility = be.ephec.padel.matches.MatchVisibility.PUBLIC
              and m.status = be.ephec.padel.matches.MatchStatus.SCHEDULED
              and m.startTime > :now
              and (:siteId is null or m.court.site.id = :siteId)
            order by m.startTime asc
            """)
    List<MatchBooking> findOpenPublicMatches(@Param("siteId") Long siteId, @Param("now") LocalDateTime now);

    @Query("""
            select distinct m from MatchBooking m
            left join m.participations p
            where p.player.id = :memberId or m.organizer.id = :memberId
            order by m.startTime asc
            """)
    List<MatchBooking> findAllForMember(@Param("memberId") Long memberId);

    /** Matches a traiter par le job de bascule (R9) : la veille du match. */
    @Query("""
            select m from MatchBooking m
            where m.status = be.ephec.padel.matches.MatchStatus.SCHEDULED
              and m.startTime > :now and m.startTime <= :limit
            """)
    List<MatchBooking> findScheduledStartingBefore(@Param("now") LocalDateTime now,
                                                   @Param("limit") LocalDateTime limit);

    @Query("""
            select m from MatchBooking m
            where m.status = be.ephec.padel.matches.MatchStatus.SCHEDULED
              and m.startTime <= :now
            """)
    List<MatchBooking> findScheduledAlreadyStarted(@Param("now") LocalDateTime now);

    @Query("select m from MatchBooking m where (:siteId is null or m.court.site.id = :siteId)")
    List<MatchBooking> findAllForStats(@Param("siteId") Long siteId);
}
