package api.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ThemeSelectionTest {

    @Test
    @DisplayName("Test constructeur par défaut (champs nuls)")
    void testDefaultConstructor() {
        // Given
        ThemeSelection selection = new ThemeSelection();

        // Then
        assertThat(selection.getLevel()).isNull();
        assertThat(selection.getRss()).isNull();
    }

    @Test
    @DisplayName("Test constructeur paramétré")
    void testParametrizedConstructor() {
        // Given
        Integer expectedLevel = 5;
        String expectedRss = "https://example.com/rss/sport";

        // When
        ThemeSelection selection = new ThemeSelection(expectedLevel, expectedRss);

        // Then
        assertThat(selection.getLevel()).isEqualTo(expectedLevel);
        assertThat(selection.getRss()).isEqualTo(expectedRss);
    }

    @Test
    @DisplayName("Test des setters")
    void testSetters() {
        // Given
        ThemeSelection selection = new ThemeSelection(1, "http://old.rss");
        Integer newLevel = 3;
        String newRss = "http://new.rss";

        // When
        selection.setLevel(newLevel);
        selection.setRss(newRss);

        // Then
        assertThat(selection.getLevel()).isEqualTo(newLevel);
        assertThat(selection.getRss()).isEqualTo(newRss);
    }
}
