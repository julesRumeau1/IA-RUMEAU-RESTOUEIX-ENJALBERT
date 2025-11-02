package api.service;

import api.dto.PreferencesRequest;
import api.util.PreferencesUtils;
import dev.langchain4j.model.chat.ChatLanguageModel;
import model.News;
import model.NewsCollection;
import rss.RssFetcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NewsService {

    private final RssFetcher rssFetcher;
    private final ChatLanguageModel llm;

    public NewsService(RssFetcher rssFetcher, ChatLanguageModel llm) {
        this.rssFetcher = rssFetcher;
        this.llm = llm;
    }

    public List<News> getNewsForPreferences(PreferencesRequest request) throws Exception {
        List<Map<String, String>> rawNews = rssFetcher.fetchRawNews();
        NewsCollection newsCollection = NewsCollectionFactory.fromRawNews(rawNews);

        Map<String, Integer> userPreferences = PreferencesUtils.flattenPreferences(request.getThemes());
        List<String> orderedCategories = new ArrayList<>(userPreferences.keySet());

        NewsCollection categorized = LLMScorer.categorize(newsCollection, orderedCategories, llm);
        return NewsSorter.sortByPreferences(categorized, userPreferences);
    }
}
