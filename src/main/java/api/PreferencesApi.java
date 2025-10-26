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
import model.NewsCategoryScore;
import model.NewsCollection;
import rss.LeMondeRSSFetcher;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import java.time.Duration;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;


public class PreferencesApi {


    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .registerModule(new JavaTimeModule());

    private static final ChatLanguageModel llm = OllamaChatModel.builder()
            .baseUrl("http://localhost:11434")
            .modelName("mistral")
            .timeout(Duration.ofMinutes(5))
            .build();

    private static final Map<Integer, Integer> PREFERENCE_WEIGHTS = Map.of(
            1, -5,  // Forte pénalité
            2, -1,  // Légère pénalité
            3, 1,   // Score de base
            4, 3,   // Bonus
            5, 5    // Fort bonus
    );

    public static void main(String[] args) {
        int port = 8080;
        Javalin app = Javalin.create(cfg -> {
            cfg.http.defaultContentType = "application/json";
        }).start(port);

        app.options("/api/preferences", ctx -> {
            setCors(ctx);
            ctx.status(204);
        });

        app.post("/api/preferences", ctx -> {
            setCors(ctx);
            handlePreferences(ctx);
        });

        app.get("/health", ctx -> ctx.result("ok"));
        System.out.println("API sur http://localhost:" + port + "/api/preferences");
    }

    private static void setCors(Context ctx) {
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
            // Récupérer les news et les préférences
            NewsCollection newsCollection = initNewsCollection();
            PreferencesRequest req = MAPPER.readValue(ctx.body(), PreferencesRequest.class);
            Map<String, Integer> userPreferences = flattenPreferences(req.getThemes());

            // CATÉGORISATION (LLM)
            List<String> allowedCategories = new ArrayList<>(userPreferences.keySet());
            NewsCollection categorizedNews = categorizeNewsWithLLM(newsCollection, allowedCategories);


            try {
                System.out.println("--- 1. JSON Après Catégorisation (Avant Tri) ---");
                System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(categorizedNews));
                System.out.println("-------------------------------------------------");
            } catch (JsonProcessingException e) {}

