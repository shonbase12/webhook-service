// Improved Circuit Breaker implementation with Half-Open State and Detailed Metrics

public class CircuitBreaker {
    private enum State { CLOSED, OPEN, HALF_OPEN }
    private State state;
    private int failureCount;
    private int successCount;
    private long lastFailureTime;
    private long timeout;

    public CircuitBreaker(long timeout) {
        this.state = State.CLOSED;
        this.timeout = timeout;
        this.failureCount = 0;
        this.successCount = 0;
    }

    public void call() {
        switch (state) {
            case CLOSED:
                // Execute call and handle success/failure
                break;
            case OPEN:
                // Reject call
                break;
            case HALF_OPEN:
                // Allow limited calls
                break;
        }
    }

    private void onSuccess() {
        successCount++;
        if (state == State.HALF_OPEN) {
            state = State.CLOSED;
        }
    }

    private void onFailure() {
        failureCount++;
        lastFailureTime = System.currentTimeMillis();
        if (failureCount >= threshold) {
            state = State.OPEN;
        }
    }

    public String getMetrics() {
        return "Failures: " + failureCount + ", Successes: " + successCount;
    }
}