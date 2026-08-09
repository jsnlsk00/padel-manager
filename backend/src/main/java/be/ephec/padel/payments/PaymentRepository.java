package be.ephec.padel.payments;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByPayerIdOrderByPaidAtDesc(Long payerId);

    @Query("""
            select coalesce(sum(p.amount), 0) from Payment p
            where :siteId is null or p.match.court.site.id = :siteId
            """)
    BigDecimal sumRevenue(@Param("siteId") Long siteId);
}