            // TRI (LLM - Prompt 2)
            List<News> sortedNews = sortNewsByPreference(categorizedNews, userPreferences);
            try {
                System.out.println("--- 2. JSON Après Tri (Liste 'sortedNews') ---");
                System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(sortedNews));
                System.out.println("-------------------------------------------------");
            } catch (JsonProcessingException e) { /* ... */ }

            ctx.status(HttpStatus.OK)
                    .json(new NewsCollection(sortedNews));

        } catch (JsonProcessingException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
                    .json(new Error("invalid_json", e.getOriginalMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .json(new Error("server_error", e.getMessage()));
        }
    }


    // ===================================================================
    // ÉTAPE 1 : NOUVELLE VERSION (1 Prompt par Article)
    // ===================================================================
    /**
     * ÉTAPE 1 : Catégorise une collection de news en appelant le LLM
     * POUR CHAQUE article.
     */
    private static NewsCollection categorizeNewsWithLLM(NewsCollection newsCollection, List<String> allowedCategories) throws Exception {

        List<News> categorizedNewsList = new ArrayList<>();
        String categoriesJson = MAPPER.writeValueAsString(allowedCategories);

        System.out.println("--- Début de la Catégorisation (1 par 1) ---");

        // On boucle sur chaque article
        for (News newsItem : newsCollection.getNewsCollection()) {

            // On sérialise UN SEUL article
            String newsJson = MAPPER.writeValueAsString(newsItem);

            String prompt = """
            Tu es un système expert en catégorisation d'actualités.
            TA TÂCHE : Analyser l'article (titre et description) et ajouter le champ "categoryScores".

            CONTRAINTES STRICTES :
            1.  CATEGORIES AUTORISÉES : Tu dois UNIQUEMENT utiliser les catégories de cette liste : %s
            2.  LIMITE : Tu dois assigner au MAXIMUM 2 catégories par article (les plus pertinentes).
            3.  SCORE : Pour chaque catégorie assignée, donne un score ENTIER de 1 (pertinence faible) à 4 (pertinence très élevée).
            4.  FORMAT : Si aucune catégorie autorisée n'est pertinente, le champ "categoryScores" doit être [].

            Retourne UNIQUEMENT le JSON de l'article MIS À JOUR (un seul objet JSON). N'ajoute aucun commentaire.

            LISTE DES CATEGORIES AUTORISÉES :
            %s

            ARTICLE (JSON en entrée) :
            %s

            FORMAT DE RÉPONSE ATTENDU (JSON objet unique) :
            [
              {
                "title": "",
                "link": "",
                "description": "",
                "categoryScores": [
                  {"category": "sport", "score": 4},
                  {"category": "politique", "score": 1}
                ]
              },
            ]
            """.formatted(categoriesJson, categoriesJson, newsJson);

            try {
                // Appel du modèle via LangChain4j
                String llmResponse = llm.generate(prompt);

                // ==================== BLOC DE NETTOYAGE ROBUSTE (pour OBJET) ====================

                System.out.println("--- DEBUG: Réponse BRUTE LLM (Article: " + newsItem.getTitle() + ") ---");
                System.out.println(llmResponse);
                System.out.println("----------------------------------------------");

                // Extraction robuste du bloc JSON : cherche { ... } au lieu de [ ... ]
                int startIndex = llmResponse.indexOf('{');
                int endIndex = llmResponse.lastIndexOf('}');
                String cleanedResponse;

                if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                    cleanedResponse = llmResponse.substring(startIndex, endIndex + 1);
                } else {
                    System.err.println("ERREUR (Etape 1): Bloc JSON '{}' non trouvé pour l'article: " + newsItem.getTitle());
                    cleanedResponse = llmResponse; // Laisse le parseur échouer
                }

                // Parse UN SEUL objet News (plus besoin de TypeReference)
                News processedNews = MAPPER.readValue(cleanedResponse, News.class);
                categorizedNewsList.add(processedNews);

            } catch (Exception e) {
                System.err.println("Erreur Etape 1 (Catégorisation LLM) pour l'article: " + newsItem.getTitle());
                e.printStackTrace();
                // En cas d'échec, on ajoute l'article original non catégorisé
                categorizedNewsList.add(newsItem);
            }
        }

        System.out.println("--- Fin de la Catégorisation ---");
        return new NewsCollection(categorizedNewsList);
    }


    // ===================================================================
    // ÉTAPE 2 : TRI (Inchangée)
    // ===================================================================
    /**
     * ÉTAPE 2 : Trie les news DÉJÀ CATÉGORISÉES en utilisant un 2e prompt LLM.
     * (Cette méthode est INCHANGÉE par rapport à la version précédente)
     */
    private static List<News> sortNewsByPreference(NewsCollection newsCollection, Map<String, Integer> userPreferences) {

        // On crée une liste de "paires" (News, Score) pour le débogage
        List<Map.Entry<News, Integer>> scoredNews = newsCollection.getNewsCollection().stream()
                .map(news -> {
                    int score = calculateMatchScore(news, userPreferences);
                    return Map.entry(news, score); // Crée une "paire"
                })
                .collect(Collectors.toList());

        // IMPRESSION 3 (Debug du Tri)
        System.out.println("--- 3. Débogage des Scores de Tri (Logique Java) ---");
        scoredNews.forEach(entry -> {
            System.out.printf("Score: %-5d | Titre: %s\n", entry.getValue(), entry.getKey().getTitle());
        });
        System.out.println("--------------------------------------------------");

        // Tri final basé sur le score (décroissant)
        return scoredNews.stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue())) // Tri par score décroissant
                .map(Map.Entry::getKey) // Récupère uniquement la News
                .collect(Collectors.toList());
    }

    private static int calculateMatchScore(News news, Map<String, Integer> userPreferences) {
        // Si l'article n'a pas de catégorie, il va au fond du classement.
        if (news.getCategoryScores() == null || news.getCategoryScores().isEmpty()) {
            return -1000;
        }

        int totalScore = 0;
        boolean hasAnyMatch = false;

        // On itère sur les catégories DE LA NEWS (assignées par le LLM)
        for (NewsCategoryScore newsScoreEntry : news.getCategoryScores()) {
            String newsCategory = newsScoreEntry.getCategory();
            int newsScore = newsScoreEntry.getScore(); // 1-4 (pertinence de l'article)

            // On vérifie si l'utilisateur a une préférence pour cette catégorie
            if (userPreferences.containsKey(newsCategory)) {
                hasAnyMatch = true;

                // 1. Récupérer le NIVEAU de préférence (1-5)
                int prefLevel = userPreferences.get(newsCategory);

                // 2. Convertir le NIVEAU en POIDS (-5 à +5)
                int prefWeight = PREFERENCE_WEIGHTS.getOrDefault(prefLevel, 0);

                // 3. Calculer le score (Poids * Pertinence)
                totalScore += prefWeight * newsScore;
            }
        }

        // Si la news n'a AUCUNE catégorie qui matche les préférences,
        // on la pénalise, mais moins que si elle était non-catégorisée.
        if (!hasAnyMatch) {
            return -500;
        }

        return totalScore;
    }

    // ... (Le reste de la classe, flattenPreferences et DTOs, est inchangé)

    private static Map<String, Integer> flattenPreferences(Themes themes) {
        Map<String, Integer> preferences = new java.util.HashMap<>();
        try {
            for (java.lang.reflect.Field field : themes.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(themes);
                if (value instanceof ThemeSelection) {
                    ThemeSelection selection = (ThemeSelection) value;
                    if (selection.getLevel() != null) {
                        JsonProperty annotation = field.getAnnotation(JsonProperty.class);
                        String themeName = (annotation != null) ? annotation.value() : field.getName();
                        preferences.put(themeName, selection.getLevel());
                    }
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return preferences;
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
        private Integer level;
        private String rss;
        public ThemeSelection() {}
        public ThemeSelection(Integer level, String rss) { this.level = level; this.rss = rss; }
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