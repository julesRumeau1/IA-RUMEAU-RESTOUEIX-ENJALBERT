package model;

/**
 * POJO pour représenter un score associé à une catégorie.
 */
public final class NewsCategoryScore {

    /** Nom de la catégorie. */
    private String category;

    /** Score associé à la catégorie (entre 0 et 4). */
    private int score;

    /** Valeur maximale du score autorisée. */
    private static final int MAX_SCORE = 4;

    /**
     * Constructeur par défaut requis pour la désérialisation JSON.
     */
    public NewsCategoryScore() {
        // constructeur vide
    }

    /**
     * Constructeur utilitaire.
     *
     * @param categoryName nom de la catégorie
     * @param categoryScore    score associé à la catégorie
     */
    public NewsCategoryScore(final String categoryName,
                             final int categoryScore) {
        this.category = categoryName;
        this.score = categoryScore;
        this.setScore(score);
    }

    /**
     * Retourne le nom de la catégorie.
     *
     * @return le nom de la catégorie
     */
    public String getCategory() {
        return category;
    }

    /**
     * Modifie le nom de la catégorie.
     *
     * @param newCategory nouvelle catégorie
     */
    public void setCategory(final String newCategory) {
        this.category = newCategory;
    }

    /**
     * Retourne le score associé à la catégorie.
     *
     * @return le score de la catégorie
     */
    public int getScore() {
        return score;
    }

    /**
     * Modifie le score associé à la catégorie.
     * La valeur est contrainte entre 0 et {@link #MAX_SCORE}.
     *
     * @param newScore nouveau score à affecter
     */
    public void setScore(final int newScore) {
        this.score = Math.max(0, Math.min(newScore, MAX_SCORE));
    }

    /**
     * Représentation textuelle du score.
     *
     * @return une chaîne contenant la catégorie et le score
     */
    @Override
    public String toString() {
        return category + ": " + score;
    }
}
