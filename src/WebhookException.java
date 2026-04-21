public class WebhookException extends Exception {
    public WebhookException(String message) {
        super(message);
    }
}

public class NetworkException extends WebhookException {
    public NetworkException(String message) {
        super(message);
    }
}

public class ValidationException extends WebhookException {
    public ValidationException(String message) {
        super(message);
    }
}

// Add more specific exceptions as needed
