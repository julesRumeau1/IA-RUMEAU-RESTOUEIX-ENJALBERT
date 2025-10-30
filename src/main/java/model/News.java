package model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * POJO représentant une news.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class News {

    /** Titre de la news. */
    private String title;

    /** Lien vers la news. */
    private String link;

    /** Description ou chapeau de la news. */
    private String description;

    /** Scores de catégories associés à la news. */
    private List<NewsCategoryScore> categoryScores;

    /**
     * Constructeur par défaut requis par Jackson.
     */
    public News() {
        // constructeur vide
    }

    /**
     * Constructeur utilitaire.
     *
     * @param newsTitle       titre de la news
     * @param newsLink        lien de la news
     * @param newsDescription description de la news
     */
    public News(final String newsTitle, final String newsLink,
                final String newsDescription) {
        this.title = newsTitle;
        this.link = newsLink;
        this.description = newsDescription;
    }

    /**
     * Retourne le titre de la news.
     *
     * @return le titre
     */
    public String getTitle() {
        return title;
    }

    /**
     * Modifie le titre de la news.
     *
     * @param newTitle nouveau titre
     */
    public void setTitle(final String newTitle) {
        this.title = newTitle;
    }

    /**
     * Retourne le lien de la news.
     *
     * @return le lien
     */
    public String getLink() {
        return link;
    }

    /**
     * Modifie le lien de la news.
     *
     * @param newLink nouveau lien
     */
    public void setLink(final String newLink) {
        this.link = newLink;
    }

    /**
     * Retourne la description de la news.
     *
     * @return la description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Modifie la description de la news.
     *
     * @param newDescription nouvelle description
     */
    public void setDescription(final String newDescription) {
        this.description = newDescription;
    }

    /**
     * Retourne les scores de catégorie.
     *
     * @return la liste des scores de catégorie
     */
    public List<NewsCategoryScore> getCategoryScores() {
        return categoryScores;
    }

    /**
     * Modifie les scores de catégorie.
     *
     * @param newCategoryScores nouvelle liste de scores
     */
    public void setCategoryScores(final List<NewsCategoryScore>
                                          newCategoryScores) {
        this.categoryScores = newCategoryScores;
    }

    /**
     * Méthode utilitaire pour récupérer un score (si besoin).
     *
     * @param category la catégorie recherchée
     * @return le score associé ou 0 si absent
     */
    public int getScoreFor(final String category) {
        if (categoryScores == null) {
            return 0;
        }
        return categoryScores.stream()
                .filter(c -> c.getCategory()
                        .equalsIgnoreCase(category))
                .map(NewsCategoryScore::getScore)
                .findFirst()
                .orElse(0);
    }

    /**
     * Représentation textuelle de la news.
     *
     * @return représentation textuelle
     */
    @Override
    public String toString() {
        return "News{"
                + "title='" + title + '\''
                + ", scores=" + categoryScores
                + '}';
    }
}
