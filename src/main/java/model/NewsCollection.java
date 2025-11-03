package model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Collection de news.
 */
public final class NewsCollection {

    /** Liste interne des news. */
    private List<News> newsCollection;

    /**
     * Constructeur vide.
     */
    public NewsCollection() {
        this.newsCollection = new ArrayList<>();
    }

    /**
     * Constructeur avec liste initiale.
     *
     * @param initialNewsCollection liste de news initiale
     */
    public NewsCollection(final List<News> initialNewsCollection) {
        if (initialNewsCollection == null) {
            this.newsCollection = new ArrayList<>();
        } else {
            // Crée une copie
            this.newsCollection = new ArrayList<>(initialNewsCollection);
        }
    }

    /**
     * Ajoute une news à la collection.
     *
     * @param news news à ajouter
     */
    public void add(final News news) {
        newsCollection.add(news);
    }

    /**
     * Ajoute plusieurs news à la collection.
     *
     * @param news liste de news à ajouter
     */
    public void addAll(final List<News> news) {
        newsCollection.addAll(news);
    }

    /**
     * Retourne la liste des news.
     *
     * @return la liste des news
     */
    public List<News> getNewsCollection() {
        // Retourne une COPIE de la liste interne.
        // L'appelant peut modifier cette copie sans affecter l'originale.
        return new ArrayList<>(this.newsCollection);
    }

    /**
     * Retourne le nombre de news.
     *
     * @return la taille de la collection
     */
    public int size() {
        return newsCollection.size();
    }

    /**
     * Filtre les news qui ont la catégorie donnée avec un score &gt; 0.
     *
     * @param category catégorie à filtrer
     * @return la liste des news correspondant à la catégorie
     */
    public List<News> filterByCategory(final String category) {
        return newsCollection.stream()
                .filter(news -> news.getCategoryScores().stream()
                        .anyMatch(score ->
                                score.getCategory()
                                        .equalsIgnoreCase(category)
                                        && score.getScore() > 0))
                .collect(Collectors.toList());
    }

    /**
     * Trie les news par score décroissant pour une catégorie donnée.
     *
     * @param category catégorie à utiliser pour le tri
     * @return la liste triée
     */
    public List<News> sortByCategoryScoreDesc(final String category) {
        return newsCollection.stream()
                .sorted((news1, news2) -> Integer.compare(
                        news2.getScoreFor(category),
                        news1.getScoreFor(category)))
                .collect(Collectors.toList());
    }

    /**
     * Représentation textuelle de la collection.
     *
     * @return représentation textuelle
     */
    @Override
    public String toString() {
        return newsCollection.toString();
    }
}
