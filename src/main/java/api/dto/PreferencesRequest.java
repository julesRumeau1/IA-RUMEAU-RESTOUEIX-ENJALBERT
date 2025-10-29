package api.dto;

/**
 * Request body pour le chemin /api/preferences
 */
public class PreferencesRequest {

    private Themes themes;

    public PreferencesRequest() {
    }

    public Themes getThemes() {
        return themes;
    }

}
