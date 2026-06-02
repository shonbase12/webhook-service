public class WebhookTimeoutException extends Exception {
    public WebhookTimeoutException(String message) {
        super(message);
    }

    public WebhookTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
