package be.ephec.padel.matches;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MatchParticipationRepository extends JpaRepository<MatchParticipation, Long> {

    List<MatchParticipation> findByMatchId(Long matchId);

    @Query("""
            select p from MatchParticipation p
            where p.paid = false and (:siteId is null or p.match.court.site.id = :siteId)
            order by p.match.startTime asc
            """)
    List<MatchParticipation> findUnpaid(@Param("siteId") Long siteId);
}
