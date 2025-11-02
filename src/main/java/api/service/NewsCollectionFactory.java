package api.service;

import model.News;
import model.NewsCollection;

import java.util.List;
import java.util.Map;

public class NewsCollectionFactory {

    private NewsCollectionFactory() {
        // utilitaire
    }

    public static NewsCollection fromRawNews(List<Map<String, String>> rawNews) {
        NewsCollection collection = new NewsCollection();
        for (Map<String, String> data : rawNews) {
            collection.add(new News(
                    data.get("title"),
                    data.get("link"),
                    data.get("description")
            ));
        }
        return collection;
    }
}
