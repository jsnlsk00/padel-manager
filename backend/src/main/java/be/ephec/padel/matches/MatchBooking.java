package be.ephec.padel.matches;

import be.ephec.padel.members.Member;
import be.ephec.padel.sites.Court;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Reservation d'un terrain sur un creneau. Un match dure 1h30 (R1), coute 60 EUR
 * repartis entre 4 joueurs (R11) et appartient a un organisateur responsable.
 */
@Entity
@Table(name = "match_booking",
        uniqueConstraints = @UniqueConstraint(columnNames = {"court_id", "start_time"}))
public class MatchBooking {

    /** Duree d'un match. */
    public static final Duration DURATION = Duration.ofMinutes(90);

    /** Battement entre deux matches sur un meme terrain. */
    public static final Duration BREAK = Duration.ofMinutes(15);

    /** Pas de la grille horaire : 1h30 de jeu + 15 min de battement. */
    public static final Duration SLOT_STEP = DURATION.plus(BREAK);

    public static final int REQUIRED_PLAYERS = 4;

    public static final BigDecimal PRICE = new BigDecimal("60.00");

    public static final BigDecimal SHARE = new BigDecimal("15.00");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "court_id", nullable = false)
    private Court court;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "organizer_id", nullable = false)
    private Member organizer;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MatchVisibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private MatchStatus status = MatchStatus.SCHEDULED;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal price = PRICE;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<MatchParticipation> participations = new ArrayList<>();

    protected MatchBooking() {
    }

    public MatchBooking(Court court, Member organizer, LocalDateTime startTime, MatchVisibility visibility) {
        this.court = court;
        this.organizer = organizer;
        this.startTime = startTime;
        this.endTime = startTime.plus(DURATION);
        this.visibility = visibility;
    }

    public MatchParticipation addPlayer(Member player, boolean paid) {
        MatchParticipation participation = new MatchParticipation(this, player, paid);
        participations.add(participation);
        return participation;
    }

    public Optional<MatchParticipation> participationOf(Long memberId) {
        return participations.stream()
                .filter(p -> p.getPlayer().getId().equals(memberId))
                .findFirst();
    }

    public boolean hasPlayer(Long memberId) {
        return participationOf(memberId).isPresent();
    }

    public long paidCount() {
        return participations.stream().filter(MatchParticipation::isPaid).count();
    }

    public int freeSlots() {
        return REQUIRED_PLAYERS - participations.size();
    }

    public boolean isFull() {
        return participations.size() >= REQUIRED_PLAYERS;
    }

    public boolean isFullyPaid() {
        return paidCount() == REQUIRED_PLAYERS;
    }

    /** Montant restant du sur le match, a charge de l'organisateur (R11). */
    public BigDecimal outstandingAmount() {
        long missing = REQUIRED_PLAYERS - paidCount();
        return SHARE.multiply(BigDecimal.valueOf(missing));
    }

    /** Retire les participations non payees : leur place redevient reservable. */
    public List<MatchParticipation> releaseUnpaidPlaces() {
        List<MatchParticipation> released = participations.stream()
                .filter(p -> !p.isPaid())
                .toList();
        participations.removeAll(released);
        return released;
    }

    public void switchToPublic() {
        this.visibility = MatchVisibility.PUBLIC;
    }

    public Long getId() {
        return id;
    }

    public Court getCourt() {
        return court;
    }

    public Member getOrganizer() {
        return organizer;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public MatchVisibility getVisibility() {
        return visibility;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public void setStatus(MatchStatus status) {
        this.status = status;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public List<MatchParticipation> getParticipations() {
        return participations;
    }
}
