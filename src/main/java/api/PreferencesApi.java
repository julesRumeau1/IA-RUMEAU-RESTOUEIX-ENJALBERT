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

/**
 * Mini API locale avec une seule route POST qui reçoit les préférences utilisateur.
 * Démarrage : exécuter PreferencesApi#main (http://localhost:8080).
 *
 * Gradle:
 *   implementation("io.javalin:javalin:5.6.2")
 *   implementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")
 *
 * Maven:
 *   <dependency>
 *     <groupId>io.javalin</groupId><artifactId>javalin</artifactId><version>5.6.2</version>
 *   </dependency>
 *   <dependency>
 *     <groupId>com.fasterxml.jackson.core</groupId><artifactId>jackson-databind</artifactId><version>2.17.1</version>
 *   </dependency>
 */
public class PreferencesApi {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) // garder ISO-8601
            .registerModule(new JavaTimeModule());                   // <-- support de Instant/LocalDate*

    // ↓↓↓ AJOUTEZ CECI ↓↓↓
    private static final ChatLanguageModel llm = OllamaChatModel.builder()
            .baseUrl("http://localhost:11434") // Assurez-vous que c'est la bonne URL
            .modelName("mistral")               // Le modèle que vous utilisez
            .timeout(Duration.ofMinutes(5))     // Laissez au LLM le temps de traiter les news
            .build();
    // ↑↑↑ FIN DE L'AJOUT ↑↑↑

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

            PreferencesRequest req = MAPPER.readValue(ctx.body(), PreferencesRequest.class);


            // Integer sciencesLevel = req.getThemes().getSciences().getLevel();

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

    private static List<News> sortNewsWithLLM(NewsCollection newsCollection, Map<String, Integer> userPreferences) {
        try {
            String newsJson = MAPPER.writeValueAsString(newsCollection.getNewsCollection());
            String preferencesJson = MAPPER.writeValueAsString(userPreferences);

            String prompt = """
        Tu es un système intelligent qui trie des actualités pour un utilisateur.
        Tu reçois des préférences utilisateur (0=déteste, 5=adore) et une liste de news.

        TA TÂCHE :
        1. Pour chaque news, analyse son titre et sa description pour déterminer ses thèmes principaux (ex: "politique", "sport", "sciences", etc.).
        2. Crée un champ "categoryScores". Ce champ doit être une LISTE d'objets.
        3. Pour chaque thème identifié, ajoute un objet à la liste avec un "score" ENTIER de 0 (pas pertinent) à 4 (très pertinent).
        4. Filtre les news dont le thème principal correspond à une préférence utilisateur de 0.
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


    // ==================== DTOs ====================

    public static class PreferencesRequest {
        private Instant ts;
        private Themes themes;

        public PreferencesRequest() {}

        public Instant getTs() { return ts; }
        public void setTs(Instant ts) { this.ts = ts; }
        public Themes getThemes() { return themes; }
        public void setThemes(Themes themes) { this.themes = themes; }
    }

    /**
     * Groupe typé des thèmes pour supporter l'accès obj.getThemes().getSciences().getLevel().
     * Chaque thème est une ThemeSelection (level + rss).
     */
    public static class Themes {
        @JsonProperty("politique")     private ThemeSelection politique;
        @JsonProperty("international") private ThemeSelection international;
        @JsonProperty("economie")      private ThemeSelection economie;
        @JsonProperty("societe")       private ThemeSelection societe;
        @JsonProperty("sport")         private ThemeSelection sport;
        @JsonProperty("culture")       private ThemeSelection culture;
        @JsonProperty("sciences")      private ThemeSelection sciences;
        @JsonProperty("planete")       private ThemeSelection planete;
        @JsonProperty("technologies")  private ThemeSelection technologies;
        @JsonProperty("sante")         private ThemeSelection sante;
        @JsonProperty("education")     private ThemeSelection education;
        @JsonProperty("idees")         private ThemeSelection idees;

        public Themes() {}

        public ThemeSelection getPolitique() { return politique; }
        public ThemeSelection getInternational() { return international; }
        public ThemeSelection getEconomie() { return economie; }
        public ThemeSelection getSociete() { return societe; }
        public ThemeSelection getSport() { return sport; }
        public ThemeSelection getCulture() { return culture; }
        public ThemeSelection getSciences() { return sciences; }
        public ThemeSelection getPlanete() { return planete; }
        public ThemeSelection getTechnologies() { return technologies; }
        public ThemeSelection getSante() { return sante; }
        public ThemeSelection getEducation() { return education; }
        public ThemeSelection getIdees() { return idees; }
    }

    public static class ThemeSelection {
        private Integer level; // 1..5
        private String rss;    // URL du flux

        public ThemeSelection() {}
        public ThemeSelection(Integer level, String rss) {
            this.level = level; this.rss = rss;
        }
        public Integer getLevel() { return level; }
        public void setLevel(Integer level) { this.level = level; }
        public String getRss() { return rss; }
        public void setRss(String rss) { this.rss = rss; }
    }

    public static class Ack {
        public String status; public Instant receivedAt;
        public Ack(String status, Instant receivedAt){ this.status = status; this.receivedAt = receivedAt; }
    }
    public static class Error {
        public String code; public String message;
        public Error(String code, String message){ this.code = code; this.message = message; }
    }
}
