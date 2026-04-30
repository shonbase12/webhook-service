# Webhook Service

## Overview
This service handles webhook dispatching with optimizations to reduce latency and improve performance.

## Retry Policy
- Retries are managed using an exponential backoff strategy with jitter to avoid retry storms.
- Retry parameters such as initial backoff, max backoff, and max retry attempts are configurable.
- Retry logic retries only on specific exceptions like timeout and transient errors.
- RetryUtility class provides a generic retry mechanism with pluggable backoff strategies.

## Idempotency Key Handling
- WebhookDispatcher maintains an in-memory store of processed idempotency keys to prevent duplicate processing.
- Idempotency keys have a configurable TTL (default 1 hour) after which they are evicted.
- Eviction tasks are scheduled to remove idempotency keys after TTL to prevent memory leaks.
- Idempotency key validation ensures keys are non-null, non-empty, and within length limits.
- Circuit breaker mechanism prevents dispatch attempts in case of repeated failures.
- Persistent or distributed storage is recommended for production to maintain idempotency across restarts.

## Configuration
- Retry and idempotency TTL parameters can be configured in the WebhookService constructor.

## Usage Examples
- Use `dispatchNewWebhook(newWebhook, idempotencyKey)` to dispatch with idempotency.
- Use `retryWebhook(webhook, event)` to perform retries with configured policies.

## Troubleshooting Tips
- Monitor logs for retry attempts and failures.
- Adjust retry parameters and TTL based on performance and error patterns.
- Implement fallback or alerting for exhausted retries.

## Contributing Guidelines
- Follow the standard contribution process for any changes made to this repository.
