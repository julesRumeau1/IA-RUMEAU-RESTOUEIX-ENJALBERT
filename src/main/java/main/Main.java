package main;

import api.PreferencesApi;
import api.util.CorsUtil;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import java.util.logging.Logger;

/**
 * Point d'entrée principal de l'application.
 * <p>
 * Cette classe initialise le serveur Javalin,
 * configure les routes et démarre l'interface web.
 * </p>
 */
public final class Main {

    /** Code de statut HTTP pour "No Content". */
    private static final int HTTP_NO_CONTENT = 204;

    /** Port d'écoute du serveur. */
    private static final int DEFAULT_PORT = 8080;

    /** Logger pour le suivi des événements serveur. */
    private static final Logger LOGGER =
            Logger.getLogger(Main.class.getName());

    /** Constructeur privé pour empêcher l'instanciation. */
    private Main() { }

    /**
     * Point d'entrée principal de l'application.
     *
     * @param args les arguments de ligne de commande (non utilisés)
     */
    public static void main(final String[] args) {
        final Javalin app = Javalin.create(cfg -> {
            cfg.http.defaultContentType = "application/json";
            cfg.plugins.enableDevLogging();
            cfg.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/";
                staticFiles.directory = "/public";
                staticFiles.location = Location.CLASSPATH;
            });
        }).start(DEFAULT_PORT);

        // Définition des routes
        app.options("/api/preferences", ctx -> {
            CorsUtil.setCors(ctx);
            ctx.status(HTTP_NO_CONTENT);
        });

        app.post("/api/preferences",
                ctx -> PreferencesApi.handlePreferences(ctx));

        app.get("/health", ctx -> ctx.result("ok"));

        LOGGER.info(
                "API et interface web démarrées : http://localhost:"
                        + DEFAULT_PORT
        );
    }
}
