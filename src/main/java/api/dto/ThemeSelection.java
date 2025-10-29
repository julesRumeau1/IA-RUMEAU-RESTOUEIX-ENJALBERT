package api.dto;

/**
 * Selection de thème basique avec le niveau
 */
public class ThemeSelection {

    private Integer level;

    public ThemeSelection(Integer level, String rss) {
        this.level = level;
    }

    public Integer getLevel() {
        return level;
    }

}
