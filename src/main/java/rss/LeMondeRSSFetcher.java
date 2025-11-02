package rss;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utilitaire pour récupérer le flux RSS du Monde.
 */
public final class LeMondeRSSFetcher implements RssFetcher {

    public static final LeMondeRSSFetcher INSTANCE = new LeMondeRSSFetcher();

    /** URL du flux RSS du Monde. */
    private static final String RSS_URL = "https://www.lemonde.fr/rss/une.xml";

    /** Longueur du préfixe <![CDATA[. */
    private static final int CDATA_PREFIX_LEN = 9;

    /** Longueur du suffixe ] ] >. */
    private static final int CDATA_SUFFIX_LEN = 3;

    public LeMondeRSSFetcher() {
        // utilitaire
    }

    /**
     * Récupère les news brutes depuis le flux RSS.
     *
     * @return une liste de maps (title, link, description, category)
     */
    @Override
    public List<Map<String, String>> fetchRawNews() {
        List<Map<String, String>> newsList = new ArrayList<>();

        try {
            String rssContent = fetchTextFromUrl(RSS_URL);

            String[] parts = rssContent.split("<item>");
            // On saute la partie 0 qui est l'entête avant le premier <item>
            for (int i = 1; i < parts.length; i++) {
                String item = parts[i];
                item = item.substring(0, item.indexOf("</item>"));

                Map<String, String> newsData = new HashMap<>();
                newsData.put("title", extractTagContent(item, "title"));
                newsData.put("link", extractTagContent(item, "link"));
                newsData.put("description",
                        extractTagContent(item, "description"));
                newsData.put("category",
                        extractTagContent(item, "category"));

                newsList.add(newsData);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return newsList;
    }

    /**
     * Extrait le contenu d'un tag XML simple.
     *
     * @param text texte dans lequel chercher
     * @param tag  nom du tag
     * @return le contenu du tag, ou chaîne vide si absent
     */
    public static String extractTagContent(final String text,
                                            final String tag) {
        String openTag = "<" + tag + ">";
        String closeTag = "</" + tag + ">";
        int start = text.indexOf(openTag);
        int end = text.indexOf(closeTag);

        if (start == -1 || end == -1 || end <= start) {
            return "";
        }

        String content =
                text.substring(start + openTag.length(), end).trim();

        // Supprimer les balises CDATA si présentes
        if (content.startsWith("<![CDATA[") && content.endsWith("]]>")) {
            content = content.substring(
                    CDATA_PREFIX_LEN,
                    content.length() - CDATA_SUFFIX_LEN
            );
        }

        return content.trim();
    }

    /**
     * Récupère le contenu texte depuis une URL.
     *
     * @param urlString URL cible
     * @return le contenu récupéré
     * @throws IOException en cas d'erreur réseau
     */
    private static String fetchTextFromUrl(final String urlString)
            throws IOException {
        StringBuilder sb = new StringBuilder();

        URL url = new URL(urlString);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", "Java Simple RSS Fetcher");

        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream(),
                        StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }
}
