package api;

import com.fasterxml.jackson.core.JsonProcessingException;
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

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .registerModule(new JavaTimeModule());

    private static final ChatLanguageModel LLM = OllamaChatModel.builder()
            .baseUrl("http://localhost:11434")
            .modelName("phi4-mini")
            .timeout(Duration.ofMinutes(5))
            .build();

    private static final Map<Integer, Integer> PREFERENCE_WEIGHTS = Map.of(
            1, -5,
            2, -1,
            3, 1,
            4, 3,
            5, 5
    );

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

            List<String> allowedCategories = new ArrayList<>(userPreferences.keySet());
            NewsCollection categorizedNews = categorizeNewsWithLLM(newsCollection, allowedCategories);

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
     * @param allowedCategories catégories de news
     * @return la collection de news catégorisé
     * @throws Exception Si problème lors de la catégorisation
     */
    private static NewsCollection categorizeNewsWithLLM(
            NewsCollection newsCollection,
            List<String> allowedCategories) throws Exception {

        List<News> categorizedNewsList = new ArrayList<>();
        String categoriesJson = MAPPER.writeValueAsString(allowedCategories);

        for (News newsItem : newsCollection.getNewsCollection()) {
            String newsJson = MAPPER.writeValueAsString(newsItem);

            String prompt = """
          Tu es un système expert en catégorisation d'actualités.
          TA TÂCHE : Analyser l'article (titre et description) et ajouter le champ "categoryScores".

          CONTRAINTES :
          1) Utiliser UNIQUEMENT ces catégories : %s
          2) Au maximum 2 catégories par article.
          3) Score ENTIER 1..4 par catégorie retenue.
          4) Si aucune catégorie pertinente : "categoryScores": [].

          Retourne UNIQUEMENT l'objet JSON de l'article mis à jour.

          CATEGORIES AUTORISÉES :
          %s

          ARTICLE (JSON) :
          %s
          """.formatted(categoriesJson, categoriesJson, newsJson);

            try {
                String llmResponse = LLM.generate(prompt);
                int startIndex = llmResponse.indexOf('{');
                int endIndex = llmResponse.lastIndexOf('}');
                String cleanedResponse;
                if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                    cleanedResponse = llmResponse.substring(startIndex, endIndex + 1);
                } else {
                    cleanedResponse = llmResponse;
                }
                News processedNews = MAPPER.readValue(cleanedResponse, News.class);
                categorizedNewsList.add(processedNews);
            } catch (Exception ex) {
                LOGGER.warning(() -> "Catégorisation raté pour: " + newsItem.getTitle() + " -> " + ex.getMessage());
                categorizedNewsList.add(newsItem);
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

        List<Map.Entry<News, Integer>> scoredNews = newsCollection.getNewsCollection().stream()
                .map(news -> Map.entry(news, calculateMatchScore(news, userPreferences)))
                .collect(Collectors.toList());

        return scoredNews.stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
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
            int relevance = entry.getScore();
            if (userPreferences.containsKey(category)) {
                matched = true;
                int prefLevel = userPreferences.get(category);
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
