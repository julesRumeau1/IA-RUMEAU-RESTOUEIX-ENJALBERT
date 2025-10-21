package model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * POJO représentant une news.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class News {

    private String title;
    private String link;
    private String description;

    private List<NewsCategoryScore> categoryScores;

    /**
     * Constructeur par défaut requis par Jackson
     */
    public News() {
    }

    /**
     * Constructeur utilitaire
     */
    public News(String title, String link, String description) {
        this.title = title;
        this.link = link;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<NewsCategoryScore> getCategoryScores() {
        return categoryScores;
    }

    public void setCategoryScores(List<NewsCategoryScore> categoryScores) {
        this.categoryScores = categoryScores;
    }

    /**
     * Méthode utilitaire pour récupérer un score (si besoin)
     */
    public int getScoreFor(String category) {
        if (categoryScores == null) {
            return 0;
        }
        return categoryScores.stream()
                .filter(c -> c.getCategory().equalsIgnoreCase(category))
                .map(NewsCategoryScore::getScore)
                .findFirst()
                .orElse(0);
    }

    @Override
    public String toString() {
        return "News{" +
                "title='" + title + '\'' +
                ", scores=" + categoryScores +
                '}';
    }
}