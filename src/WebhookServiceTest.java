import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class WebhookServiceTest {

    private WebhookService webhookService;
    private WebhookDispatcher mockDispatcher;

    @BeforeEach
    public void setUp() {
        // Mock the WebhookDispatcher
        mockDispatcher = mock(WebhookDispatcher.class);

        // Create a partial mock of WebhookService to inject the mockDispatcher
        webhookService = new WebhookService() {
            {
                // Override the webhookDispatcher instance
                java.lang.reflect.Field field;
                try {
                    field = WebhookService.class.getDeclaredField("webhookDispatcher");
                    field.setAccessible(true);
                    field.set(this, mockDispatcher);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @Test
    public void testDispatchWebhook_CallsDispatcher() {
        Webhook webhook = new Webhook("payload");
        String event = "eventData";

        webhookService.dispatchWebhook(webhook, event);

        verify(mockDispatcher, times(1)).dispatchWebhook(webhook, event);
    }

    @Test
    public void testRetryWebhook_CallsDispatcherMultipleTimes() {
        Webhook webhook = new Webhook("payload");
        String event = "eventData";
        int retryCount = 3;

        webhookService.retryWebhook(webhook, event, retryCount);

        verify(mockDispatcher, times(retryCount)).dispatchWebhook(webhook, event);
    }

    @Test
    public void testDispatchNewWebhook_ConvertsAndDispatches() {
        NewWebhook newWebhook = mock(NewWebhook.class);
        when(newWebhook.getPayload()).thenReturn("newPayload");

        webhookService.dispatchNewWebhook(newWebhook, "idempotencyKey");

        ArgumentCaptor<Webhook> captor = ArgumentCaptor.forClass(Webhook.class);
        verify(mockDispatcher, times(1)).dispatchWebhook(captor.capture(), eq("idempotencyKey"));

        Webhook dispatchedWebhook = captor.getValue();
        assertEquals("newPayload", dispatchedWebhook.getPayload());
    }

}
