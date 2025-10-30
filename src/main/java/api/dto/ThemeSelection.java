package api.dto;

/**
 * Selection de thème basique avec le niveau
 */
public class ThemeSelection {

    private Integer level;

    /**
     * Constructeur par défaut (vide) requis par Jackson
     */
    public ThemeSelection() {
    }

    /**
     * Constructeur utilitaire
     */
    public ThemeSelection(Integer level, String rss) {
        this.level = level;
    }

    public Integer getLevel() {
        return level;
    }

    /**
     * NOUVEAU: Setter requis par Jackson pour injecter la valeur
     */
    public void setLevel(Integer level) {
        this.level = level;
    }
}