import java.util.logging.Logger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.concurrent.CompletableFuture;

public class WebhookService {

    private static final Logger logger = Logger.getLogger(WebhookService.class.getName());
    private final WebhookDispatcher webhookDispatcher;
    private final Map<Object, Webhook> registeredWebhooks = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    // Retry config defaults
    private final long initialBackoffMillis;
    private final long maxBackoffMillis;
    private final int maxRetryAttempts;
    private final long idempotencyKeyTTL;

    public WebhookService() {
        this(1000, 30000, 3, 3600000); // default retry config and 1 hour TTL
    }

    public WebhookService(long initialBackoffMillis, long maxBackoffMillis, int maxRetryAttempts, long idempotencyKeyTTL) {
        this.initialBackoffMillis = initialBackoffMillis;
        this.maxBackoffMillis = maxBackoffMillis;
        this.maxRetryAttempts = maxRetryAttempts;
        this.idempotencyKeyTTL = idempotencyKeyTTL;

        WebhookDispatcher.BackoffStrategy backoffStrategy = new WebhookDispatcher.ExponentialBackoffWithJitter(initialBackoffMillis, maxBackoffMillis);

        // Define retry condition to retry only on specific exceptions
        Predicate<Exception> retryCondition = e -> {
            return e instanceof WebhookDispatcher.TimeoutException || e instanceof WebhookDispatcher.SpecificException;
        };

        this.webhookDispatcher = new WebhookDispatcher(maxRetryAttempts, idempotencyKeyTTL, backoffStrategy, retryCondition);
    }

    public void registerWebhook(Webhook webhook) {
        if (webhook == null) {
            logger.warning("Attempted to register a null webhook.");
            return;
        }
        registeredWebhooks.put(webhook.getId(), webhook);
        logger.info("Registered webhook with ID: " + webhook.getId());
    }

    // Centralized async dispatch with retries handled inside WebhookDispatcher
    public CompletableFuture<Void> dispatchWebhookAsync(Webhook webhook, Object event) {
        if (webhook == null) {
            logger.warning("Attempted to dispatch a null webhook.");
            return CompletableFuture.completedFuture(null);
        }
        if (!registeredWebhooks.containsKey(webhook.getId())) {
            logger.warning("Webhook with ID " + webhook.getId() + " is not registered.");
            return CompletableFuture.completedFuture(null);
        }

        logger.info("Asynchronously dispatching webhook ID: " + webhook.getId() + " with event: " + event);
        return CompletableFuture.runAsync(() -> {
            try {
                webhookDispatcher.dispatchWithRetry(webhook, event.toString());
                logger.info("Successfully dispatched webhook ID: " + webhook.getId());
            } catch (Exception e) {
                logger.severe("Failed to dispatch webhook ID: " + webhook.getId() + ". Error: " + e.getMessage());
            }
        }, executorService);
    }

    public void dispatchNewWebhook(NewWebhook newWebhook, String idempotencyKey) {
        if (newWebhook == null) {
            logger.warning("Attempted to dispatch a null newWebhook.");
            return;
        }
        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            logger.warning("Idempotency key is required to dispatch new webhook.");
            return;
        }
        logger.info("Asynchronously dispatching new webhook with idempotency key: " + idempotencyKey);
        executorService.submit(() -> {
            try {
                Webhook webhook = convertNewWebhookToWebhook(newWebhook);
                registerWebhook(webhook);
                webhookDispatcher.dispatchWithRetry(webhook, idempotencyKey);
            } catch (Exception e) {
                logger.severe("Failed to asynchronously dispatch new webhook. Error: " + e.getMessage());
            }
        });
    }

    private Webhook convertNewWebhookToWebhook(NewWebhook newWebhook) {
        Webhook webhook = new Webhook(newWebhook.getPayload());
        webhook.setId(newWebhook.getId());
        return webhook;
    }

}
