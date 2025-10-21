package model;

/**
 * POJO pour un score de catégorie.
 */
public class NewsCategoryScore {
    private String category;
    private int score; // 0 à 4

    /**
     * Constructeur par défaut (vide) requis par Jackson
     */
    public NewsCategoryScore() {
    }

    /**
     * Constructeur utilitaire
     */
    public NewsCategoryScore(String category, int score) {
        this.category = category;
        this.setScore(score);
    }


    public String getCategory() {
        return category;
    }

    public int getScore() {
        return score;
    }


    public void setCategory(String category) {
        this.category = category;
    }

    public void setScore(int score) {
        // contraindre la valeur entre 0 et 4
        this.score = Math.max(0, Math.min(score, 4));
    }

    @Override
    public String toString() {
        return category + ": " + score;
    }
}