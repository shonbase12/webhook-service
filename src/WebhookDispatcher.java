// Updated Webhook Dispatcher with retry logic

public class WebhookDispatcher {
    // Existing code...

    public void dispatchWebhook(Webhook webhook) {
        int retryCount = 0;
        boolean success = false;

        while (retryCount < MAX_RETRIES && !success) {
            try {
                // Code to send webhook...
                success = true; // Assuming send is successful
            } catch (Exception e) {
                retryCount++;
                if (retryCount >= MAX_RETRIES) {
                    // Log failure and handle accordingly
                }
            }
        }
    }
}