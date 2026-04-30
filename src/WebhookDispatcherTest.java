import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class WebhookDispatcherTest {

    @Test
    public void testDispatchWebhook_Success() {
        WebhookDispatcher dispatcher = new WebhookDispatcher(3);
        Webhook webhook = new Webhook(); // Mock or create a webhook object

        // Mock sendWebhook to do nothing
        doNothing().when(dispatcher).sendWebhook(webhook);

        dispatcher.dispatchWebhook(webhook);

        // Verify sendWebhook was called
        verify(dispatcher, times(1)).sendWebhook(webhook);
    }

    @Test
    public void testDispatchWebhook_MaxRetries() {
        WebhookDispatcher dispatcher = new WebhookDispatcher(3);
        Webhook webhook = new Webhook(); // Mock or create a webhook object

        // Mock sendWebhook to always throw SpecificException
        doThrow(new SpecificException("Error sending webhook")).when(dispatcher).sendWebhook(webhook);

        dispatcher.dispatchWebhook(webhook);

        // Verify sendWebhook was called maxRetries times
        verify(dispatcher, times(3)).sendWebhook(webhook);
    }

    @Test
    public void testValidateWebhook_NullWebhook() {
        WebhookDispatcher dispatcher = new WebhookDispatcher(3);
        assertFalse(dispatcher.validateWebhook(null));
    }

    @Test
    public void testValidateWebhook_EmptyPayload() {
        WebhookDispatcher dispatcher = new WebhookDispatcher(3);
        Webhook webhook = mock(Webhook.class);
        when(webhook.getPayload()).thenReturn("");
        assertFalse(dispatcher.validateWebhook(webhook));
    }

    @Test
    public void testValidateWebhook_ValidWebhook() {
        WebhookDispatcher dispatcher = new WebhookDispatcher(3);
        Webhook webhook = mock(Webhook.class);
        when(webhook.getPayload()).thenReturn("some payload");
        assertTrue(dispatcher.validateWebhook(webhook));
    }
}
