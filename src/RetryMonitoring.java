import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RetryMonitoring {

    private static final Logger logger = Logger.getLogger(RetryMonitoring.class.getName());

    private final AtomicInteger retryFailures = new AtomicInteger(0);
    private final AtomicInteger circuitBreakerEvents = new AtomicInteger(0);
    private final AtomicInteger idempotencyKeyStoreHealthIssues = new AtomicInteger(0);

    public void recordRetryFailure() {
        int count = retryFailures.incrementAndGet();
        logger.log(Level.WARNING, "Retry failure count incremented to: " + count);
        checkAlerting(count, "Retry Failures");
    }

    public void recordCircuitBreakerEvent() {
        int count = circuitBreakerEvents.incrementAndGet();
        logger.log(Level.WARNING, "Circuit breaker event count incremented to: " + count);
        checkAlerting(count, "Circuit Breaker Events");
    }

    public void recordIdempotencyKeyStoreHealthIssue() {
        int count = idempotencyKeyStoreHealthIssues.incrementAndGet();
        logger.log(Level.WARNING, "Idempotency key store health issue count incremented to: " + count);
        checkAlerting(count, "Idempotency Key Store Health Issues");
    }

    private void checkAlerting(int count, String alertType) {
        // Basic alerting threshold example
        if (count > 10) {
            logger.log(Level.SEVERE, "ALERT: High number of " + alertType + ": " + count);
            // Here, you could integrate with real alerting systems like email, SMS, or monitoring dashboards
        }
    }

    public int getRetryFailures() {
        return retryFailures.get();
    }

    public int getCircuitBreakerEvents() {
        return circuitBreakerEvents.get();
    }

    public int getIdempotencyKeyStoreHealthIssues() {
        return idempotencyKeyStoreHealthIssues.get();
    }
}
