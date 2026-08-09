package be.ephec.padel.sites;

import jakarta.persistence.*;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "site")
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @Column(nullable = false, length = 200)
    private String address;

    /** Heure du premier creneau, propre a chaque site. */
    @Column(name = "opening_time", nullable = false)
    private LocalTime openingTime;

    /** Heure de fin du dernier creneau, propre a chaque site. */
    @Column(name = "closing_time", nullable = false)
    private LocalTime closingTime;

    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("number asc")
    private List<Court> courts = new ArrayList<>();

    protected Site() {
    }

    public Site(String name, String address, LocalTime openingTime, LocalTime closingTime) {
        this.name = name;
        this.address = address;
        this.openingTime = openingTime;
        this.closingTime = closingTime;
    }

    public Court addCourt(int number) {
        Court court = new Court(number, this);
        courts.add(court);
        return court;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public LocalTime getOpeningTime() {
        return openingTime;
    }

    public LocalTime getClosingTime() {
        return closingTime;
    }

    public List<Court> getCourts() {
        return courts;
    }
}
