package api.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ThemesTest {

    @Test
    @DisplayName("Test constructeur par défaut (objet non nul)")
    void testDefaultConstructor() {
        // Given
        Themes themes = new Themes();

        // Then
        assertThat(themes).isNotNull();
    }

}
