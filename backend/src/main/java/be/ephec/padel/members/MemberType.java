package be.ephec.padel.members;

/**
 * Categorie de membre. Porte la fenetre de reservation (regle R2 du cahier des charges)
 * et le prefixe de matricule.
 */
public enum MemberType {

    /** Gxxxx : reserve sur tous les sites, 3 semaines a l'avance. */
    GLOBAL('G', 21),

    /** Sxxxxx : reserve uniquement sur son site, 2 semaines a l'avance. */
    SITE('S', 14),

    /** Lxxxxx : reserve sur tous les sites, 5 jours a l'avance. */
    FREE('L', 5);

    private final char prefix;
    private final int bookingWindowDays;

    MemberType(char prefix, int bookingWindowDays) {
        this.prefix = prefix;
        this.bookingWindowDays = bookingWindowDays;
    }

    public char getPrefix() {
        return prefix;
    }

    public int getBookingWindowDays() {
        return bookingWindowDays;
    }

    public boolean canBookOnAnySite() {
        return this != SITE;
    }

    public static MemberType fromMatricule(String matricule) {
        if (matricule == null || matricule.isBlank()) {
            throw new IllegalArgumentException("Matricule vide");
        }
        char first = Character.toUpperCase(matricule.charAt(0));
        for (MemberType type : values()) {
            if (type.prefix == first) {
                return type;
            }
        }
        throw new IllegalArgumentException("Prefixe de matricule inconnu : " + first);
    }
}
