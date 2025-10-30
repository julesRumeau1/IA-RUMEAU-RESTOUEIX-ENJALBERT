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
import java.util.stream.Collectors;
import model.News;
import model.NewsCategoryScore;
import model.NewsCollection;
import rss.LeMondeRSSFetcher;
import api.dto.ErrorResponse;
import api.dto.PreferencesRequest;
import api.util.CorsUtil;
import api.util.PreferencesUtils;

/**
 * API permettant de faire le lien entre le front et la back, puis de gérer les préférences et le scoring
 */
public final class PreferencesApi {

    private static final Logger LOGGER = Logger.getLogger(PreferencesApi.class.getName());
    private static final int LLM_BATCH_SIZE = 3;

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .registerModule(new JavaTimeModule());

    private static final ChatLanguageModel LLM = OllamaChatModel.builder()
            .baseUrl("http://localhost:11434")
            .modelName("qwen2.5:3b")
            .timeout(Duration.ofMinutes(5))
            .build();

    private static final Map<Integer, Integer> PREFERENCE_WEIGHTS = Map.of(
            1, -5,
            2, -1,
            3, 1,
            4, 3,
            5, 5
    );

    private static final TypeReference<Map<String, List<Integer>>> MAP_STRING_LIST_INT_TYPE =
            new TypeReference<>() {};

    /**
     * Démarre le serveur API (est appelé depuis la classe Main à la racine)
     * @param args arguments
     */
    public static void main(String[] args) {
        final int port = 8080;
        Javalin app = Javalin.create(cfg -> {
                    cfg.http.defaultContentType = "application/json";
                    cfg.plugins.enableDevLogging();
                })

                .start(port);

        app.options("/api/preferences", ctx -> {
            CorsUtil.setCors(ctx);
            ctx.status(204);
        });

        app.post("/api/preferences", ctx -> {
            CorsUtil.setCors(ctx);
            handlePreferences(ctx);
        });

        app.get("/health", ctx -> ctx.result("ok"));
        LOGGER.info(() -> "API: http://localhost:" + port + "/api/preferences");
    }

    /**
     * Permet de définir la structure d'une collection de news (titre, lien, ...)
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
     * Permet de gérer les préférences de l'utilisateur et de les envoyer au modèle
     * @param ctx Contexte de la requête HTTP envoyé depuis le front
     */
    private static void handlePreferences(Context ctx) {
        try {
            NewsCollection newsCollection = initNewsCollection();
            PreferencesRequest req = MAPPER.readValue(ctx.body(), PreferencesRequest.class);
            Map<String, Integer> userPreferences = PreferencesUtils.flattenPreferences(req.getThemes());

            // La liste des catégories
            List<String> orderedCategories = new ArrayList<>(userPreferences.keySet());

            NewsCollection categorizedNews = categorizeNewsWithLLM(newsCollection, orderedCategories);

            if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
                try {
                    LOGGER.fine("--- JSON après catégorisation ---\n"
                            + MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(categorizedNews));
                } catch (JsonProcessingException ignored) {
                    // pas d'erreur loggé
                }
            }

            List<News> sortedNews = sortNewsByPreference(categorizedNews, userPreferences);

            if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
                try {
                    LOGGER.fine("--- JSON après tri ---\n"
                            + MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(sortedNews));
                } catch (JsonProcessingException ignored) {
                    // pas d'erreur loggé
                }
            }

