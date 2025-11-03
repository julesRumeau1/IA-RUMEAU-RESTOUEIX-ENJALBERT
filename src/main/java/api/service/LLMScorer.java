package api.service;

import api.PreferencesApi;
import api.util.ApiException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.langchain4j.model.chat.ChatLanguageModel;
import model.News;
import model.NewsCategoryScore;
import model.NewsCollection;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Service utilitaire de catégorisation d'articles via
 * un modèle de langage (LLM).
 * Fournit des fonctions statiques pour évaluer la pertinence
 * d'articles selon des catégories.
 */
public final class LLMScorer {

    /** Constructeur privé pour empêcher l'instanciation. */
    private LLMScorer() {
        // utilitaire
    }

    /** Logger de la classe. */
    private static final Logger LOGGER =
            Logger.getLogger(PreferencesApi.class.getName());

    /** Nombre d'articles envoyés au LLM par lot. */
    private static final int LLM_BATCH_SIZE = 3;

    /** Score maximum retourné par le LLM. */
    private static final int MAX_LLM_SCORE = 4;

    /** URL vers le conteneur Ollama. */
    private static final String OLLAMA_URL =
            System.getenv()
                    .getOrDefault("OLLAMA_HOST", "http://localhost:11434");

    /** Mapper JSON configuré. */
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .registerModule(new JavaTimeModule());

    /** Type générique pour la réponse du LLM. */
    private static final TypeReference<Map<String, List<Integer>>>
            MAP_STRING_LIST_INT_TYPE =
            new TypeReference<>() { };


    /**
     * Catégorise une collection de news selon des catégories ordonnées
     * et un modèle de langage.
     *
     * @param newsCollection   la collection de news à catégoriser
     * @param orderedCategories liste des catégories dans l'ordre à respecter
     * @param llm              modèle de langage pour scorer les articles
     * @return une nouvelle NewsCollection avec les scores de catégorie
     * @throws ApiException si la collection est vide ou si le LLM échoue
     */
    public static NewsCollection categorize(
            final NewsCollection newsCollection,
            final List<String> orderedCategories,
            final ChatLanguageModel llm
    ) throws ApiException {

        validateInputs(newsCollection, orderedCategories);

        // === AJOUT DU LOG DES CATÉGORIES ORDONNÉES ===
        LOGGER.info("--- Catégories ordonnées pour le LLM ---");
        LOGGER.info(String.join(", ", orderedCategories));
        // ============================================

        List<News> allNews = newsCollection.getNewsCollection();
        List<News> categorizedNewsList = new ArrayList<>();

        for (int i = 0; i < allNews.size(); i += LLM_BATCH_SIZE) {
            List<News> batch = createBatch(allNews, i);
            Map<String, News> batchMap = mapBatchById(batch);
            String articlesPrompt = buildArticlesPrompt(batch);
            String articlesLog = buildArticlesLog(batch);

            String finalPrompt =
                    buildFinalPrompt(orderedCategories, articlesPrompt);

            Map<String, List<Integer>> scoresMap =
                    callLlmAndParse(finalPrompt, llm, articlesLog);

            assignScoresToBatch(scoresMap, batchMap, orderedCategories,
                    categorizedNewsList);
        }

        return new NewsCollection(categorizedNewsList);
    }

    /**
     * Vérifie que la collection de news et la liste de catégories
     * sont valides.
     *
     * @param newsCollection   la collection à vérifier
     * @param orderedCategories les catégories à vérifier
     * @throws ApiException si les entrées sont nulles ou vides
     */
    private static void validateInputs(
            final NewsCollection newsCollection,
            final List<String> orderedCategories
    ) throws ApiException {
        if (newsCollection == null
                || newsCollection.getNewsCollection() == null) {
            throw new ApiException("empty_news_collection",
                    "La collection de news est vide ou nulle");
        }
        if (orderedCategories == null || orderedCategories.isEmpty()) {
            throw new ApiException("empty_categories",
                    "La liste de catégories est vide ou nulle");
        }
    }

    /**
     * Extrait un sous-lot d'articles pour un batch.
     *
     * @param allNews    la liste complète d'articles
     * @param startIndex index de départ du batch
     * @return la liste des articles du batch
     */
    private static List<News> createBatch(
            final List<News> allNews,
            final int startIndex
    ) {
        return allNews.subList(startIndex,
                Math.min(startIndex + LLM_BATCH_SIZE, allNews.size()));
    }

    /**
     * Mappe les articles d'un batch avec des IDs sous forme de chaîne.
     *
     * @param batch le batch d'articles
     * @return une map ID -> News
     */
    private static Map<String, News> mapBatchById(final List<News> batch) {
        Map<String, News> batchMap = new HashMap<>();
        for (int i = 0; i < batch.size(); i++) {
            batchMap.put(String.valueOf(i + 1), batch.get(i));
        }
        return batchMap;
    }

