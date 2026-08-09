package be.ephec.padel.payments;

import be.ephec.padel.matches.MatchBooking;
import be.ephec.padel.members.Member;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private MatchBooking match;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "payer_id", nullable = false)
    private Member payer;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal amount;

    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt;

    /** Vrai quand le versement solde une dette et non une part de match. */
    @Column(name = "is_balance_payment", nullable = false)
    private boolean balancePayment;

    protected Payment() {
    }

    public Payment(MatchBooking match, Member payer, BigDecimal amount,
                   LocalDateTime paidAt, boolean balancePayment) {
        this.match = match;
        this.payer = payer;
        this.amount = amount;
        this.paidAt = paidAt;
        this.balancePayment = balancePayment;
    }

    public Long getId() {
        return id;
    }

    public MatchBooking getMatch() {
        return match;
    }

    public Member getPayer() {
        return payer;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public boolean isBalancePayment() {
        return balancePayment;
    }
}
