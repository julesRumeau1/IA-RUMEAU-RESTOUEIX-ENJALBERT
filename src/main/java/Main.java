import rss.LeMondeRSSFetcher;

import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        List<Map<String, String>> rawNews = LeMondeRSSFetcher.fetchRawNews();

        for (Map<String, String> newsData : rawNews) {
            String title = newsData.get("title");
            String link = newsData.get("link");
            String description = newsData.get("description");
            System.out.println(title);
            System.out.println(link);
            System.out.println(description);
        }
    }
}
