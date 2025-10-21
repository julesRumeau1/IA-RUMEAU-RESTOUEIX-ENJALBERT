package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Conteneur pour une liste de NewsCategoryScore.
 */
public class NewsCategoryScoreCollection {

    private List<NewsCategoryScore> scores;

    /**
     * Constructeur par défaut requis par Jackson
     */
    public NewsCategoryScoreCollection() {
        this.scores = new ArrayList<>();
    }

    /**
     * Constructeur utilitaire
     */
    public NewsCategoryScoreCollection(List<NewsCategoryScore> scores) {
        this.scores = scores;
    }

    public List<NewsCategoryScore> getScores() {
        return scores;
    }

    public void setScores(List<NewsCategoryScore> scores) {
        this.scores = scores;
    }

    public void addScore(NewsCategoryScore score) {
        this.scores.add(score);
    }

    @Override
    public String toString() {
        return scores.toString();
    }
}
