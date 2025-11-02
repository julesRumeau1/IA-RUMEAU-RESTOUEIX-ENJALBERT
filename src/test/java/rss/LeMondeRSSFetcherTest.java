package rss;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class LeMondeRSSFetcherTest {

    @Test
    @DisplayName("Test extractTagContent (cas simple)")
    void testExtractBasicTag() {
        // Given
        String xml = "<item><title>Le Titre</title><link>http://lien</link></item>";

        // When
        // Appel direct et simple
        String title = LeMondeRSSFetcher.extractTagContent(xml, "title");
        String link = LeMondeRSSFetcher.extractTagContent(xml, "link");

        // Then
        assertThat(title).isEqualTo("Le Titre");
        assertThat(link).isEqualTo("http://lien");
    }

    @Test
    @DisplayName("Test extractTagContent (avec CDATA)")
    void testExtractCdataTag() {
        // Given
        String xml = "<description><![CDATA[Une description <avec> du html.]]></description>";

        // When
        String desc = LeMondeRSSFetcher.extractTagContent(xml, "description");

        // Then
        assertThat(desc).isEqualTo("Une description <avec> du html.");
    }

    @Test
    @DisplayName("Test extractTagContent (avec espaces)")
    void testExtractTagWithWhitespace() {
        // Given
        String xml = "<title>  \n  Un titre avec espaces   \n </title>";

        // When
        String title = LeMondeRSSFetcher.extractTagContent(xml, "title");

        // Then
        assertThat(title).isEqualTo("Un titre avec espaces");
    }

    @Test
    @DisplayName("Test extractTagContent (tag non trouvé)")
    void testExtractTagNotFound() {
        // Given
        String xml = "<item><title>Le Titre</title></item>";

        // When
        String category = LeMondeRSSFetcher.extractTagContent(xml, "category");

        // Then
        assertThat(category).isEmpty();
    }

    @Test
    @DisplayName("Test extractTagContent (tag mal formé)")
    void testExtractTagMalformed() {
        // Given
        String xml = "<item><title>Le Titre</item>"; // Pas de </title>

        // When
        String title = LeMondeRSSFetcher.extractTagContent(xml, "title");

        // Then
        assertThat(title).isEmpty();
    }
}
