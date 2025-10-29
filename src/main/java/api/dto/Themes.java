package api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Liste des thèmes pour formattage JSON
 */
public class Themes {

    @JsonProperty("politique")     private ThemeSelection politique;
    @JsonProperty("international") private ThemeSelection international;
    @JsonProperty("economie")      private ThemeSelection economie;
    @JsonProperty("societe")       private ThemeSelection societe;
    @JsonProperty("sport")         private ThemeSelection sport;
    @JsonProperty("culture")       private ThemeSelection culture;
    @JsonProperty("sciences")      private ThemeSelection sciences;
    @JsonProperty("planete")       private ThemeSelection planete;
    @JsonProperty("technologies")  private ThemeSelection technologies;
    @JsonProperty("sante")         private ThemeSelection sante;
    @JsonProperty("education")     private ThemeSelection education;
    @JsonProperty("idees")         private ThemeSelection idees;

    public Themes() {
    }
}