    /**
     * Construit le prompt texte pour le LLM à partir d'un batch.
     *
     * @param batch le batch d'articles
     * @return la chaîne représentant les articles pour le prompt
     */
    private static String buildArticlesPrompt(final List<News> batch) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < batch.size(); i++) {
            News news = batch.get(i);
            sb.append(String.format("\"%d\": \"%s. %s\"%n",
                    i + 1,
                    news.getTitle().replace("\"", "'"),
                    news.getDescription().replace("\"", "'")
                            .replace("\n", " ").trim()));
        }
        return sb.toString();
    }

    /**
     * Construit le log texte des articles pour débogage.
     *
     * @param batch le batch d'articles
     * @return le texte de log
     */
    private static String buildArticlesLog(final List<News> batch) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < batch.size(); i++) {
            sb.append(String.format("  Article %d: %s%n",
                    i + 1, batch.get(i).getTitle()));
        }
        return sb.toString();
    }

    /**
     * Construit le prompt final à envoyer au LLM.
     *
     * @param orderedCategories liste des catégories
     * @param articlesPrompt    prompt des articles
     * @return le prompt complet
     */
    private static String buildFinalPrompt(
            final List<String> orderedCategories,
            final String articlesPrompt
    ) {
        String categoryListPrompt = String.join(", ", orderedCategories);

        String promptTemplate = """
          Tu es un système d'évaluation d'actualités.
          TA TÂCHE: Évaluer les articles suivants et retourner une map JSON.

          RÈGLES:
          1.  Scores: 0 (pas lié), 1 (un peu lié), 2 (lié),
           3 (bien lié), 4 (très lié).
          2.  Ordre des catégories: Tu DOIS respecter cet ordre EXACT de %d
              thèmes: %s
          3.  Format: Réponds UNIQUEMENT avec la map JSON. Pas de texte, pas
              d'explication.

          ARTICLES À ÉVALUER (Format "ID": "Titre. Description"):
          %s

          FORMAT DE RÉPONSE JSON ATTENDU (Map<String, List<Integer>> avec %d
                                          scores):
          {
            "1": [x, x, x, x, x, x, x, x, x, x, x, x],
            "2": [x, xx, x, x, x, x, x, x, x, x, x, x],
            "3": [x, x, x, x, x, x, x, x, x, x, x, x],
          }
          """;

        return String.format(promptTemplate, orderedCategories.size(),
                categoryListPrompt, articlesPrompt,
                orderedCategories.size());
    }

    /**
     * Appelle le LLM et parse la réponse en map ID -> scores.
     *
     * @param prompt      le prompt à envoyer
     * @param llm         le modèle de langage
     * @param articlesLog texte de log des articles
     * @return map ID -> scores
     * @throws ApiException si la réponse du LLM est invalide
     */
    private static Map<String, List<Integer>> callLlmAndParse(
            final String prompt,
            final ChatLanguageModel llm,
            final String articlesLog
    ) throws ApiException {
        try {
            // === AJOUT DU LOG ICI ===
            // Log des articles envoyés au LLM pour ce lot
            LOGGER.info("--- Envoi du lot au LLM ---");
            LOGGER.info(articlesLog);
            // =========================

            String response = llm.generate(prompt);

            // J'ajoute aussi un titre ici pour mieux lire la réponse
            LOGGER.info("--- Réponse LLM reçue ---");
            LOGGER.info(response);

            int startIndex = response.indexOf('{');
            int endIndex = response.lastIndexOf('}');
            if (startIndex == -1 || endIndex == -1 || endIndex <= startIndex) {
                throw new ApiException("invalid_llm_response",
                        "Le LLM a retourné une réponse JSON invalide");
            }

            String cleanedResponse =
                    response.substring(startIndex, endIndex + 1);
            return MAPPER.readValue(cleanedResponse, MAP_STRING_LIST_INT_TYPE);

        } catch (Exception ex) {
            // Le log des articles est déjà passé dans le `categorize`
            // Il serait bon de l'ajouter ici aussi en cas d'erreur
            LOGGER.warning("Échec de catégorisation. Articles concernés :\n"
                    + articlesLog);
            throw new ApiException("llm_batch_failed",
                    "Échec de catégorisation par le LLM : " + ex.getMessage());
        }
    }

    /**
     * Assigne les scores de catégorie à chaque article du batch.
     *
     * @param scoresMap           map ID -> scores
     * @param batchMap            map ID -> News
     * @param orderedCategories   liste des catégories
     * @param categorizedNewsList liste où ajouter les articles catégorisés
     */
    private static void assignScoresToBatch(
            final Map<String, List<Integer>> scoresMap,
            final Map<String, News> batchMap,
            final List<String> orderedCategories,
            final List<News> categorizedNewsList
    ) {
        for (Map.Entry<String, List<Integer>> entry : scoresMap.entrySet()) {
            String id = entry.getKey();
            List<Integer> scores = entry.getValue();
            News news = batchMap.get(id);
            if (news == null) {
                continue;
            }

            List<NewsCategoryScore> categoryScores = new ArrayList<>();
            for (int i = 0; i < orderedCategories.size(); i++) {
                int score =
                        Math.max(0, Math.min(scores.get(i), MAX_LLM_SCORE));
                if (score > 0) {
                    categoryScores.add(new NewsCategoryScore(
                            orderedCategories.get(i), score));
                }
            }
            news.setCategoryScores(categoryScores);
            categorizedNewsList.add(news);
        }
    }
}
