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
    public void testDispatchWebhook_ImmediateSuccess() {
        WebhookDispatcher dispatcher = new WebhookDispatcher(3);
        Webhook webhook = new Webhook(); // Mock or create a webhook object

        // Mock sendWebhook to do nothing
        doNothing().when(dispatcher).sendWebhook(webhook);

        dispatcher.dispatchWebhook(webhook);

        // Verify sendWebhook was called once
        verify(dispatcher, times(1)).sendWebhook(webhook);
    }

    @Test
    public void testDispatchWebhook_FailureThenSuccess() {
        WebhookDispatcher dispatcher = new WebhookDispatcher(3);
        Webhook webhook = new Webhook(); // Mock or create a webhook object

        // Mock sendWebhook to throw exception on first call, then do nothing on the second call
        doThrow(new SpecificException("Error sending webhook"))
            .doNothing()
            .when(dispatcher).sendWebhook(webhook);

        dispatcher.dispatchWebhook(webhook);

        // Verify sendWebhook was called twice: once for failure, once for success
        verify(dispatcher, times(2)).sendWebhook(webhook);
    }

    @Test
    public void testDispatchWebhook_AllAttemptsFail() {
        WebhookDispatcher dispatcher = new WebhookDispatcher(3);
        Webhook webhook = new Webhook(); // Mock or create a webhook object

        // Mock sendWebhook to always throw SpecificException
        doThrow(new SpecificException("Error sending webhook")).when(dispatcher).sendWebhook(webhook);

        dispatcher.dispatchWebhook(webhook);

        // Verify sendWebhook was called maxRetries times
        verify(dispatcher, times(3)).sendWebhook(webhook);
        // Here you can add additional assertions for logging or handling failure
    }

    // New tests for network-related failures

    @Test
    public void testDispatchWebhook_NetworkTimeout() {
        WebhookDispatcher dispatcher = new WebhookDispatcher(3);
        Webhook webhook = new Webhook();

        // Simulate network timeout exception
        doThrow(new NetworkTimeoutException("Network timeout occurred")).when(dispatcher).sendWebhook(webhook);

        dispatcher.dispatchWebhook(webhook);

        verify(dispatcher, times(3)).sendWebhook(webhook);
    }

    @Test
    public void testDispatchWebhook_ConnectionReset() {
        WebhookDispatcher dispatcher = new WebhookDispatcher(3);
        Webhook webhook = new Webhook();

        // Simulate connection reset exception
        doThrow(new ConnectionResetException("Connection reset by peer")).when(dispatcher).sendWebhook(webhook);

        dispatcher.dispatchWebhook(webhook);

        verify(dispatcher, times(3)).sendWebhook(webhook);
    }
}
