package model;

import java.util.ArrayList;
import java.util.List;

public class News {
    private String title;
    private String link;
    private String description;
    private String htmlContent;

    private List<NewsCategoryScore> categoryScores;

    public News(String title, String link, String description, String htmlContent) {
        this.title = title;
        this.link = link;
        this.description = description;
        this.htmlContent = htmlContent;

        this.categoryScores = computeScores(); // tu peux faire une vraie logique IA plus tard
    }

    private List<NewsCategoryScore> computeScores() {
        List<NewsCategoryScore> scores = new ArrayList<>();

        // EXEMPLE STUPIDE à remplacer par une vraie logique de scoring
        int politiqueScore = 0;
        if (title.toLowerCase().contains("politique")) politiqueScore += 2;
        if (description.toLowerCase().contains("gouvernement")) politiqueScore += 1;
        if (htmlContent.toLowerCase().contains("élection")) politiqueScore += 1;

        scores.add(new NewsCategoryScore("Politique", Math.min(politiqueScore, 4)));

        // Tu peux ajouter d'autres catégories ici
        scores.add(new NewsCategoryScore("Positif", 0)); // par défaut

        return scores;
    }

    public List<NewsCategoryScore> getCategoryScores() {
        return categoryScores;
    }

    public int getScoreFor(String category) {
        return categoryScores.stream()
                .filter(c -> c.getCategory().equalsIgnoreCase(category))
                .map(NewsCategoryScore::getScore)
                .findFirst()
                .orElse(0);
    }

    @Override
    public String toString() {
        return title + "\n" + link + "\nCatégories: " + categoryScores + "\n";
    }
}
