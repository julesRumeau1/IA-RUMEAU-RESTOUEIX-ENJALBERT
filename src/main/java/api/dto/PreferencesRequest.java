package api.dto;

/**
 * Requête HTTP reçue pour le chemin /api/preferences.
 */
public final class PreferencesRequest {

    /** Thèmes envoyés dans la requête. */
    private Themes themes;

    /**
     * Constructeur vide requis pour la désérialisation JSON.
     */
    public PreferencesRequest() {
        // Constructeur par défaut
    }

    /**
     * Retourne les thèmes de la requête.
     *
     * @return les thèmes envoyés par le client
     */
    public Themes getThemes() {
        return themes;
    }
}
