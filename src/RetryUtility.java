import java.util.Random;
import java.util.concurrent.*;
import java.util.function.Consumer;
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
     * Common retry conditions for typical transient exceptions
     */
    public static class RetryConditions {
        public static Predicate<Exception> alwaysRetry() {
            return e -> true;
        }

        public static Predicate<Exception> retryOnNetworkErrors() {
            return e -> e instanceof java.io.IOException || e instanceof java.net.SocketTimeoutException;
        }

        public static Predicate<Exception> retryOnHttp5xx() {
            return e -> {
                if (e instanceof HttpStatusException) {
                    int statusCode = ((HttpStatusException) e).getStatusCode();
                    return statusCode >= 500 && statusCode < 600;
                }
                return false;
            };
        }
    }

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
     * Set optional max elapsed time for retries. Negative value disables.
     * @param maxElapsedTimeMillis max elapsed time in milliseconds
     */
    public void setMaxElapsedTimeMillis(long maxElapsedTimeMillis) {
        this.maxElapsedTimeMillis = maxElapsedTimeMillis;
    }

    /**
     * Set callback for retry attempt metric.
     * @param onRetryAttempt consumer accepting current attempt number
     */
    public void setOnRetryAttempt(Consumer<Integer> onRetryAttempt) {
        this.onRetryAttempt = onRetryAttempt;
    }

    /**
     * Set callback for retry failure metric.
     * @param onRetryFailure consumer accepting the exception causing failure
     */
    public void setOnRetryFailure(Consumer<Exception> onRetryFailure) {
        this.onRetryFailure = onRetryFailure;
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
                logger.fine(() -> {
                    StringBuilder sb = new StringBuilder();
                    for (StackTraceElement el : e.getStackTrace()) {
                        sb.append(el.toString()).append("\n");
                    }
                    return sb.toString();
                });
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

    /**
     * Executes the given asynchronous operation with retry logic.
     * 
     * @param <T> the return type of the operation
     * @param asyncOperation the async operation that returns CompletableFuture
     * @return CompletableFuture with the result of the operation
     */
    public <T> CompletableFuture<T> executeWithRetryAsync(Supplier<CompletableFuture<T>> asyncOperation) {
        CompletableFuture<T> resultFuture = new CompletableFuture<>();
        long startTime = System.currentTimeMillis();
        executeAsyncHelper(asyncOperation, 1, resultFuture, startTime);
        return resultFuture;
    }

    private <T> void executeAsyncHelper(Supplier<CompletableFuture<T>> asyncOperation, int attempt, CompletableFuture<T> resultFuture, long startTime) {
        asyncOperation.get().whenComplete((result, throwable) -> {
            if (throwable == null) {
                resultFuture.complete(result);
            } else {
                Throwable cause = throwable instanceof java.util.concurrent.CompletionException ? throwable.getCause() : throwable;
                if (!(cause instanceof Exception) || !retryCondition.test((Exception) cause)) {
                    logger.warning("Exception not retryable: " + cause.toString());
                    resultFuture.completeExceptionally(cause);
                    return;
                }
                long elapsedTime = System.currentTimeMillis() - startTime;
                if (maxElapsedTimeMillis >= 0 && elapsedTime > maxElapsedTimeMillis) {
                    logger.severe("Max elapsed retry time exceeded. Giving up.");
                    if (onRetryFailure != null) {
                        onRetryFailure.accept((Exception) cause);
                    }
                    resultFuture.completeExceptionally(cause);
                    return;
                }
                logger.warning("Attempt " + attempt + " failed with exception: " + cause.toString());
                logger.fine(() -> {
                    StringBuilder sb = new StringBuilder();
                    for (StackTraceElement el : cause.getStackTrace()) {
                        sb.append(el.toString()).append("\n");
                    }
                    return sb.toString();
                });
                if (attempt == maxRetryAttempts) {
                    logger.severe("Max retry attempts reached. Giving up.");
                    if (onRetryFailure != null) {
                        onRetryFailure.accept((Exception) cause);
                    }
                    resultFuture.completeExceptionally(cause);
                    return;
                }
                if (onRetryAttempt != null) {
                    onRetryAttempt.accept(attempt);
                }
                long backoffMillis = backoffStrategy.computeBackoffMillis(attempt);
                logger.info("Backing off for " + backoffMillis + " ms before next retry.");
                scheduler.schedule(() -> executeAsyncHelper(asyncOperation, attempt + 1, resultFuture, startTime), backoffMillis, TimeUnit.MILLISECONDS);
            }
        });
    }

    private long defaultBackoffStrategy(int attempt) {
        long expBackoff = initialBackoffMillis * (1L << (attempt - 1));
        long cappedBackoff = Math.min(expBackoff, maxBackoffMillis);
        return (long) (random.nextDouble() * cappedBackoff);
    }

    // Hypothetical HttpStatusException class to demonstrate retry condition on HTTP 5xx
    public static class HttpStatusException extends Exception {
        private final int statusCode;

        public HttpStatusException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }

}
