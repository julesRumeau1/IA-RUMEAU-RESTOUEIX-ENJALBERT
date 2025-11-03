package api.service;

import api.dto.PreferencesRequest;
import api.util.ApiException;
import api.util.PreferencesUtils;
import dev.langchain4j.model.chat.ChatLanguageModel;
import model.News;
import model.NewsCollection;
import rss.RssFetcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service chargé de récupérer et classer des articles d'actualité
 * selon les préférences utilisateur à l'aide d'un modèle de langage (LLM).
 */
public final class NewsService {

    /** Service de récupération des flux RSS. */
    private final RssFetcher rssFetcher;

    /** Modèle de langage utilisé pour la catégorisation. */
    private final ChatLanguageModel llm;

    /**
     * Crée une instance de {@code NewsService}.
     *
     * @param inputRssFetcher instance du fetcher RSS à utiliser
     * @param inputLlm modèle de langage chargé de la catégorisation
     */
    public NewsService(
            final RssFetcher inputRssFetcher,
            final ChatLanguageModel inputLlm
    ) {
        this.rssFetcher = inputRssFetcher;
        this.llm = inputLlm;
    }

    /**
     * Récupère les actualités et les trie selon les préférences utilisateur.
     *
     * @param request la requête contenant les thèmes de préférences
     * @return une liste d'articles classés selon les préférences
     * @throws Exception si une erreur survient lors de la
     * récupération ou du traitement
     */
    public List<News> getNewsForPreferences(final PreferencesRequest request)
            throws ApiException {

        List<Map<String, String>> rawNews = rssFetcher.fetchRawNews();
        NewsCollection newsCollection =
                NewsCollectionFactory.fromRawNews(rawNews);

        Map<String, Integer> userPreferences =
                PreferencesUtils.flattenPreferences(request.getThemes());

        List<String> orderedCategories =
                new ArrayList<>(userPreferences.keySet());

        NewsCollection categorized =
                LLMScorer.categorize(newsCollection, orderedCategories, llm);

        return NewsSorter.sortByPreferences(categorized, userPreferences);
    }
}
