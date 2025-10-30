package api.dto;

/**
 * Sélection de thème basique avec le niveau.
 */
public final class ThemeSelection {

    /** Niveau d'intérêt de l'utilisateur pour ce thème. */
    private Integer level;

    /** Flux RSS associé (optionnel). */
    private String rss;

    /**
     * Constructeur par défaut requis pour la désérialisation JSON.
     */
    public ThemeSelection() {
        // Constructeur vide
    }

    /**
     * Constructeur utilitaire.
     *
     * @param levelInterest niveau d'intérêt du thème
     * @param rssLink   lien RSS associé au thème
     */
    public ThemeSelection(final Integer levelInterest, final String rssLink) {
        this.level = levelInterest;
        this.rss = rssLink;
    }

    /**
     * Retourne le niveau d'intérêt du thème.
     *
     * @return le niveau du thème
     */
    public Integer getLevel() {
        return level;
    }

    /**
     * Modifie le niveau d'intérêt du thème.
     *
     * @param newLevel nouveau niveau du thème
     */
    public void setLevel(final Integer newLevel) {
        this.level = newLevel;
    }

    /**
     * Retourne le lien RSS associé.
     *
     * @return le lien RSS
     */
    public String getRss() {
        return rss;
    }

    /**
     * Modifie le lien RSS associé.
     *
     * @param newRss nouveau lien RSS
     */
    public void setRss(final String newRss) {
        this.rss = newRss;
    }
}
