import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Utility class to perform retry operations with configurable backoff strategy and enhanced exception handling.
 * Supports both synchronous and asynchronous retry operations.
 */
public class RetryUtility {

    private static final Logger logger = Logger.getLogger(RetryUtility.class.getName());

    private volatile long initialBackoffMillis = 500; // Updated default initial backoff
    private volatile long maxBackoffMillis = 8000; // Updated default max backoff
    private volatile int maxRetryAttempts = 5; // Updated default max retry attempts
    private volatile long maxElapsedTimeMillis = -1; // Optional max elapsed time limit, -1 means unlimited
    private final Random random;
    private final Predicate<Exception> retryCondition;
    private BackoffStrategy backoffStrategy;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // Callbacks for metrics and observability
    private Consumer<Integer> onRetryAttempt;
    private Consumer<Exception> onRetryFailure;

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
    }

    /**
     * Executes the given synchronous operation with retry logic.
     * 
     * @param <T> the return type of the operation
     * @param operation the operation to execute
     * @return the result of the operation
     * @throws Exception if all retries fail
     */
    public <T> T executeWithRetry(Supplier<T> operation) throws Exception {
        long startTime = System.currentTimeMillis();
        for (int attempt = 1; attempt <= maxRetryAttempts; attempt++) {
            try {
                return operation.get();
            } catch (Exception e) {
                if (!retryCondition.test(e)) {
                    logger.warning("Exception not retryable: " + e.toString());
                    throw e; // Non-retryable exception, propagate
                }
                long elapsedTime = System.currentTimeMillis() - startTime;
                if (maxElapsedTimeMillis >= 0 && elapsedTime > maxElapsedTimeMillis) {
                    logger.severe("Max elapsed retry time exceeded. Giving up.");
                    if (onRetryFailure != null) {
                        onRetryFailure.accept(e);
                    }
                    throw e;
                }
                logger.warning("Attempt " + attempt + " failed with exception: " + e.toString());
                if (attempt == maxRetryAttempts) {
                    logger.severe("Max retry attempts reached. Giving up.");
                    if (onRetryFailure != null) {
                        onRetryFailure.accept(e);
                    }
                    throw e;
                }
                if (onRetryAttempt != null) {
                    onRetryAttempt.accept(attempt);
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
        }
        throw new IllegalStateException("Unreachable code reached in retry logic");
    }

    private long defaultBackoffStrategy(int attempt) {
        long expBackoff = initialBackoffMillis * (1L << (attempt - 1));
        long cappedBackoff = Math.min(expBackoff, maxBackoffMillis);
        return (long) (random.nextDouble() * cappedBackoff);
    }
}