package api.service;

import api.PreferencesApi;
import api.util.ApiException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import model.News;
import model.NewsCategoryScore;
import model.NewsCollection;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class NewsSorter {

    private NewsSorter () {
        // utilitaire
    }

    /** Logger de la classe. */
    private static final Logger LOGGER =
            Logger.getLogger(PreferencesApi.class.getName());

    /** Score appliqué si la news n'a aucune catégorie. */
    private static final int NO_CATEGORY_SCORE = -1000;

    /** Score appliqué si la news n'a pas matché l'une des prefs. */
    private static final int NO_MATCH_SCORE = -500;


    /** Poids par niveau de préférence utilisateur. */
    private static final Map<Integer, Integer> PREFERENCE_WEIGHTS = Map.of(
            1, -5,
            2, -1,
            3, 1,
            4, 3,
            5, 5
    );


    public static List<News> sortByPreferences(
            NewsCollection newsCollection,
            Map<String, Integer> userPreferences
    ) throws ApiException {

        if (newsCollection == null || userPreferences == null) {
            throw new ApiException(
                    "invalid_sort_input",
                    "La collection de news ou les préférences utilisateur sont nulles"
            );
        }

        List<News> allNews = newsCollection.getNewsCollection();
        if (allNews.isEmpty()) {
            throw new ApiException(
                    "empty_news_collection",
                    "La collection de news est vide, impossible de trier"
            );
        }
        try {
            allNews.sort((news1, news2) -> {
                int score1 = calculateMatchScore(news1, userPreferences);
                int score2 = calculateMatchScore(news2, userPreferences);
                return Integer.compare(score2, score1);
            });
        } catch (Exception ex) {
            throw new ApiException(
                    "sorting_failed",
                    "Échec du tri des articles : " + ex.getMessage()
            );
        }

        LOGGER.info("--- Tri final des articles par score ---");
        StringBuilder sortedLog = new StringBuilder();
        for (News news : allNews) {
            int score = calculateMatchScore(news, userPreferences);
            sortedLog.append(String.format(
                    "  Score: %-5d | Titre: %s%n",
                    score,
                    news.getTitle()
            ));
        }
        LOGGER.info(sortedLog.toString());

        // ne garde que les news ou le score est supérieur a 0
        List<News> filteredNews = new ArrayList<>();
        for (News news : allNews) {
            if (calculateMatchScore(news, userPreferences) >= 0) {
                filteredNews.add(news);
            }
        }

        if (filteredNews.isEmpty()) {
            throw new ApiException(
                    "no_matching_news",
                    "Aucun article ne correspond aux préférences utilisateur"
            );
        }

        return filteredNews;
    }

    /**
     * Calcule le score pour une news en fonction des préférences
     * utilisateurs.
     *
     * @param news la news pour laquelle on calcule le score
     * @param userPreferences les préférences de l'utilisateur
     * @return le score de "match" pour cette news
     */
    private static int calculateMatchScore(
            final News news,
            final Map<String, Integer> userPreferences) {

        if (news.getCategoryScores() == null
                || news.getCategoryScores().isEmpty()) {
            return NO_CATEGORY_SCORE;
        }
        int totalScore = 0;
        boolean matched = false;

        for (NewsCategoryScore entry : news.getCategoryScores()) {
            String category = entry.getCategory();
            int relevance = entry.getScore();
            if (userPreferences.containsKey(category)) {
                matched = true;
                int prefLevel = userPreferences.get(category);
                int prefWeight =
                        PREFERENCE_WEIGHTS.getOrDefault(prefLevel,
                                0);

                totalScore += prefWeight * relevance;
            }
        }
        if (!matched) {
            return NO_MATCH_SCORE;
        }
        return totalScore;
    }
}
