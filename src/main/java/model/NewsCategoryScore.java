package model;

public class NewsCategoryScore {
    private final String category;
    private final int score; // 0 à 4

    public NewsCategoryScore(String category, int score) {
        this.category = category;
        this.score = Math.max(0, Math.min(score, 4)); // clamp entre 0 et 4
    }

    public String getCategory() {
        return category;
    }

    public int getScore() {
        return score;
    }

    @Override
    public String toString() {
        return category + ": " + score;
    }
}
