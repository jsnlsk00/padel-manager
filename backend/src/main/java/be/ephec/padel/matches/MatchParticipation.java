package be.ephec.padel.matches;

import be.ephec.padel.members.Member;
import jakarta.persistence.*;

@Entity
@Table(name = "match_participation",
        uniqueConstraints = @UniqueConstraint(columnNames = {"match_id", "player_id"}))
public class MatchParticipation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private MatchBooking match;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Member player;

    @Column(nullable = false)
    private boolean paid;

    protected MatchParticipation() {
    }

    MatchParticipation(MatchBooking match, Member player, boolean paid) {
        this.match = match;
        this.player = player;
        this.paid = paid;
    }

    public Long getId() {
        return id;
    }

    public MatchBooking getMatch() {
        return match;
    }

    public Member getPlayer() {
        return player;
    }

    public boolean isPaid() {
        return paid;
    }

    public void markPaid() {
        this.paid = true;
    }
}
