import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class RetryUtilityExample {

    private static final Logger logger = Logger.getLogger(RetryUtilityExample.class.getName());

    public static void main(String[] args) {
        RetryUtilityExample example = new RetryUtilityExample();
        example.runSynchronousExample();
        example.runAsynchronousExample();
    }

    /**
     * Demonstrates usage of RetryUtility for synchronous operations with retry on network errors.
     */
    public void runSynchronousExample() {
        RetryUtility retryUtility = new RetryUtility(
            500,    // initial backoff 500ms
            8000,   // max backoff 8000ms
            5,      // max retry attempts
            RetryUtility.RetryConditions.retryOnNetworkErrors()  // retry on network errors
        );

        retryUtility.setOnRetryAttempt(attempt -> logger.info("Sync Retry attempt: " + attempt));
        retryUtility.setOnRetryFailure(ex -> logger.severe("Sync Retry failed with exception: " + ex.toString()));

        try {
            String result = retryUtility.executeWithRetry(() -> {
                logger.info("Executing synchronous operation...");
                // Simulate operation that may fail
                if (Math.random() < 0.7) {
                    throw new java.io.IOException("Simulated network failure");
                }
                return "Success!";
            });
            logger.info("Synchronous operation result: " + result);
        } catch (Exception ex) {
            logger.severe("Synchronous operation failed after retries: " + ex.toString());
        }
    }

    /**
     * Demonstrates usage of RetryUtility for asynchronous operations with retry on HTTP 5xx errors
     * and a custom exponential backoff strategy.
     */
    public void runAsynchronousExample() {
        RetryUtility retryUtility = new RetryUtility(
            300,    // initial backoff 300ms
            5000,   // max backoff 5000ms
            4,      // max retry attempts
            RetryUtility.RetryConditions.retryOnHttp5xx()  // retry on HTTP 5xx errors
        );

        // Custom backoff strategy: fixed backoff of 1000ms
        retryUtility.setBackoffStrategy(attempt -> 1000);

        retryUtility.setOnRetryAttempt(attempt -> logger.info("Async Retry attempt: " + attempt));
        retryUtility.setOnRetryFailure(ex -> logger.severe("Async Retry failed with exception: " + ex.toString()));

        Supplier<CompletableFuture<String>> asyncOperation = () -> CompletableFuture.supplyAsync(() -> {
            logger.info("Executing asynchronous operation...");
            // Simulate operation that may fail with HTTP 5xx
            if (Math.random() < 0.6) {
                throw new RetryUtility.HttpStatusException(503, "Simulated HTTP 503 Service Unavailable");
            }
            return "Async Success!";
        });

        CompletableFuture<String> futureResult = retryUtility.executeWithRetryAsync(asyncOperation);

        try {
            String result = futureResult.get();
            logger.info("Asynchronous operation result: " + result);
        } catch (InterruptedException | ExecutionException ex) {
            logger.severe("Asynchronous operation failed after retries: " + ex.getCause().toString());
        }
    }

}
