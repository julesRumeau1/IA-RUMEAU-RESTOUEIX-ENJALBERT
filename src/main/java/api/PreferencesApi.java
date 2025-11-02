package api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import io.javalin.http.staticfiles.Location;
import model.News;
import model.NewsCategoryScore;
import model.NewsCollection;
import rss.LeMondeRSSFetcher;
import api.dto.ErrorResponse;
import api.dto.PreferencesRequest;
import api.util.CorsUtil;
import api.util.PreferencesUtils;

/**
 * API permettant de faire le lien entre le front et le back,
 * puis de gérer les préférences et le scoring.
 */
public final class PreferencesApi {

    /** Logger de la classe. */
    private static final Logger LOGGER =
            Logger.getLogger(PreferencesApi.class.getName());

    /** Nombre d'articles envoyés au LLM par lot. */
    private static final int LLM_BATCH_SIZE = 3;

    /** Timeout (en minutes) du LLM. */
    private static final int LLM_TIMEOUT_MINUTES = 5;

    /** Valeur HTTP pour "No Content". */
    private static final int HTTP_NO_CONTENT = 204;

    /** Score maximum retourné par le LLM. */
    private static final int MAX_LLM_SCORE = 4;

    /** Score appliqué si la news n'a aucune catégorie. */
    private static final int NO_CATEGORY_SCORE = -1000;

    /** Score appliqué si la news n'a pas matché l'une des prefs. */
    private static final int NO_MATCH_SCORE = -500;

    /** Url vers le conteneur ollama. */
    private static final String OLLAMAURL = System.getenv().getOrDefault("OLLAMA_HOST", "http://localhost:11434");

