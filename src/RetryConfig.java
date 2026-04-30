import java.util.function.Predicate;

/**
 * Centralized configuration for retry policies.
 */
public class RetryConfig {

    private long initialBackoffMillis;
    private long maxBackoffMillis;
    private int maxRetryAttempts;
    private Predicate<Exception> retryCondition;

    public RetryConfig(long initialBackoffMillis, long maxBackoffMillis, int maxRetryAttempts, Predicate<Exception> retryCondition) {
        this.initialBackoffMillis = initialBackoffMillis;
        this.maxBackoffMillis = maxBackoffMillis;
        this.maxRetryAttempts = maxRetryAttempts;
        this.retryCondition = retryCondition;
    }

    public long getInitialBackoffMillis() {
        return initialBackoffMillis;
    }

    public void setInitialBackoffMillis(long initialBackoffMillis) {
        this.initialBackoffMillis = initialBackoffMillis;
    }

    public long getMaxBackoffMillis() {
        return maxBackoffMillis;
    }

    public void setMaxBackoffMillis(long maxBackoffMillis) {
        this.maxBackoffMillis = maxBackoffMillis;
    }

    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public void setMaxRetryAttempts(int maxRetryAttempts) {
        this.maxRetryAttempts = maxRetryAttempts;
    }

    public Predicate<Exception> getRetryCondition() {
        return retryCondition;
    }

    public void setRetryCondition(Predicate<Exception> retryCondition) {
        this.retryCondition = retryCondition;
    }

}