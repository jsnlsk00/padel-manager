package be.ephec.padel.members;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByMatriculeIgnoreCase(String matricule);

    boolean existsByMatriculeIgnoreCase(String matricule);

    List<Member> findByType(MemberType type);

    /** Membres visibles depuis un site : les rattaches au site, plus tous les globaux et libres. */
    @Query("""
            select m from Member m
            where m.homeSite.id = :siteId or m.type <> be.ephec.padel.members.MemberType.SITE
            order by m.lastName, m.firstName
            """)
    List<Member> findVisibleFromSite(@Param("siteId") Long siteId);

    @Query("select count(m) from Member m where m.type = :type")
    long countByMemberType(@Param("type") MemberType type);
}
