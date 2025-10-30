package api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Liste des thèmes pour le formattage JSON.
 */
public final class Themes {

    /** Thème politique. */
    @JsonProperty("politique")
    private ThemeSelection politique;

    /** Thème international. */
    @JsonProperty("international")
    private ThemeSelection international;

    /** Thème économie. */
    @JsonProperty("economie")
    private ThemeSelection economie;

    /** Thème société. */
    @JsonProperty("societe")
    private ThemeSelection societe;

    /** Thème sport. */
    @JsonProperty("sport")
    private ThemeSelection sport;

    /** Thème culture. */
    @JsonProperty("culture")
    private ThemeSelection culture;

    /** Thème sciences. */
    @JsonProperty("sciences")
    private ThemeSelection sciences;

    /** Thème planète. */
    @JsonProperty("planete")
    private ThemeSelection planete;

    /** Thème technologies. */
    @JsonProperty("technologies")
    private ThemeSelection technologies;

    /** Thème santé. */
    @JsonProperty("sante")
    private ThemeSelection sante;

    /** Thème éducation. */
    @JsonProperty("education")
    private ThemeSelection education;

    /** Thème idées. */
    @JsonProperty("idees")
    private ThemeSelection idees;

    /**
     * Constructeur vide requis pour la désérialisation JSON.
     */
    public Themes() {
        // Constructeur par défaut
    }
}
