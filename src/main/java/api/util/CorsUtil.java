package api.util;

import io.javalin.http.Context;

/**
 * Aide pour les CORS HTTP
 */
public final class CorsUtil {

    private CorsUtil() {
    }

    /**
     * Applique les permissions relatives aux CORS
     * @param ctx context HTTP
     */
    public static void setCors(Context ctx) {
        ctx.header("Access-Control-Allow-Origin", "*");
        ctx.header("Access-Control-Allow-Methods", "POST, OPTIONS");
        ctx.header("Access-Control-Allow-Headers", "Content-Type");
        ctx.header("Vary", "Origin");
    }
}
