import java.util.Random;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Interface for backoff strategy.
 */
interface BackoffStrategy {
    long computeBackoffMillis(int attempt);
}

/**
 * Utility class to perform retry operations with configurable backoff strategy and enhanced exception handling.
 * Added timeout support for each operation attempt.
 */
public class RetryUtility {

    private static final Logger logger = Logger.getLogger(RetryUtility.class.getName());

    private volatile long initialBackoffMillis = 500; // Updated default initial backoff
    private volatile long maxBackoffMillis = 8000; // Updated default max backoff
    private volatile int maxRetryAttempts = 5; // Updated default max retry attempts
    private volatile long timeoutMillis = 3000; // Default timeout for each attempt
    private final Random random;
    private final Predicate<Exception> retryCondition;
    private BackoffStrategy backoffStrategy;
    private final ExecutorService executorService;

    /**
     * Constructor for RetryUtility with default exponential backoff strategy with jitter.
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
        this.backoffStrategy = this::defaultBackoffStrategy;
        this.executorService = Executors.newCachedThreadPool();
    }

    /**
     * Set a custom backoff strategy.
     * 
     * @param strategy new backoff strategy
     */
    public void setBackoffStrategy(BackoffStrategy strategy) {
        if (strategy != null) {
            this.backoffStrategy = strategy;
        }
    }

    /**
     * Update retry configuration at runtime.
     */
    public void updateRetryConfig(long initialBackoffMillis, long maxBackoffMillis, int maxRetryAttempts) {
        this.initialBackoffMillis = initialBackoffMillis;
        this.maxBackoffMillis = maxBackoffMillis;
        this.maxRetryAttempts = maxRetryAttempts;
    }

    /**
     * Set timeout duration in milliseconds for each operation attempt.
     */
    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * Executes the given operation with retry logic and timeout for each attempt.
     * 
     * @param <T> the return type of the operation
     * @param operation the operation to execute
     * @return the result of the operation
     * @throws Exception if all retries fail or timeout occurs
     */
    public <T> T executeWithRetry(Supplier<T> operation) throws Exception {
        for (int attempt = 1; attempt <= maxRetryAttempts; attempt++) {
            try {
                CompletableFuture<T> future = CompletableFuture.supplyAsync(operation, executorService);
                return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
            } catch (TimeoutException te) {
                logger.warning("Attempt " + attempt + " timed out after " + timeoutMillis + " ms.");
                if (!retryCondition.test(te)) {
                    throw te;
                }
            } catch (Exception e) {
                if (!retryCondition.test(e)) {
                    logger.warning("Exception not retryable: " + e.toString());
                    throw e; // Non-retryable exception, propagate
                }
                logger.warning("Attempt " + attempt + " failed with exception: " + e.toString());
                logger.fine(() -> {
                    StringBuilder sb = new StringBuilder();
                    for (StackTraceElement el : e.getStackTrace()) {
                        sb.append(el.toString()).append("\n");
                    }
                    return sb.toString();
                });
            }
            if (attempt == maxRetryAttempts) {
                logger.severe("Max retry attempts reached. Giving up.");
                throw new Exception("Max retry attempts reached.");
            }
            long backoffMillis = backoffStrategy.computeBackoffMillis(attempt);
            logger.info("Backing off for " + backoffMillis + " ms before next retry.");
            try {
                TimeUnit.MILLISECONDS.sleep(backoffMillis);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                logger.severe("Thread interrupted during backoff. Aborting retries.");
                throw ie;
            }
        }
        throw new IllegalStateException("Unreachable code reached in retry logic");
    }

    private long defaultBackoffStrategy(int attempt) {
        long expBackoff = initialBackoffMillis * (1L << (attempt - 1));
        long cappedBackoff = Math.min(expBackoff, maxBackoffMillis);
        return (long) (random.nextDouble() * cappedBackoff);
    }

}
