// Adjusted backoff time based on error severity and partner rules
function calculateBackoff(errorSeverity) {
    let backoffTime;
    switch (errorSeverity) {
        case 'critical':
            backoffTime = 1000; // 1 second
            break;
        case 'high':
            backoffTime = 5000; // 5 seconds
            break;
        case 'medium':
            backoffTime = 10000; // 10 seconds
            break;
        case 'low':
            backoffTime = 30000; // 30 seconds
            break;
        default:
            backoffTime = 60000; // 1 minute
    }
    return backoffTime;
}