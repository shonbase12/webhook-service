// Implementing half-open state logic and metrics tracking for circuit breaker

class CircuitBreaker {
    constructor() {
        this.state = 'CLOSED';
        this.failureCount = 0;
        this.successCount = 0;
        this.metrics = [];
    }

    // Method to simulate failure
    fail() {
        this.failureCount++;
        this.metrics.push({ state: this.state, success: false });
        if (this.state === 'CLOSED') {
            this.state = 'OPEN';
        }
    }

    // Method to simulate success
    succeed() {
        this.successCount++;
        this.metrics.push({ state: this.state, success: true });
        if (this.state === 'OPEN') {
            this.state = 'HALF-OPEN';
        }
    }

    // Method to reset state
    reset() {
        this.state = 'CLOSED';
        this.failureCount = 0;
        this.successCount = 0;
        this.metrics = [];
    }

    // Method to get metrics
    getMetrics() {
        return this.metrics;
    }
}