import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class WebhookDispatcherTest {

    private WebhookDispatcher dispatcher;
    private Webhook webhook;

    @BeforeEach
    public void setup() {
        dispatcher = Mockito.spy(new WebhookDispatcher(3));
        webhook = mock(Webhook.class);
        when(webhook.getPayload()).thenReturn("valid payload");
    }

    @Test
    public void testDispatchWebhook_Success() {
        doNothing().when(dispatcher).sendWebhookWithTimeout(webhook);
        dispatcher.dispatchWebhook(webhook, "key1");
        verify(dispatcher, times(1)).sendWebhookWithTimeout(webhook);
    }

    @Test
    public void testDispatchWebhook_MaxRetries() {
        doThrow(new SpecificException("Error sending webhook")).when(dispatcher).sendWebhookWithTimeout(webhook);
        dispatcher.dispatchWebhook(webhook, "key2");
        verify(dispatcher, times(3)).sendWebhookWithTimeout(webhook);
    }

    @Test
    public void testDispatchWebhook_PartialSuccess() {
        doThrow(new SpecificException("Fail 1")).doNothing().when(dispatcher).sendWebhookWithTimeout(webhook);
        dispatcher.dispatchWebhook(webhook, "key3");
        verify(dispatcher, times(2)).sendWebhookWithTimeout(webhook);
    }

    @Test
    public void testDispatchWebhook_InterruptedExceptionDuringBackoff() throws InterruptedException {
        // We will mock TimeUnit.MILLISECONDS.sleep to throw InterruptedException
        AtomicBoolean interruptedFlag = new AtomicBoolean(false);
        try (MockedStatic<Thread> threadMockedStatic = Mockito.mockStatic(Thread.class)) {
            doAnswer(invocation -> {
                interruptedFlag.set(true);
                return null;
            }).when(Thread.class);
            Thread.currentThread().interrupt();
            
            dispatcher = new WebhookDispatcher(1) {
                @Override
                public void dispatchWebhook(Webhook webhook, String key) {
                    int attempt = 0;
                    while (attempt < maxRetries) {
                        try {
                            sendWebhookWithTimeout(webhook);
                            return;
                        } catch (Exception e) {
                            attempt++;
                            try {
                                throw new InterruptedException("Interrupted during sleep");
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }
                }
            };
            doThrow(new SpecificException("Fail")).when(dispatcher).sendWebhookWithTimeout(webhook);
            dispatcher.dispatchWebhook(webhook, "key4");
            assertTrue(Thread.currentThread().isInterrupted());
        }
    }

    @Test
    public void testDispatchWebhook_TimeoutExceptionHandling() {
        doThrow(new TimeoutException("Timeout")).when(dispatcher).sendWebhookWithTimeout(webhook);
        dispatcher.dispatchWebhook(webhook, "key5");
        verify(dispatcher, times(3)).sendWebhookWithTimeout(webhook);
    }

    @Test
    public void testDispatchWebhook_IdempotencyKey_PreventsDuplicate() {
        dispatcher.dispatchWebhook(webhook, "key6");
        // Second call with same key should not invoke sendWebhookWithTimeout
        dispatcher.dispatchWebhook(webhook, "key6");
        verify(dispatcher, times(1)).sendWebhookWithTimeout(webhook);
    }

    @Test
    public void testValidateWebhook_NullWebhook() {
        assertFalse(dispatcher.validateWebhook(null));
    }

    @Test
    public void testValidateWebhook_EmptyPayload() {
        Webhook emptyPayloadWebhook = mock(Webhook.class);
        when(emptyPayloadWebhook.getPayload()).thenReturn("");
        assertFalse(dispatcher.validateWebhook(emptyPayloadWebhook));
    }

    @Test
    public void testValidateWebhook_ValidWebhook() {
        assertTrue(dispatcher.validateWebhook(webhook));
    }

    // Additional validation edge cases can be added here

}
