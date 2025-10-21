package model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NewsCollection {

    private List<News> newsCollection;

    public NewsCollection() {
        this.newsCollection = new ArrayList<>();
    }

    public NewsCollection(List<News> newsCollection) {
        this.newsCollection = newsCollection;
    }

    public void add(News news) {
        newsCollection.add(news);
    }

    public void addAll(List<News> news) {
        newsCollection.addAll(news);
    }

    public List<News> getNewsCollection() {
        return newsCollection;
    }

    public int size() {
        return newsCollection.size();
    }

    public List<News> filterByCategory(String category) {
        return newsCollection.stream()
                .filter(news -> news.getCategoryScores().stream()
                        .anyMatch(score -> score.getCategory().equalsIgnoreCase(category) && score.getScore() > 0))
                .collect(Collectors.toList());
    }

    // Méthode utile : trier par score dans une catégorie donnée
    public List<News> sortByCategoryScoreDesc(String category) {
        return newsCollection.stream()
                .sorted((n1, n2) -> Integer.compare(
                        n2.getScoreFor(category),
                        n1.getScoreFor(category)
                ))
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return newsCollection.toString();
    }

}
