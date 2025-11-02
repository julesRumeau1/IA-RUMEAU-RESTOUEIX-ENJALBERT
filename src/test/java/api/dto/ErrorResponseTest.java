package api.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ErrorResponseTest {

    @Test
    @DisplayName("Test constructeur et getters")
    void testConstructorAndGetters() {
        // Given
        String expectedCode = "AUTH_001";
        String expectedMessage = "Token invalide";

        // When
        ErrorResponse errorResponse = new ErrorResponse(expectedCode, expectedMessage);

        // Then
        assertThat(errorResponse.getCode()).isEqualTo(expectedCode);
        assertThat(errorResponse.getMessage()).isEqualTo(expectedMessage);
    }
}