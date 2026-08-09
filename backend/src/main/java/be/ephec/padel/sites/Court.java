package be.ephec.padel.sites;

import jakarta.persistence.*;

@Entity
@Table(name = "court", uniqueConstraints = @UniqueConstraint(columnNames = {"site_id", "number"}))
public class Court {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int number;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    protected Court() {
    }

    Court(int number, Site site) {
        this.number = number;
        this.site = site;
    }

    public Long getId() {
        return id;
    }

    public int getNumber() {
        return number;
    }

    public Site getSite() {
        return site;
    }
}
