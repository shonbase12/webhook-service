import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.concurrent.CompletionException;

public class WebhookDispatcher {
    private static final Logger logger = Logger.getLogger(WebhookDispatcher.class.getName());

    private final RetryUtility retryUtility;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Map<String, String> idempotencyStore = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> evictionTasks = new ConcurrentHashMap<>();
    private final AtomicBoolean circuitOpen = new AtomicBoolean(false);
    private final int circuitBreakerThreshold = 5;
    private int failureCount = 0;
    private final long idempotencyKeyTTL;

    private static final long TIMEOUT = 5000; // 5 seconds timeout for sending webhook

    public WebhookDispatcher(RetryUtility retryUtility, long idempotencyKeyTTL) {
        this.retryUtility = retryUtility;
        this.idempotencyKeyTTL = idempotencyKeyTTL;
        this.retryUtility.setOnRetryAttempt(attempt -> logger.info("Retry attempt #" + attempt));
        this.retryUtility.setOnRetryFailure(e -> {
            logger.severe("Retry failed with exception: " + e.getMessage());
            failureCount++;
            if (failureCount >= circuitBreakerThreshold) {
                openCircuitBreaker();
                logger.severe("Circuit breaker opened due to repeated failures.");
            }
        });
    }

    public void dispatchWebhook(Webhook webhook, String idempotencyKey) {
        if (!validateIdempotencyKey(idempotencyKey)) {
            logger.warning("Invalid idempotency key. Aborting dispatch.");
            return;
        }
        if (idempotencyStore.containsKey(idempotencyKey)) {
            logger.info("Webhook with idempotency key " + idempotencyKey + " has already been processed.");
            return;
        }
        if (circuitOpen.get()) {
            logger.warning("Circuit breaker is open. Aborting dispatch.");
            return;
        }

        retryUtility.executeWithRetryAsync(() -> CompletableFuture.supplyAsync(() -> {
            try {
                sendWebhookWithTimeout(webhook);
                return null;
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        })).whenComplete((result, throwable) -> {
            if (throwable == null) {
                logger.info("Webhook dispatched successfully.");
                idempotencyStore.put(idempotencyKey, "sent");
                scheduleEviction(idempotencyKey);
                resetCircuitBreaker();
            } else {
                logger.severe("Failed to dispatch webhook after retries: " + throwable.getCause().getMessage());
                // Implement fallback or dead-letter queue here if needed
            }
        });
    }

    public void dispatchUpdateWebhook(Webhook webhook, String idempotencyKey) {
        if (!validateIdempotencyKey(idempotencyKey)) {
            logger.warning("Invalid idempotency key. Aborting update dispatch.");
            return;
        }
        if (circuitOpen.get()) {
            logger.warning("Circuit breaker is open. Aborting update dispatch.");
            return;
        }

        retryUtility.executeWithRetryAsync(() -> CompletableFuture.supplyAsync(() -> {
            try {
                sendWebhookWithTimeout(webhook);
                return null;
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        })).whenComplete((result, throwable) -> {
            if (throwable == null) {
                logger.info("Webhook updated successfully.");
                idempotencyStore.put(idempotencyKey, "updated");
                scheduleEviction(idempotencyKey);
                resetCircuitBreaker();
            } else {
                logger.severe("Failed to update webhook after retries: " + throwable.getCause().getMessage());
                // Implement fallback or dead-letter queue here if needed
            }
        });
    }

    private void scheduleEviction(String idempotencyKey) {
        ScheduledFuture<?> previousTask = evictionTasks.put(idempotencyKey, scheduler.schedule(() -> {
            idempotencyStore.remove(idempotencyKey);
            evictionTasks.remove(idempotencyKey);
            logger.info("Evicted idempotency key from store: " + idempotencyKey);
        }, idempotencyKeyTTL, TimeUnit.MILLISECONDS));

        if (previousTask != null) {
            previousTask.cancel(false);
        }
    }

    private void sendWebhookWithTimeout(Webhook webhook) throws Exception {
        long startTime = System.currentTimeMillis();
        boolean success = sendToWebhookEndpoint(webhook);
        long elapsedTime = System.currentTimeMillis() - startTime;

        if (!success || elapsedTime > TIMEOUT) {
            throw new WebhookDispatcher.TimeoutException("Failed to send webhook or timeout exceeded.");
        }
    }

    private boolean validateIdempotencyKey(String key) {
        return key != null && !key.trim().isEmpty() && key.length() <= 255;
    }

    private void openCircuitBreaker() {
        circuitOpen.set(true);
        scheduler.schedule(this::resetCircuitBreaker, 1, TimeUnit.MINUTES);
    }

    private void resetCircuitBreaker() {
        circuitOpen.set(false);
        failureCount = 0;
        logger.info("Circuit breaker reset.");
    }

    // Exception classes
    public static class TimeoutException extends Exception {
        public TimeoutException(String message) {
            super(message);
        }
    }
}
