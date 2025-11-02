package api.service;

import api.util.ApiException;
import model.News;
import model.NewsCollection;

import java.util.List;
import java.util.Map;

public class NewsCollectionFactory {

    private NewsCollectionFactory() {
        // utilitaire
    }

    public static NewsCollection fromRawNews(List<Map<String, String>> rawNews) throws ApiException{
        if (rawNews == null) {
            throw new ApiException("empty_news_list", "La liste des articles est vide ou nulle");
        }

        NewsCollection collection = new NewsCollection();
        for (Map<String, String> data : rawNews) {
            if (!data.containsKey("title") || !data.containsKey("link") || !data.containsKey("description")) {
                throw new ApiException("invalid_news_item", "Un article ne contient pas toutes les informations requises");
            }
            collection.add(new News(
                    data.get("title"),
                    data.get("link"),
                    data.get("description")
            ));
        }
        return collection;
    }
}
