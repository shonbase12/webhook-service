package com.novapay.webhooks;

public class WebhookDispatcher {
    private static final int MAX_RETRIES = 5;

    public void dispatch(WebhookEvent event) {
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            try {
                send(event);
                return;
            } catch (Exception e) {
                attempt++;
                long backoff = (long) Math.pow(2, attempt) * 1000;
                try { Thread.sleep(backoff); } catch (InterruptedException ie) { break; }
            }
        }
        deadLetterQueue.enqueue(event);
    }
}
