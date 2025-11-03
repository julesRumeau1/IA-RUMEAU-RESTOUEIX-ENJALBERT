package api.service;

import api.PreferencesApi;
import api.util.ApiException;
import model.News;
import model.NewsCategoryScore;
import model.NewsCollection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

final class NewsSorter {

    private NewsSorter() {
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
            3, 3,
            4, 5,
            5, 10
    );


    /**
     * Trie une collection de news en fonction des préférences utilisateur.
     * <p>
     * Chaque article reçoit un score calculé via
     * {@link #calculateMatchScore(News, Map)},
     * et la liste est triée par ordre décroissant de score. Les articles
     * dont le score
     * est inférieur à 0 sont filtrés. Les logs détaillent le score final
     * de chaque article.
     * </p>
     *
     * @param newsCollection   la collection de news à trier ; ne peut pas
     *                         être {@code null}
     * @param userPreferences  map des préférences utilisateur
     *                         (nom de catégorie -> poids) ;
     *                         ne peut pas être {@code null}
     * @return une liste de {@link News} triée par score de correspondance
     * avec les préférences utilisateur
     * @throws ApiException si la collection de news ou les préférences
     * sont nulles, si la collection est vide,
     *                      si le tri échoue, ou si aucun article ne
     *                      correspond aux préférences
     */
    public static List<News> sortByPreferences(
            final NewsCollection newsCollection,
            final Map<String, Integer> userPreferences
    ) throws ApiException {

        if (newsCollection == null || userPreferences == null) {
            throw new ApiException(
                    "invalid_sort_input",
                    "La collection de news ou les préférences"
                            + " utilisateur sont nulles"
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
