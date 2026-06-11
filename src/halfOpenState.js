// Half-open state logic and metrics implementation

class HalfOpenState {
    constructor() {
        this.state = 'CLOSED';
        this.metrics = {};
    }

    open() {
        this.state = 'OPEN';
        this.recordMetric('opened');
    }

    close() {
        this.state = 'CLOSED';
        this.recordMetric('closed');
    }

    halfOpen() {
        this.state = 'HALF_OPEN';
        this.recordMetric('half_opened');
    }

    recordMetric(action) {
        this.metrics[action] = (this.metrics[action] || 0) + 1;
    }

    getMetrics() {
        return this.metrics;
    }
}

module.exports = HalfOpenState;