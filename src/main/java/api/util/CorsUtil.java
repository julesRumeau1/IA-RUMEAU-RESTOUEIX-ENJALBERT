package api.util;

import io.javalin.http.Context;

/**
 * Utilitaire pour la gestion des en-têtes CORS HTTP.
 */
public final class CorsUtil {

    private CorsUtil() {
        // Classe utilitaire, pas d'instance
    }

    /**
     * Applique les permissions relatives aux CORS.
     *
     * @param ctx contexte HTTP de la requête
     */
    public static void setCors(final Context ctx) {
        ctx.header("Access-Control-Allow-Origin", "*");
        ctx.header("Access-Control-Allow-Methods", "POST, OPTIONS");
        ctx.header("Access-Control-Allow-Headers", "Content-Type");
        ctx.header("Vary", "Origin");
    }
}
