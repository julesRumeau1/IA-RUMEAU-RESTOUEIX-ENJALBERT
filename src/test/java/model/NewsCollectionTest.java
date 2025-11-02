package model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class NewsCollectionTest {
    private News newsSportHi;
    private News newsSportLo;
    private News newsEco;
    private News newsNoMatch;
    private NewsCollection collection;

    @BeforeEach
    void setUp() {
        // Given : plusieurs articles avec différents scores
        newsSportHi = new News("Sport 1", "l1", "d1");
        newsSportHi.setCategoryScores(List.of(new NewsCategoryScore("sport", 4))); // Score 4

        newsSportLo = new News("Sport 2", "l2", "d2");
        newsSportLo.setCategoryScores(List.of(new NewsCategoryScore("sport", 2))); // Score 2

        newsEco = new News("Eco 1", "l3", "d3");
        newsEco.setCategoryScores(List.of(new NewsCategoryScore("economie", 3))); // Score 0 pour "sport"

        newsNoMatch = new News("Culture 1", "l4", "d4");
        newsNoMatch.setCategoryScores(List.of(new NewsCategoryScore("culture", 2))); // Score 0 pour "sport"

        collection = new NewsCollection(List.of(newsSportHi, newsSportLo, newsEco, newsNoMatch));
    }

    @Test
    @DisplayName("Test constructeurs, add et size")
    void testConstructAndAdd() {
        // Given
        NewsCollection col1 = new NewsCollection();

        // Then
        assertThat(col1.size()).isEqualTo(0);
        assertThat(col1.getNewsCollection()).isNotNull().isEmpty();

        // When
        News n1 = new News();
        News n2 = new News();
        col1.add(n1);
        col1.addAll(List.of(n2));

        // Then
        assertThat(col1.size()).isEqualTo(2);
        assertThat(col1.getNewsCollection()).contains(n1, n2);
    }

    @Test
    @DisplayName("Test constructeur avec liste")
    void testParamConstructor() {
        // Given
        List<News> initialList = List.of(newsSportHi, newsEco);

        // When
        NewsCollection col = new NewsCollection(initialList);

        // Then
        assertThat(col.size()).isEqualTo(2);
        assertThat(col.getNewsCollection()).isEqualTo(initialList);
    }

    @Test
    @DisplayName("Test filterByCategory() (cas trouvé)")
    void testFilterByCategoryFound() {
        // When
        List<News> sportNews = collection.filterByCategory("sport");

        // Then
        assertThat(sportNews)
                .hasSize(2)
                .containsExactlyInAnyOrder(newsSportHi, newsSportLo);
    }

    @Test
    @DisplayName("Test filterByCategory() (cas non trouvé)")
    void testFilterByCategoryNotFound() {
        // When
        List<News> politiqueNews = collection.filterByCategory("politique");

        // Then
        assertThat(politiqueNews).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Test sortByCategoryScoreDesc()")
    void testSortByCategoryScoreDesc() {
        // When
        List<News> sorted = collection.sortByCategoryScoreDesc("sport");

        // Then
        // Doit trier par score "sport": 4, 2, 0, 0
        assertThat(sorted)
                .hasSize(4)
                .extracting(News::getTitle)
                .containsExactly(
                        "Sport 1",      // Score 4
                        "Sport 2",      // Score 2
                        "Eco 1",        // Score 0
                        "Culture 1"     // Score 0
                );
    }

    @Test
    @DisplayName("Test sortByCategoryScoreDesc() (autre cat)")
    void testSortByCategoryScoreDescOther() {
        // When
        List<News> sorted = collection.sortByCategoryScoreDesc("economie");

        // Then
        // Doit trier par score "economie": 3, 0, 0, 0
        assertThat(sorted)
                .hasSize(4)
                .extracting(News::getTitle)
                .containsExactly(
                        "Eco 1",        // Score 3
                        "Sport 1",      // Score 0
                        "Sport 2",      // Score 0
                        "Culture 1"     // Score 0
                );
    }
}
