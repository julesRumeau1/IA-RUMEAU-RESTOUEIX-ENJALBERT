package api.service;

import api.util.ApiException;
import model.News;
import model.NewsCollection;

import java.util.List;
import java.util.Map;


/**
 * Fabrique utilitaire pour construire des {@link NewsCollection}
 * à partir de données brutes récupérées depuis des flux RSS.
 * <p>
 * Cette classe ne doit pas être instanciée.
 * </p>
 */
public final class NewsCollectionFactory {

    /** Constructeur privé pour empêcher l’instanciation. */
    private NewsCollectionFactory() {
        // utilitaire
    }

    /**
     * Construit une {@link NewsCollection} à partir d'une liste d'articles
     * bruts représentés par des maps (clé/valeur).
     * <p>
     * Chaque entrée doit contenir au minimum les clés :
     * <ul>
     *   <li>{@code title}</li>
     *   <li>{@code link}</li>
     *   <li>{@code description}</li>
     * </ul>
     * </p>
     *
     * @param rawNews la liste brute des articles RSS,
     * où chaque élément est une
     * map contenant les métadonnées d’un article
     * @return une instance de {@link NewsCollection} contenant les
     * articles valides
     * @throws ApiException si la liste est vide, nulle, ou si un
     * article est invalide
     */
    public static NewsCollection fromRawNews(
            final List<Map<String, String>> rawNews
    ) throws ApiException {

        if (rawNews == null) {
            throw new ApiException(
                    "empty_news_list",
                    "La liste des articles est vide ou nulle"
            );
        }

        final NewsCollection collection = new NewsCollection();
        for (Map<String, String> data : rawNews) {
            if (!data.containsKey("title")
                    || !data.containsKey("link")
                    || !data.containsKey("description")) {
                throw new ApiException(
                        "invalid_news_item",
                        "Un article ne contient pas toutes les "
                                + "informations requises"
                );
            }

            collection.add(new News(
                    data.get("title"),
                    data.get("link"),
                    data.get("description")
            ));
        }
        return collection;
    }
}
