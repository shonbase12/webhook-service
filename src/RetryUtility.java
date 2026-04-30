import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Utility class to perform retry operations with configurable exponential backoff and jitter.
 */
public class RetryUtility {

    private static final Logger logger = Logger.getLogger(RetryUtility.class.getName());

    private final long initialBackoffMillis;
    private final long maxBackoffMillis;
    private final int maxRetryAttempts;
    private final Random random;
    private final Predicate<Exception> retryCondition;

    /**
     * Constructor for RetryUtility.
     * 
     * @param initialBackoffMillis initial backoff in milliseconds
     * @param maxBackoffMillis maximum backoff in milliseconds
     * @param maxRetryAttempts maximum retry attempts
     * @param retryCondition predicate to determine if an exception should trigger retry
     */
    public RetryUtility(long initialBackoffMillis, long maxBackoffMillis, int maxRetryAttempts, Predicate<Exception> retryCondition) {
        this.initialBackoffMillis = initialBackoffMillis;
        this.maxBackoffMillis = maxBackoffMillis;
        this.maxRetryAttempts = maxRetryAttempts;
        this.random = new Random();
        this.retryCondition = retryCondition;
    }

    /**
     * Executes the given operation with retry logic.
     * 
     * @param <T> the return type of the operation
     * @param operation the operation to execute
     * @return the result of the operation
     * @throws Exception if all retries fail
     */
    public <T> T executeWithRetry(Supplier<T> operation) throws Exception {
        for (int attempt = 1; attempt <= maxRetryAttempts; attempt++) {
            try {
                return operation.get();
            } catch (Exception e) {
                if (!retryCondition.test(e)) {
                    logger.warning("Exception not retryable: " + e.getMessage());
                    throw e; // Non-retryable exception, propagate
                }
                logger.warning("Attempt " + attempt + " failed with exception: " + e.getMessage());
                if (attempt == maxRetryAttempts) {
                    logger.severe("Max retry attempts reached. Giving up.");
                    throw e;
                }
                long backoffMillis = calculateBackoffWithJitter(attempt);
                logger.info("Backing off for " + backoffMillis + " ms before next retry.");
                try {
                    TimeUnit.MILLISECONDS.sleep(backoffMillis);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.severe("Thread interrupted during backoff. Aborting retries.");
                    throw ie;
                }
            }
        }
        throw new IllegalStateException("Unreachable code reached in retry logic");
    }

    private long calculateBackoffWithJitter(int attempt) {
        long expBackoff = initialBackoffMillis * (1L << (attempt - 1));
        long cappedBackoff = Math.min(expBackoff, maxBackoffMillis);
        return (long) (random.nextDouble() * cappedBackoff);
    }

}
