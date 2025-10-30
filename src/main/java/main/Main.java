package main;

import api.PreferencesApi;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Point d'entrée de l'application.
 */
public final class Main {

    /** Permet d'initiliser le délai du startup (0.7s). */
    private static final int STARTUP_DELAY_MS = 700;

    private Main() {
        // utilitaire
    }

    /**
     * Lance l'API puis ouvre l'UI.
     *
     * @param args arguments de la ligne de commande
     */
    public static void main(final String[] args) {
        // 1) API Javalin
        Thread apiThread = new Thread(() -> {
            try {
                System.out.println("[main.Main] Démarrage de l'API…");
                PreferencesApi.main(new String[0]);
            } catch (Exception e) {
                System.err.println("[main.Main] Échec démarrage API : "
                        + e.getMessage());
            }
        }, "api-thread");
        apiThread.setDaemon(true);
        apiThread.start();

        // 2) Ouvrir l'UI dans le navigateur
        try {
            Thread.sleep(STARTUP_DELAY_MS);
            openIndexHtmlInBrowser();
        } catch (Exception e) {
            System.err.println("[main.Main] Impossible "
                    + "d'ouvrir le navigateur : "
                    + e.getMessage());
        }

        System.out.println("[main.Main] Tout est lancé. API: "
                + "http://localhost:8080/api/preferences");
    }

    /**
     * Ouvre le fichier ui/index.html dans le navigateur par défaut.
     *
     * @throws Exception si le fichier n'est pas trouvé ou ne peut pas être
     * ouvert
     */
    private static void openIndexHtmlInBrowser() throws Exception {
        Path html = findIndexHtml();
        if (html == null) {
            throw new IOException("index.html introuvable. Chemins testés : "
                    + "src/main/java/ui/index.html, "
                    + "src/main/resources/ui/index.html, "
                    + "ui/index.html");
        }
        URI uri = html.toUri();
        System.out.println("[main.Main] Ouverture de l'UI : " + uri);
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(uri);
        } else {
            System.out.println("[main.Main] Desktop non supporté. "
                    + "Ouvrez manuellement : " + uri);
        }
    }

    /**
     * Cherche index.html à des emplacements plausibles selon la structure.
     *
     * @return le chemin absolu de index.html s'il est trouvé, sinon
     * {@code null}
     */
    private static Path findIndexHtml() {
        Path base = Paths.get(System.getProperty("user.dir"));
        Path[] candidates = new Path[] {
                base.resolve("src/main/java/ui/index.html"),
                base.resolve("src/main/resources/ui/index.html"),
                base.resolve("ui/index.html")
        };
        for (Path p : candidates) {
            if (Files.exists(p)) {
                return p.toAbsolutePath().normalize();
            }
        }
        return null;
    }
}
