package api.util;

import api.dto.ThemeSelection;
import api.dto.Themes;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Utile pour le mapping des préférences utilisateurs
 */
public final class PreferencesUtils {

    private PreferencesUtils() {
    }

    /**
     * Réorganisé les préférences pour avoir un dictionnaire nom -> valeur
     * @param themes Le thème voulu
     * @return Map thème -> niveau
     */
    public static Map<String, Integer> flattenPreferences(Themes themes) {
        Map<String, Integer> preferences = new HashMap<>();
        if (themes == null) {
            return preferences;
        }
        try {
            for (Field field : themes.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(themes);
                if (value instanceof ThemeSelection) {
                    ThemeSelection selection = (ThemeSelection) value;
                    if (selection.getLevel() != null) {
                        JsonProperty annotation = field.getAnnotation(JsonProperty.class);
                        String themeName = annotation != null ? annotation.value() : field.getName();
                        preferences.put(themeName, selection.getLevel());
                    }
                }
            }
        } catch (IllegalAccessException e) {
            // keep method side-effect free, just return what we have
        }
        return preferences;
    }
}
