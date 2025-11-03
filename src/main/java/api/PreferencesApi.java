package api;

import api.dto.ErrorResponse;
import api.dto.PreferencesRequest;
import api.util.ApiException;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import main.Main;
import model.News;
import model.NewsCollection;
import api.service.NewsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.core.JsonProcessingException;
import rss.LeMondeRSSFetcher;
import rss.RssFetcher;

import java.time.Duration;
import java.util.List;
import java.util.logging.Logger;

/**
 * Classe utilitaire pour gérer les endpoints de l'API
 * liés aux préférences utilisateur.
 */
public final class PreferencesApi {

    /**
     * Mapper JSON configuré.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .registerModule(new JavaTimeModule());

    /**
     * Logger.
     */
    private static final Logger LOGGER =
            Logger.getLogger(Main.class.getName());

    /**
     * Initialisation du fetcher.
     */
    private static final RssFetcher RSS_FETCHER = LeMondeRSSFetcher.INSTANCE;

    /**
     * time out du llm.
     */
    private static final int LLM_TIMEOUT_MINUTES = 5;

    /**
     * Initialisation du LLM.
     */
    private static final ChatLanguageModel LLM = OllamaChatModel.builder()
            .baseUrl(System.getenv().getOrDefault(
                    "OLLAMA_HOST",
                    "http://localhost:11434"
            ))
            .modelName("qwen2.5:7b")
            .timeout(Duration.ofMinutes(LLM_TIMEOUT_MINUTES))
            .build();

    /**
     * Service qui contient toute la logique métier.
     */
    private static final NewsService NEWS_SERVICE = new NewsService(
            RSS_FETCHER,
            LLM
    );

    private PreferencesApi() {
        // Classe utilitaire, pas d'instance
    }

    /**
     * Gère la requête POST /api/preferences.
     * Délègue la récupération, catégorisation et tri des news
     * au service métier.
     *
     * @param ctx contexte HTTP de Javalin
     */
    public static void handlePreferences(final Context ctx) {
        try {
            // Parse la requête JSON
            PreferencesRequest req = MAPPER.readValue(
                    ctx.body(),
                    PreferencesRequest.class
            );

            // Appel du service métier
            List<News> news = NEWS_SERVICE.getNewsForPreferences(req);

            // Retourne la réponse JSON
            ctx.status(HttpStatus.OK).json(new NewsCollection(news));

        } catch (JsonProcessingException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                    .json(new ErrorResponse(
                            "invalid_json",
                            e.getOriginalMessage()
                    ));

        } catch (ApiException e) {
            // Erreur métier personnalisée
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(new ErrorResponse(e.getCode(), e.getMessage()));

        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(new ErrorResponse(
                            "server_error",
                            "Erreur interne du serveur"
                    ));

            LOGGER.info(
                    e.toString()
            );
        }
    }
}
