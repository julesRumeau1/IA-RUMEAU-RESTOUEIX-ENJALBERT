package model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NewsCategoryScoreTest {

    @Test
    @DisplayName("Test constructeur par défaut")
    void testDefaultConstructor() {
        // Given
        NewsCategoryScore score = new NewsCategoryScore();

        // Then
        assertThat(score.getCategory()).isNull();
        assertThat(score.getScore()).isEqualTo(0);
    }

    @Test
    @DisplayName("Test constructeur paramétré (nominal)")
    void testParamConstructor() {
        // Given
        NewsCategoryScore score = new NewsCategoryScore("sport", 3);

        // Then
        assertThat(score.getCategory()).isEqualTo("sport");
        assertThat(score.getScore()).isEqualTo(3);
    }

    @Test
    @DisplayName("Test plafonnement du score (constructeur)")
    void testConstructorScoreClamping() {
        // Given
        NewsCategoryScore scoreHigh = new NewsCategoryScore("sport", 10); // Trop haut
        NewsCategoryScore scoreLow = new NewsCategoryScore("politique", -5); // Trop bas

        // Then
        assertThat(scoreHigh.getScore()).isEqualTo(4); // Doit être 4 (MAX_SCORE)
        assertThat(scoreLow.getScore()).isEqualTo(0); // Doit être 0
    }

    @Test
    @DisplayName("Test plafonnement du score (setter)")
    void testSetterScoreClamping() {
        // Given
        NewsCategoryScore score = new NewsCategoryScore();

        // When
        score.setScore(10);
        // Then
        assertThat(score.getScore()).isEqualTo(4);

        // When
        score.setScore(-5);
        // Then
        assertThat(score.getScore()).isEqualTo(0);

        // When
        score.setScore(2);
        // Then
        assertThat(score.getScore()).isEqualTo(2);
    }

    @Test
    @DisplayName("Test setters (catégorie)")
    void testSetCategory() {
        // Given
        NewsCategoryScore score = new NewsCategoryScore();

        // When
        score.setCategory("economie");

        // Then
        assertThat(score.getCategory()).isEqualTo("economie");
    }

    @Test
    @DisplayName("Test toString()")
    void testToString() {
        // Given
        NewsCategoryScore score = new NewsCategoryScore("culture", 2);

        // Then
        assertThat(score.toString()).isEqualTo("culture: 2");
    }
}
