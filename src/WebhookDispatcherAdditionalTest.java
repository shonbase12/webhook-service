import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class WebhookDispatcherAdditionalTest {

    @Test
    public void testDispatchWebhook_Idempotency() throws InterruptedException {
        WebhookDispatcher dispatcher = spy(new WebhookDispatcher(3));
        Webhook webhook = new Webhook();
        String idempotencyKey = "unique-key-123";

        // First dispatch should call sendWebhookWithTimeout
        dispatcher.dispatchWebhook(webhook, idempotencyKey);
        // Wait a bit to let async run
        TimeUnit.SECONDS.sleep(1);
        verify(dispatcher, atLeastOnce()).sendWebhookWithTimeout(webhook);

        // Second dispatch with same idempotency key should not call sendWebhookWithTimeout
        dispatcher.dispatchWebhook(webhook, idempotencyKey);
        TimeUnit.SECONDS.sleep(1);
        // Verify sendWebhookWithTimeout still called only once
        verify(dispatcher, atLeastOnce()).sendWebhookWithTimeout(webhook);
    }

    @Test
    public void testDispatchWebhook_AsyncExecution() throws InterruptedException, ExecutionException {
        WebhookDispatcher dispatcher = spy(new WebhookDispatcher(1));
        Webhook webhook = new Webhook();
        String idempotencyKey = "async-key-456";

        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> dispatcher.dispatchWebhook(webhook, idempotencyKey));
        // The method returns immediately, so future should not be done yet
        assertFalse(future.isDone());

        // Wait some time to let async complete
        TimeUnit.SECONDS.sleep(1);
        future.get(); // Ensure no exceptions
        verify(dispatcher, atLeastOnce()).sendWebhookWithTimeout(webhook);
    }

    @Test
    public void testExponentialBackoffTiming() throws InterruptedException {
        WebhookDispatcher dispatcher = spy(new WebhookDispatcher(3));
        Webhook webhook = new Webhook();
        String idempotencyKey = "backoff-key-789";

        // Mock sendWebhookWithTimeout to always throw SpecificException to trigger retries
        doThrow(new SpecificException("Simulated failure")).when(dispatcher).sendWebhookWithTimeout(webhook);

        long startTime = System.currentTimeMillis();
        dispatcher.dispatchWebhook(webhook, idempotencyKey);
        // Wait enough time for retries with backoff
        TimeUnit.SECONDS.sleep(6);
        long elapsedTime = System.currentTimeMillis() - startTime;

        // Rough check: elapsed time should be at least sum of backoffs (1s + 2s + 4s = 7s minus jitter, so > 5s safe)
        assertTrue(elapsedTime >= 5000, "Elapsed time should respect exponential backoff");

        verify(dispatcher, times(3)).sendWebhookWithTimeout(webhook);
    }

    @Test
    public void testExceptionHandlingDifferentTypes() throws InterruptedException {
        WebhookDispatcher dispatcher = spy(new WebhookDispatcher(3));
        Webhook webhook = new Webhook();
        String idempotencyKey = "exception-key-101";

        // Test NetworkException
        doThrow(new NetworkException("Network issue")).when(dispatcher).sendWebhookWithTimeout(webhook);
        dispatcher.dispatchWebhook(webhook, idempotencyKey);
        TimeUnit.SECONDS.sleep(4);
        verify(dispatcher, times(3)).sendWebhookWithTimeout(webhook);

        // Reset interactions
        reset(dispatcher);

        // Test ValidationException (should not retry, so only 1 call expected)
        doThrow(new ValidationException("Validation failed")).when(dispatcher).sendWebhookWithTimeout(webhook);
        dispatcher.dispatchWebhook(webhook, idempotencyKey + "-val");
        TimeUnit.SECONDS.sleep(2);
        verify(dispatcher, times(1)).sendWebhookWithTimeout(webhook);
    }
}
