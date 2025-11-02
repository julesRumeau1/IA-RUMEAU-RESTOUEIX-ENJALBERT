package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class NewsTest {

    private News news;

    @BeforeEach
    void setUp() {
        news = new News("Titre Test", "http://lien.test", "Description Test");

        List<NewsCategoryScore> scores = List.of(
                new NewsCategoryScore("sport", 4),
                new NewsCategoryScore("economie", 2)
        );
        news.setCategoryScores(scores);
    }

    @Test
    @DisplayName("Test constructeur par défaut")
    void testDefaultConstructor() {
        // Given
        News emptyNews = new News();

        // Then
        assertThat(emptyNews.getTitle()).isNull();
        assertThat(emptyNews.getLink()).isNull();
        assertThat(emptyNews.getDescription()).isNull();
        assertThat(emptyNews.getCategoryScores()).isNull();
    }

    @Test
    @DisplayName("Test constructeur paramétré")
    void testParamConstructor() {
        // Given
        News constructedNews = new News("Titre", "Lien", "Desc");

        // Then
        assertThat(constructedNews.getTitle()).isEqualTo("Titre");
        assertThat(constructedNews.getLink()).isEqualTo("Lien");
        assertThat(constructedNews.getDescription()).isEqualTo("Desc");
        assertThat(constructedNews.getCategoryScores()).isNull(); // Le constructeur ne les initialise pas
    }

    @Test
    @DisplayName("Test des setters")
    void testSetters() {
        // Given
        News n = new News();
        List<NewsCategoryScore> scores = List.of(new NewsCategoryScore("test", 1));

        // When
        n.setTitle("Nouveau Titre");
        n.setLink("http://nouveau.lien");
        n.setDescription("Nouvelle Desc");
        n.setCategoryScores(scores);

        // Then
        assertThat(n.getTitle()).isEqualTo("Nouveau Titre");
        assertThat(n.getLink()).isEqualTo("http://nouveau.lien");
        assertThat(n.getDescription()).isEqualTo("Nouvelle Desc");
        assertThat(n.getCategoryScores()).isEqualTo(scores);
    }

    @Test
    @DisplayName("Test getScoreFor()")
    void testGetScoreFor() {
        // Then
        // Cas trouvé
        assertThat(news.getScoreFor("sport")).isEqualTo(4);

        // Cas insensible à la casse
        assertThat(news.getScoreFor("ECONOMIE")).isEqualTo(2);

        // Cas non trouvé
        assertThat(news.getScoreFor("politique")).isEqualTo(0);

        // Cas avec liste de scores nulle
        News emptyNews = new News();
        assertThat(emptyNews.getScoreFor("sport")).isEqualTo(0);
    }

    @Test
    @DisplayName("Test toString()")
    void testToString() {
        // Given
        String s = news.toString();

        // Then
        assertThat(s).contains("title='Titre Test'");
        assertThat(s).contains("scores=[sport: 4, economie: 2]");
    }
}
