package api.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PreferencesRequestTest {

    @Test
    @DisplayName("Test constructeur par défaut (themes nuls)")
    void testDefaultConstructor() {
        // Given
        PreferencesRequest request = new PreferencesRequest();

        // Then
        assertThat(request.getThemes()).isNull();
    }
}
