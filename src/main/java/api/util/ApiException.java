package api.util;

/**
 * Exception personnalisée utilisée pour les erreurs de l'API.
 * <p>
 * Cette exception contient un code d'erreur applicatif en plus du message.
 * Elle est {@code final} afin d'éviter toute extension non contrôlée.
 * </p>
 */
public final class ApiException extends RuntimeException {

    /** Code d'erreur associé à cette exception. */
    private final String code;

    /**
     * Crée une nouvelle exception API avec un code et un message.
     *
     * @param errorCode le code d'erreur unique associé à l'exception
     * @param message le message décrivant l'erreur
     */
    public ApiException(final String errorCode, final String message) {
        super(message);
        this.code = errorCode;
    }

    /**
     * Retourne le code d'erreur associé à cette exception.
     *
     * @return le code d'erreur sous forme de chaîne
     */
    public String getCode() {
        return code;
    }
}
