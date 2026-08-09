package be.ephec.padel.members;

import be.ephec.padel.sites.Site;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(name = "member")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String matricule;

    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    /** Hash BCrypt : le mot de passe n'est jamais stocke en clair. */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MemberType type;

    /** Site de rattachement, obligatoire pour un membre de type SITE. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_site_id")
    private Site homeSite;

    /** Site administre, pour un ROLE_ADMIN_SITE. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_site_id")
    private Site adminSite;

    /** Solde du : bloque toute nouvelle reservation tant qu'il est non nul (R4). */
    @Column(name = "balance_due", nullable = false, precision = 8, scale = 2)
    private BigDecimal balanceDue = BigDecimal.ZERO;

    /** Penalite d'une semaine appliquee a l'organisateur d'un match non rempli (R3). */
    @Column(name = "banned_until")
    private LocalDate bannedUntil;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "member_role", joinColumns = @JoinColumn(name = "member_id"))
    @Column(name = "role", nullable = false, length = 24)
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = EnumSet.of(Role.ROLE_USER);

    protected Member() {
    }

    public Member(String matricule, String firstName, String lastName, String email,
                  String passwordHash, MemberType type, Site homeSite) {
        this.matricule = matricule;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.type = type;
        this.homeSite = homeSite;
    }

    public boolean hasActivePenalty(LocalDate today) {
        return bannedUntil != null && !bannedUntil.isBefore(today);
    }

    public boolean owesBalance() {
        return balanceDue != null && balanceDue.compareTo(BigDecimal.ZERO) > 0;
    }

    public void addBalance(BigDecimal amount) {
        this.balanceDue = this.balanceDue.add(amount);
    }

    public void clearBalance() {
        this.balanceDue = BigDecimal.ZERO;
    }

    public void applyOneWeekPenalty(LocalDate from) {
        this.bannedUntil = from.plusWeeks(1);
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public Long getId() {
        return id;
    }

    public String getMatricule() {
        return matricule;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public MemberType getType() {
        return type;
    }

    public Site getHomeSite() {
        return homeSite;
    }

    public void setHomeSite(Site homeSite) {
        this.homeSite = homeSite;
    }

    public Site getAdminSite() {
        return adminSite;
    }

    public void setAdminSite(Site adminSite) {
        this.adminSite = adminSite;
    }

    public BigDecimal getBalanceDue() {
        return balanceDue;
    }

    public LocalDate getBannedUntil() {
        return bannedUntil;
    }

    public void setBannedUntil(LocalDate bannedUntil) {
        this.bannedUntil = bannedUntil;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }
}
