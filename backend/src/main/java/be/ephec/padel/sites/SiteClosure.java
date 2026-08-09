package be.ephec.padel.sites;

import jakarta.persistence.*;

import java.time.LocalDate;

/** Jour de fermeture. Un site null signifie une fermeture globale du reseau. */
@Entity
@Table(name = "site_closure")
public class SiteClosure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "closed_on", nullable = false)
    private LocalDate closedOn;

    @Column(nullable = false, length = 200)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private Site site;

    protected SiteClosure() {
    }

    public SiteClosure(LocalDate closedOn, String reason, Site site) {
        this.closedOn = closedOn;
        this.reason = reason;
        this.site = site;
    }

    public boolean isGlobal() {
        return site == null;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getClosedOn() {
        return closedOn;
    }

    public String getReason() {
        return reason;
    }

    public Site getSite() {
        return site;
    }
}
