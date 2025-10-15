package rss;

import java.io.*;
import java.net.*;
import java.util.*;

public class LeMondeRSSFetcher {

    private static final String RSS_URL = "https://www.lemonde.fr/rss/une.xml";

    public static List<Map<String, String>> fetchRawNews() {
        List<Map<String, String>> newsList = new ArrayList<>();

        try {
            // Télécharger le flux RSS complet en texte
            String rssContent = fetchTextFromUrl(RSS_URL);

            // Séparer les items (par simple split sur <item> ... </item>)
            String[] parts = rssContent.split("<item>");
            for (int i = 1; i < parts.length; i++) {  // On skip le 0 qui est avant le premier <item>
                String item = parts[i];
                item = item.substring(0, item.indexOf("</item>"));

                Map<String, String> newsData = new HashMap<>();
                newsData.put("title", extractTagContent(item, "title"));
                newsData.put("link", extractTagContent(item, "link"));
                newsData.put("description", extractTagContent(item, "description"));
                newsData.put("category", extractTagContent(item, "category"));

                /**
                // Récupérer le contenu HTML complet depuis le lien
                String link = newsData.get("link");
                if (link != null && !link.isEmpty()) {
                    String htmlContent = fetchTextFromUrl(link);
                    newsData.put("htmlContent", htmlContent);
                } else {
                    newsData.put("htmlContent", "");
                }
                **/
                newsList.add(newsData);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return newsList;
    }

    // Méthode simple pour extraire le contenu entre <tag> et </tag> dans un texte donné
    private static String extractTagContent(String text, String tag) {
        String openTag = "<" + tag + ">";
        String closeTag = "</" + tag + ">";
        int start = text.indexOf(openTag);
        int end = text.indexOf(closeTag);
        if (start == -1 || end == -1 || end <= start) return "";
        return text.substring(start + openTag.length(), end).trim();
    }

    // Méthode simple pour récupérer un contenu texte depuis une URL
    private static String fetchTextFromUrl(String urlString) throws IOException {
        StringBuilder sb = new StringBuilder();

        URL url = new URL(urlString);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", "Java Simple RSS Fetcher");

        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }
}
