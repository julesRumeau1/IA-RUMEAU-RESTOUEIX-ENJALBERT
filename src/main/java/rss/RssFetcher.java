package rss;

import java.util.List;
import java.util.Map;

public interface RssFetcher {
    /**
     * Récupère les news brutes depuis un flux RSS.
     * @return liste de maps avec title, link, description, category
     */
    List<Map<String, String>> fetchRawNews();
}
