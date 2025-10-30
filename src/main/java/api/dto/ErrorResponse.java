package api.dto;

/**
 * Représente une réponse d'erreur envoyée par l'API.
 */
public class ErrorResponse {

    /** Code d'erreur. */
    private String code;

    /** Message d'erreur. */
    private String message;

    /**
     * Crée une nouvelle réponse d'erreur.
     *
     * @param errorCode code d'erreur
     * @param errorMessage message descriptif de l'erreur
     */
    public ErrorResponse(final String errorCode, final String errorMessage) {
        this.code = errorCode;
        this.message = errorMessage;
    }

    /**
     * Retourne le code d'erreur.
     *
     * @return le code d'erreur
     */
    public String getCode() {
        return code;
    }

    /**
     * Retourne le message d'erreur.
     *
     * @return le message d'erreur
     */
    public String getMessage() {
        return message;
    }
}
