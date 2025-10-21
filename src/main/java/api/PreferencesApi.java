package api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import model.News;
import model.NewsCategoryScoreCollection;
import model.NewsCollection;
import org.eclipse.jetty.http.HttpMethod;
import rss.LeMondeRSSFetcher;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import java.time.Duration;

import com.fasterxml.jackson.core.type.TypeReference;


public class PreferencesApi {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) // garder ISO-8601
            .registerModule(new JavaTimeModule());                   // <-- support de Instant/LocalDate*

    private static final ChatLanguageModel llm = OllamaChatModel.builder()
            .baseUrl("http://localhost:11434")
            .modelName("mistral")
            .timeout(Duration.ofMinutes(5))
            .build();

    public static void main(String[] args) {
        int port = 8080;
        Javalin app = Javalin.create(cfg -> {
            cfg.http.defaultContentType = "application/json";
        }).start(port);

        // CORS: pré-vol (OPTIONS) -> 204 + bons headers
        app.options("/api/preferences", ctx -> {
            setCors(ctx);
            ctx.status(204);
        });

        // POST réel
        app.post("/api/preferences", ctx -> {
            setCors(ctx);
            handlePreferences(ctx);
        });

        // (optionnel) health
        app.get("/health", ctx -> ctx.result("ok"));

        System.out.println("API sur http://localhost:" + port + "/api/preferences");
    }

    private static void setCors(Context ctx) {
        // Tu peux restreindre ici: "http://localhost:63342"
        ctx.header("Access-Control-Allow-Origin", "*");
        ctx.header("Access-Control-Allow-Methods", "POST, OPTIONS");
        ctx.header("Access-Control-Allow-Headers", "Content-Type");
        ctx.header("Vary", "Origin");
    }

    private static NewsCollection initNewsCollection() {
        NewsCollection newsCollection = new NewsCollection();
        List<Map<String, String>> rawNews = LeMondeRSSFetcher.fetchRawNews();

        for (Map<String, String> newsData : rawNews) {
            // Créer un objet News à partir des données brutes
            String title = newsData.get("title");
            String link = newsData.get("link");
            String description = newsData.get("description");

            News news = new News(title, link, description);

            // Ajouter la news dans la collection
            newsCollection.add(news);
        }

        return newsCollection;
    }

    private static void handlePreferences(Context ctx) {
        try {
            NewsCollection newsCollection = initNewsCollection();

            NewsCategoryScoreCollection preferences = MAPPER.readValue(ctx.body(), NewsCategoryScoreCollection.class);
            List<News> sortedNews = sortNewsWithLLM(newsCollection, preferences);

            // TODO: brancher ici votre pipeline RSS + scoring IA
            ctx.status(HttpStatus.OK)
                    .json(newsCollection);

        } catch (JsonProcessingException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                    .json(new Error("invalid_json", e.getOriginalMessage()));
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(new Error("server_error", e.getMessage()));
        }
    }

    private static List<News> sortNewsWithLLM(NewsCollection newsCollection, NewsCategoryScoreCollection userPreferences) {
        try {
            String newsJson = MAPPER.writeValueAsString(newsCollection.getNewsCollection());
            String preferencesJson = MAPPER.writeValueAsString(userPreferences);

            String prompt = """
        Tu es un système intelligent qui trie des actualités pour un utilisateur.
        Tu reçois des préférences utilisateur (1=déteste, 5=adore) et une liste de news.

        TA TÂCHE :
        1. Pour chaque news, analyse son titre et sa description pour déterminer ses thèmes principaux (ex: "politique", "sport", "sciences", etc.).
        2. Crée un champ "categoryScores". Ce champ doit être une LISTE d'objets.
        3. Pour chaque thème identifié, ajoute un objet à la liste avec un "score" ENTIER de 1 (pas pertinent) à 4 (très pertinent).
        4. Filtre les news dont le thème principal correspond à une préférence utilisateur de 1.
        5. Trie les news restantes par pertinence décroissante, en favorisant les thèmes que l'utilisateur adore (préférence 4-5).
        
        Retourne UNIQUEMENT le JSON trié. N'ajoute aucun commentaire.

        PRÉFÉRENCES UTILISATEUR :
        %s

        NEWS (JSON en entrée) :
        %s

        FORMAT DE RÉPONSE ATTENDU (JSON uniquement, attention à "categoryScores") :
        [
          {
            "title": "...",
            "link": "...",
            "description": "...",
            "categoryScores": [
              {"category": "sport", "score": 4},
              {"category": "politique", "score": 1}
            ]
          },
          ...
        ]
        """.formatted(preferencesJson, newsJson);

            // Appel du modèle via LangChain4j
            String llmResponse = llm.generate(prompt);

            // Conversion JSON → List<News>
            // Ceci fonctionnera car nos POJOs (News, NewsCategoryScore)
            // correspondent maintenant au format JSON demandé.
            return MAPPER.readValue(llmResponse, new TypeReference<List<News>>() {});

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erreur tri LLM: " + e.getMessage());
            return newsCollection.getNewsCollection(); // en fallback on renvoie tout
        }
    }




    public static class Error {
        public String code; public String message;
        public Error(String code, String message){ this.code = code; this.message = message; }
    }
}
