package main;

import api.PreferencesApi;

/**
 * Point d'entrée de l'application.
 */
public final class Main {

    private Main() {
        // utilitaire
    }

    /**
     * Lance l'API et la maintient en vie.
     *
     * @param args arguments de la ligne de commande
     */
    public static void main(final String[] args) {
        // Lancez l'API directement dans le thread principal.
        // On suppose que PreferencesApi.main() est une méthode bloquante
        // (comme app.start() de Javalin), ce qui est normal pour un serveur.
        try {
            System.out.println("[main.Main] Démarrage de l'API sur http://localhost:8080");
            PreferencesApi.main(new String[0]);
        } catch (Exception e) {
            System.err.println("[main.Main] Échec démarrage API : "
                    + e.getMessage());
            System.exit(1);
        }
    }
}