package main;

import api.PreferencesApi;
import api.util.CorsUtil;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import java.util.logging.Logger;

public final class Main {

    private Main() {}

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(final String[] args) {
        final int port = 8080;

        Javalin app = Javalin.create(cfg -> {
            cfg.http.defaultContentType = "application/json";
            cfg.plugins.enableDevLogging();
            cfg.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/";
                staticFiles.directory = "/public";
                staticFiles.location = Location.CLASSPATH;
            });
        }).start(port);

        // Définition des routes
        app.options("/api/preferences", ctx -> {
            CorsUtil.setCors(ctx);
            ctx.status(204);
        });

        app.post("/api/preferences", ctx -> PreferencesApi.handlePreferences(ctx));

        app.get("/health", ctx -> ctx.result("ok"));

        LOGGER.info("API et Interface web démarrées: http://localhost:" + port);
    }
}
