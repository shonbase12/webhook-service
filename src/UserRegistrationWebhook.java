import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class UserRegistrationWebhook {
    private static final Logger logger = Logger.getLogger(UserRegistrationWebhook.class.getName());
    private WebhookDispatcher webhookDispatcher;

    public UserRegistrationWebhook(WebhookDispatcher webhookDispatcher) {
        this.webhookDispatcher = webhookDispatcher;
    }

    public void handleRegistrationEvent(User user) {
        String idempotencyKey = user.getId(); // Use user ID as idempotency key
        Webhook webhook = createWebhookForUserRegistration(user);
        webhookDispatcher.dispatchWebhook(webhook, idempotencyKey);
    }

    private Webhook createWebhookForUserRegistration(User user) {
        // Create and return a webhook object for the user registration event.
        // This is a placeholder implementation. You should replace it with actual logic to build the webhook.
        return new Webhook("http://example.com/user/registration", user.toString());
    }
}