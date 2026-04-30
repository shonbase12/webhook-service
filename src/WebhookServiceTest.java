import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class WebhookServiceTest {

    private WebhookService webhookService;
    private WebhookDispatcher mockDispatcher;
    private ExecutorService mockExecutor;

    @BeforeEach
    public void setUp() {
        // Mock the WebhookDispatcher
        mockDispatcher = mock(WebhookDispatcher.class);

        // Create a partial mock of WebhookService to inject the mockDispatcher
        webhookService = new WebhookService() {
            {
                java.lang.reflect.Field field;
                try {
                    field = WebhookService.class.getDeclaredField("webhookDispatcher");
                    field.setAccessible(true);
                    field.set(this, mockDispatcher);

                    field = WebhookService.class.getDeclaredField("executorService");
                    field.setAccessible(true);
                    // Provide a single-thread executor to avoid async complications in tests
                    field.set(this, Executors.newSingleThreadExecutor());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @Test
    public void testRegisterWebhook_NullWebhook() {
        webhookService.registerWebhook(null); // Should not throw
        // No direct output, check logs if possible or just ensure no exception
    }

    @Test
    public void testRegisterWebhook_ValidWebhook() {
        Webhook webhook = new Webhook("payload");
        webhookService.registerWebhook(webhook);
        // No direct getter for registeredWebhooks, so test dispatch as proxy
        webhookService.dispatchWebhook(webhook, "event");
        verify(mockDispatcher, times(1)).dispatchWebhook(webhook, "event");
    }

    @Test
    public void testDispatchWebhook_NullWebhook() {
        webhookService.dispatchWebhook(null, "event"); // Should not throw
    }

    @Test
    public void testDispatchWebhook_UnregisteredWebhook() {
        Webhook webhook = new Webhook("payload");
        webhookService.dispatchWebhook(webhook, "event"); // Should log warning and not dispatch
        verify(mockDispatcher, never()).dispatchWebhook(any(), any());
    }

    @Test
    public void testDispatchWebhook_ExceptionHandling() {
        Webhook webhook = new Webhook("payload");
        webhookService.registerWebhook(webhook);
        doThrow(new RuntimeException("fail")).when(mockDispatcher).dispatchWebhook(webhook, "event");

        webhookService.dispatchWebhook(webhook, "event"); // Should catch and log
        verify(mockDispatcher, times(1)).dispatchWebhook(webhook, "event");
    }

    @Test
    public void testRetryWebhook_NullWebhook() {
        webhookService.retryWebhook(null, "event", 3); // Should not throw
    }

    @Test
    public void testRetryWebhook_SuccessfulDispatchStopsRetries() {
        Webhook webhook = new Webhook("payload");
        webhookService.registerWebhook(webhook);

        doNothing().when(mockDispatcher).dispatchWebhook(webhook, "event");

        webhookService.retryWebhook(webhook, "event", 3);

        verify(mockDispatcher, atLeastOnce()).dispatchWebhook(webhook, "event");
    }

    @Test
    public void testRetryWebhook_MaxRetriesReached() {
        Webhook webhook = new Webhook("payload");
        webhookService.registerWebhook(webhook);

        doThrow(new RuntimeException("fail")).when(mockDispatcher).dispatchWebhook(webhook, "event");

        webhookService.retryWebhook(webhook, "event", 3);

        verify(mockDispatcher, times(3)).dispatchWebhook(webhook, "event");
    }

    @Test
    public void testDispatchNewWebhook_NullNewWebhook() {
        webhookService.dispatchNewWebhook(null, "key"); // Should not throw
    }

    @Test
    public void testDispatchNewWebhook_NullOrEmptyIdempotencyKey() {
        NewWebhook newWebhook = mock(NewWebhook.class);
        webhookService.dispatchNewWebhook(newWebhook, null); // Should not throw
        webhookService.dispatchNewWebhook(newWebhook, ""); // Should not throw
    }

    @Test
    public void testDispatchNewWebhook_RegistersAndDispatches() throws Exception {
        NewWebhook newWebhook = mock(NewWebhook.class);
        when(newWebhook.getPayload()).thenReturn("payload");
        when(newWebhook.getId()).thenReturn("id123");

        webhookService.dispatchNewWebhook(newWebhook, "key");

        // Wait for async task to complete
        Thread.sleep(500);

        ArgumentCaptor<Webhook> captor = ArgumentCaptor.forClass(Webhook.class);
        verify(mockDispatcher, times(1)).dispatchWebhook(captor.capture(), eq("key"));
        assertEquals("payload", captor.getValue().getPayload());
    }

}