            ctx.status(HttpStatus.OK).json(new NewsCollection(sortedNews));

        } catch (JsonProcessingException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                    .json(new ErrorResponse("invalid_json", e.getOriginalMessage()));
        } catch (Exception e) {
            LOGGER.severe(e::getMessage);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(new ErrorResponse("server_error", e.getMessage()));
        }
    }

    /**
     * Catégorise chaque nouvelles news en utilisant un modèle et une liste de catégorie
     * @param newsCollection collection de news
     * @param orderedCategories catégories de news (l'ordre est important)
     * @return la collection de news catégorisé
     * @throws Exception Si problème lors de la catégorisation
     */
    private static NewsCollection categorizeNewsWithLLM(
            NewsCollection newsCollection,
            List<String> orderedCategories) throws Exception {

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
          2.  Ordre des catégories: Tu DOIS respecter cet ordre EXACT de %d thèmes:
              %s
          3.  Format: Réponds UNIQUEMENT avec la map JSON. Pas de texte, pas d'explication.
          
          ARTICLES À ÉVALUER (Format "ID": "Titre. Description"):
          %s
          
          FORMAT DE RÉPONSE JSON ATTENDU (Map<String, List<Integer>> avec %d scores):
          {
            "1": [0, 0, 4, 1, 0, 0, 0, 0, 0, 0, 1, 0],
            "2": [1, 4, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0],
            "3": [0, 0, 0, 0, 4, 1, 0, 0, 0, 0, 0, 0]
          }
          """;

        // Boucle par lots
        for (int i = 0; i < allNews.size(); i += LLM_BATCH_SIZE) {

            List<News> batch = allNews.subList(i, Math.min(i + LLM_BATCH_SIZE, allNews.size()));

            Map<String, News> batchMap = new HashMap<>();
            StringBuilder articlesPrompt = new StringBuilder();

            StringBuilder articlesLog = new StringBuilder();

            for (int j = 0; j < batch.size(); j++) {
                News news = batch.get(j);
                String batchId = String.valueOf(j + 1); // "1", "2", ou "3"
                batchMap.put(batchId, news);

                articlesPrompt.append(String.format(
                        "\"%s\": \"%s. %s\"\n", // Format: "ID": "Titre. Description"
                        batchId,
                        news.getTitle().replace("\"", "'"),
                        news.getDescription().replace("\"", "'").replace("\n", " ").trim()
                ));

                articlesLog.append(String.format("  Article %s: %s\n", batchId, news.getTitle()));
            }

            String finalPrompt = String.format(
                    promptTemplate,
                    orderedCategories.size(),
                    categoryListPrompt,
                    articlesPrompt.toString(),
                    orderedCategories.size()
            );

            // Appeler le LLM et parser la réponse
            try {
                String llmResponse = LLM.generate(finalPrompt);

                // Log le scoring et le nom de l'article
                LOGGER.info(String.format(
                        "--- Articles envoyés au LLM (Lot %d) ---\n%s" +
                                "--- Réponse brute du LLM ---\n%s",
                        (i / LLM_BATCH_SIZE + 1), // Calcule le N° du lot
                        articlesLog.toString(),    // Titres des articles
                        llmResponse                // Scoring du llm
                ));

                int startIndex = llmResponse.indexOf('{');
                int endIndex = llmResponse.lastIndexOf('}');
                String cleanedResponse;
                if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                    cleanedResponse = llmResponse.substring(startIndex, endIndex + 1);
                } else {
                    cleanedResponse = llmResponse;
                }

                Map<String, List<Integer>> scoresMap = MAPPER.readValue(cleanedResponse, MAP_STRING_LIST_INT_TYPE);

                // Appliquer les scores aux objets News
                for (Map.Entry<String, List<Integer>> entry : scoresMap.entrySet()) {
                    String batchId = entry.getKey();
                    List<Integer> scores = entry.getValue();
                    News newsItem = batchMap.get(batchId);

                    if (newsItem == null) {
                        LOGGER.warning(() -> "Le LLM a retourné un ID de lot inconnu: " + batchId);
                        continue;
                    }

                    if(scores.size() != orderedCategories.size()) {
                        LOGGER.warning(() -> "Le LLM a retourné un nombre de scores incorrect pour " + batchId);
                        categorizedNewsList.add(newsItem);
                        continue;
                    }

                    List<NewsCategoryScore> categoryScores = new ArrayList<>();
                    for (int catIndex = 0; catIndex < orderedCategories.size(); catIndex++) {

                        int rawScore = scores.get(catIndex);
                        // On force la valeur à être entre 0 et 4, quoi que le LLM ait répondu
                        int score = Math.max(0, Math.min(rawScore, 4));

                        // On n'ajoute que les scores > 0 (car 0 = pas lié)
                        if (score > 0) {
                            String categoryName = orderedCategories.get(catIndex);
                            categoryScores.add(new NewsCategoryScore(categoryName, score));
                        }
                    }
                    newsItem.setCategoryScores(categoryScores);
                    categorizedNewsList.add(newsItem);
                }

            } catch (Exception ex) {
                LOGGER.warning(() -> "Catégorisation par lot ratée: " + ex.getMessage());
                categorizedNewsList.addAll(batch);
            }
        }
        return new NewsCollection(categorizedNewsList);
    }

    /**
     * Tri les news déjà catégorisé conformément aux préférences de l'utilisateur.
     * @param newsCollection Collection de news trié
     * @param userPreferences Préférence de l'utilisateur selon les thèmes sur une échele de 1 à 5
     * @return Liste de news trié de façon décroissante
     */
    private static List<News> sortNewsByPreference(
            NewsCollection newsCollection,
            Map<String, Integer> userPreferences) {

        // Récupérer la liste de toutes les news
        List<News> allNews = newsCollection.getNewsCollection();

        // Trier la liste directement.
        // On fournit une règle de comparaison (un "Comparator") pour dire à Java
        // comment classer deux articles (news1 et news2).
        allNews.sort((news1, news2) -> {

            // Calcul le score pour le premier article
            int score1 = calculateMatchScore(news1, userPreferences);

            // Calcul le score pour le deuxième article
            int score2 = calculateMatchScore(news2, userPreferences);

            // Comparer les deux scores.
            // On met score2 avant score1 pour avoir un tri décroissant (score plus élevé en haut)
            return Integer.compare(score2, score1);
        });

        LOGGER.info("--- Tri final des articles par score ---");
        StringBuilder sortedLog = new StringBuilder();
        for (News news : allNews) {
            int score = calculateMatchScore(news, userPreferences);

            sortedLog.append(String.format(
                    "  Score: %-5d | Titre: %s\n", // %-5d aligne le score sur 5 caractères
                    score,
                    news.getTitle()
            ));
        }
        LOGGER.info(sortedLog.toString());

        // Renvoie la liste triée
        return allNews;
    }

    /**
     * Calcul le score pour une news en fonction des préférences utilisateurs
     * @param news la news pour laquelle on calcul le "match score"
     * @param userPreferences les préférences de l'utilisateur sur les thèmes
     * @return le "match score" pour la news voulues
     */
    private static int calculateMatchScore(News news, Map<String, Integer> userPreferences) {
        if (news.getCategoryScores() == null || news.getCategoryScores().isEmpty()) {
            return -1000;
        }
        int totalScore = 0;
        boolean matched = false;

        for (NewsCategoryScore entry : news.getCategoryScores()) {
            String category = entry.getCategory();
            int relevance = entry.getScore(); // Score 1-4 du LLM
            if (userPreferences.containsKey(category)) {
                matched = true;
                int prefLevel = userPreferences.get(category); // Préférence utilisateur 1-5
                int prefWeight = PREFERENCE_WEIGHTS.getOrDefault(prefLevel, 0);

                totalScore += prefWeight * relevance;
            }
        }
        if (!matched) {
            return -500;
        }
        return totalScore;
    }
}