    /** Mapper JSON configuré. */
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                    false)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .registerModule(new JavaTimeModule());

    /** Modèle de LLM pour la catégorisation. */
    private static final ChatLanguageModel LLM = OllamaChatModel.builder()
            .baseUrl(OLLAMAURL)
            .modelName("qwen2.5:3b")
            .timeout(Duration.ofMinutes(LLM_TIMEOUT_MINUTES))
            .build();

    /** Poids par niveau de préférence utilisateur. */
    private static final Map<Integer, Integer> PREFERENCE_WEIGHTS = Map.of(
            1, -5,
            2, -1,
            3, 1,
            4, 3,
            5, 5
    );

    /** Type générique pour la réponse du LLM. */
    private static final TypeReference<Map<String, List<Integer>>>
            MAP_STRING_LIST_INT_TYPE = new TypeReference<>() { };

    private PreferencesApi() {
        // utilitaire
    }

    /**
     * Démarre le serveur API (est appelé depuis la classe main.Main à la
     * racine).
     *
     * @param args arguments
     */
    /**
     * Démarre le serveur API (est appelé depuis la classe main.Main à la
     * racine).
     *
     * @param args arguments
     */
    public static void main(final String[] args) {
        final int port = 8080;
        Javalin app = Javalin.create(cfg -> {
                    cfg.http.defaultContentType = "application/json";
                    cfg.plugins.enableDevLogging();

                    cfg.staticFiles.add(staticFiles -> {
                        staticFiles.hostedPath = "/";
                        staticFiles.directory = "/public";
                        staticFiles.location = Location.CLASSPATH;
                    });

                })
                .start(port);

        app.options("/api/preferences", ctx -> {
            CorsUtil.setCors(ctx);
            ctx.status(HTTP_NO_CONTENT);
        });

        app.post("/api/preferences", ctx -> {
            CorsUtil.setCors(ctx);
            handlePreferences(ctx);
        });

        app.get("/health", ctx -> ctx.result("ok"));

        LOGGER.info(() -> "API et Interface web démarrées: http://localhost:"
                + port);
    }

    /**
     * Permet de définir la structure d'une collection de news
     * (titre, lien, ...).
     *
     * @return la collection de news
     */
    private static NewsCollection initNewsCollection() {
        NewsCollection newsCollection = new NewsCollection();
        List<Map<String, String>> rawNews = LeMondeRSSFetcher.fetchRawNews();

        for (Map<String, String> newsData : rawNews) {
            String title = newsData.get("title");
            String link = newsData.get("link");
            String description = newsData.get("description");
            News news = new News(title, link, description);
            newsCollection.add(news);
        }
        return newsCollection;
    }

    /**
     * Permet de gérer les préférences de l'utilisateur et de les
     * envoyer au modèle.
     *
     * @param ctx contexte de la requête HTTP envoyé depuis le front
     */
    private static void handlePreferences(final Context ctx) {
        try {
            NewsCollection newsCollection = initNewsCollection();
            PreferencesRequest req = MAPPER.readValue(
                    ctx.body(), PreferencesRequest.class);
            Map<String, Integer> userPreferences =
                    PreferencesUtils.flattenPreferences(req.getThemes());

            // La liste des catégories
            List<String> orderedCategories =
                    new ArrayList<>(userPreferences.keySet());

            NewsCollection categorizedNews =
                    categorizeNewsWithLLM(newsCollection, orderedCategories);

            if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
                try {
                    LOGGER.fine("--- JSON après catégorisation ---\n"
                            + MAPPER.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(categorizedNews));
                } catch (JsonProcessingException ignored) {
                    // pas d'erreur loggée
                }
            }

            List<News> sortedNews =
                    sortNewsByPreference(categorizedNews, userPreferences);

            if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
                try {
                    LOGGER.fine("--- JSON après tri ---\n"
                            + MAPPER.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(sortedNews));
                } catch (JsonProcessingException ignored) {
                    // pas d'erreur loggée
                }
            }

            ctx.status(HttpStatus.OK).json(new NewsCollection(sortedNews));

        } catch (JsonProcessingException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                    .json(new ErrorResponse("invalid_json",
                            e.getOriginalMessage()));
        } catch (Exception e) {
            LOGGER.severe(e::getMessage);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(new ErrorResponse("server_error",
                            e.getMessage()));
        }
    }

    /**
     * Catégorise chaque nouvelles news en utilisant un modèle et
     * une liste de catégorie.
     *
     * @param newsCollection   collection de news
     * @param orderedCategories catégories de news
     *                          (l'ordre est important)
     * @return la collection de news catégorisée
     * @throws Exception si problème lors de la catégorisation
     */
    private static NewsCollection categorizeNewsWithLLM(
            final NewsCollection newsCollection,
            final List<String> orderedCategories) throws Exception {

        List<News> allNews = newsCollection.getNewsCollection();
        List<News> categorizedNewsList = new ArrayList<>();

        // Formatte la liste des catégories pour le prompt
        String categoryListPrompt = String.join(", ", orderedCategories);
        System.out.println(categoryListPrompt);

        String promptTemplate = """
          Tu es un système d'évaluation d'actualités.
          TA TÂCHE: Évaluer les articles suivants et retourner une map JSON.

          RÈGLES:
          1.  Scores: 0 (pas lié) à 4 (très lié).
          2.  Ordre des catégories: Tu DOIS respecter cet ordre EXACT de %d
              thèmes: %s
          3.  Format: Réponds UNIQUEMENT avec la map JSON. Pas de texte, pas
              d'explication.

          ARTICLES À ÉVALUER (Format "ID": "Titre. Description"):
          %s

          FORMAT DE RÉPONSE JSON ATTENDU (Map<String, List<Integer>> avec %d
                                          scores):
          {
            "1": [0, 0, 4, 1, 0, 0, 0, 0, 0, 0, 1, 0],
            "2": [1, 4, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0],
            "3": [0, 0, 0, 0, 4, 1, 0, 0, 0, 0, 0, 0]
          }
          """;

        // Boucle par lots
        for (int i = 0; i < allNews.size(); i += LLM_BATCH_SIZE) {

            List<News> batch =
                    allNews.subList(i, Math.min(i + LLM_BATCH_SIZE,
                            allNews.size()));

            Map<String, News> batchMap = new HashMap<>();
            StringBuilder articlesPrompt = new StringBuilder();
            StringBuilder articlesLog = new StringBuilder();

            for (int j = 0; j < batch.size(); j++) {
                News news = batch.get(j);
                String batchId = String.valueOf(j + 1);
                batchMap.put(batchId, news);

                articlesPrompt.append(String.format("\"%s\": \"%s. %s\"%n",
                        batchId,
                        news.getTitle().replace("\"", "'"),
                        news.getDescription()
                                .replace("\"", "'")
                                .replace("\n", " ")
                                .trim()
                ));

                articlesLog.append(String.format("  Article %s: %s%n",
                        batchId,
                        news.getTitle()
                ));
            }

            String finalPrompt = String.format(
                    promptTemplate,
                    orderedCategories.size(),
                    categoryListPrompt,
                    articlesPrompt.toString(),
                    orderedCategories.size()
            );

            try {
                String llmResponse = LLM.generate(finalPrompt);

                LOGGER.info(String.format(
                        "--- Articles envoyés au LLM (Lot %d) ---%n%s"
                                + "--- Réponse brute du LLM ---%n%s",
                        (i / LLM_BATCH_SIZE + 1),
                        articlesLog.toString(),
                        llmResponse
                ));

                int startIndex = llmResponse.indexOf('{');
                int endIndex = llmResponse.lastIndexOf('}');
                String cleanedResponse;
                if (startIndex != -1 && endIndex != -1
                        && endIndex > startIndex) {
                    cleanedResponse = llmResponse.substring(
                            startIndex, endIndex + 1);
                } else {
                    cleanedResponse = llmResponse;
                }

                Map<String, List<Integer>> scoresMap =
                        MAPPER.readValue(cleanedResponse,
                                MAP_STRING_LIST_INT_TYPE);

                for (Map.Entry<String, List<Integer>> entry
                        : scoresMap.entrySet()) {
                    String batchId = entry.getKey();
                    List<Integer> scores = entry.getValue();
                    News newsItem = batchMap.get(batchId);

                    if (newsItem == null) {
                        LOGGER.warning(() ->
                                "Le LLM a retourné un ID de lot inconnu: "
                                        + batchId);
                        continue;
                    }

                    if (scores.size() != orderedCategories.size()) {
                        LOGGER.warning(() ->
                                "Le LLM a retourné un nombre de scores "
                                        + "incorrect pour "
                                        + batchId);
                        categorizedNewsList.add(newsItem);
                        continue;
                    }

                    List<NewsCategoryScore> categoryScores =
                            new ArrayList<>();
                    for (int catIndex = 0;
                         catIndex < orderedCategories.size();
                         catIndex++) {

                        int rawScore = scores.get(catIndex);
                        int score = Math.max(0,
                                Math.min(rawScore, MAX_LLM_SCORE));

                        if (score > 0) {
                            String categoryName =
                                    orderedCategories.get(catIndex);
                            categoryScores.add(
                                    new NewsCategoryScore(
                                            categoryName, score));
                        }
                    }
                    newsItem.setCategoryScores(categoryScores);
                    categorizedNewsList.add(newsItem);
                }

            } catch (Exception ex) {
                LOGGER.warning(() ->
                        "Catégorisation par lot ratée: "
                                + ex.getMessage());
                categorizedNewsList.addAll(batch);
            }
        }
        return new NewsCollection(categorizedNewsList);
    }

    /**
     * Trie les news déjà catégorisées conformément aux préférences
     * de l'utilisateur.
     *
     * @param newsCollection collection de news triée
     * @param userPreferences préférences de l'utilisateur
     * @return liste de news triée de façon décroissante
     */
    private static List<News> sortNewsByPreference(
            final NewsCollection newsCollection,
            final Map<String, Integer> userPreferences) {

        List<News> allNews = newsCollection.getNewsCollection();

        allNews.sort((news1, news2) -> {
            int score1 = calculateMatchScore(news1, userPreferences);
            int score2 = calculateMatchScore(news2, userPreferences);
            return Integer.compare(score2, score1);
        });

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
