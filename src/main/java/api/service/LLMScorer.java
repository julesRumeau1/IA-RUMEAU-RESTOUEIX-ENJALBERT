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

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class LLMScorer {

    private LLMScorer () {
        // utilitaire
    }
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

    /** Url vers le conteneur ollama. */
    private static final String OLLAMAURL = System.getenv().getOrDefault("OLLAMA_HOST", "http://localhost:11434");

    /** Mapper JSON configuré. */
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                    false)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .registerModule(new JavaTimeModule());


    /** Type générique pour la réponse du LLM. */
    private static final TypeReference<Map<String, List<Integer>>>
            MAP_STRING_LIST_INT_TYPE = new TypeReference<>() { };

    public static NewsCollection categorize(
            NewsCollection newsCollection,
            List<String> orderedCategories,
            ChatLanguageModel llm
    ) throws ApiException {

        if (newsCollection == null || newsCollection.getNewsCollection() == null) {
            throw new ApiException("empty_news_collection", "La collection de news est vide ou nulle");
        }
        if (orderedCategories == null || orderedCategories.isEmpty()) {
            throw new ApiException("empty_categories", "La liste de catégories est vide ou nulle");
        }
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
                if (news == null || news.getTitle() == null || news.getDescription() == null) {
                    throw new ApiException("invalid_news_item", "Un article du lot est invalide");
                }
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
                String llmResponse = llm.generate(finalPrompt);

                LOGGER.info(String.format(
                        "--- Articles envoyés au LLM (Lot %d) ---%n%s"
                                + "--- Réponse brute du LLM ---%n%s",
                        (i / LLM_BATCH_SIZE + 1),
                        articlesLog.toString(),
                        llmResponse
                ));

                int startIndex = llmResponse.indexOf('{');
                int endIndex = llmResponse.lastIndexOf('}');
                if (startIndex == -1 || endIndex == -1 || endIndex <= startIndex) {
                    throw new ApiException("invalid_llm_response", "Le LLM a retourné une réponse JSON invalide");
                }

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

            } catch (IOException | RuntimeException ex) {
                throw new ApiException("llm_batch_failed", "Échec de catégorisation par le LLM : " + ex.getMessage());
            }
        }
        return new NewsCollection(categorizedNewsList);
    }
}
