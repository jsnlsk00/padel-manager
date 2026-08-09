package be.ephec.padel.sites;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SiteClosureRepository extends JpaRepository<SiteClosure, Long> {

    /** Fermetures applicables a un site : les siennes et les fermetures globales. */
    @Query("""
            select c from SiteClosure c
            where c.site is null or c.site.id = :siteId
            order by c.closedOn asc
            """)
    List<SiteClosure> findApplicableToSite(@Param("siteId") Long siteId);

    @Query("""
            select count(c) from SiteClosure c
            where c.closedOn = :day and (c.site is null or c.site.id = :siteId)
            """)
    long countClosuresOn(@Param("siteId") Long siteId, @Param("day") LocalDate day);
}
