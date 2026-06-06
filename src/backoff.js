module.exports = {
  backoff: (attempt) => {
    const maxRetries = 5;
    const maxDelay = 30000; // 30 seconds
    const delay = Math.min(maxDelay, Math.pow(2, attempt) * 1000);
    return delay;
  }
};