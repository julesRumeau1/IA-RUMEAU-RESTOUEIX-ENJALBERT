package rss;

import model.News;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LeMondeRSSFetcher {

    private static final String RSS_URL = "https://www.lemonde.fr/rss/une.xml";

    public List<News> fetchNews() {
        List<News> newsList = new ArrayList<>();

        try {
            URL url = new URL(RSS_URL);
            InputStream stream = url.openStream();

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(stream);
            doc.getDocumentElement().normalize();

            NodeList items = doc.getElementsByTagName("item");

            for (int i = 0; i < items.getLength(); i++) {
                Node node = items.item(i);

                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;

                    String title = getTextContent(element, "title");
                    String link = getTextContent(element, "link");
                    String description = getTextContent(element, "description");

                    News news = new News(title, link, description, LocalDateTime.now());
                    newsList.add(news);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return newsList;
    }

    private String getTextContent(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagName(tagName);
        if (list.getLength() > 0) {
            return list.item(0).getTextContent();
        }
        return "";
    }
}
