import rss.LeMondeRSSFetcher;
import model.News;

public class Main {
    public static void main(String[] args) {
        LeMondeRSSFetcher fetcher = new LeMondeRSSFetcher();
        for (News news : fetcher.fetchNews()) {
            System.out.println(news);
        }
    }
}
