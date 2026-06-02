import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Enhanced LoggerConfig for asynchronous and structured logging with context and thread pool support.
 * This enforces observability best practices with consistent logging context propagation.
 */
public class LoggerConfig {
    private static final Logger logger = LoggerFactory.getLogger(LoggerConfig.class);
    private static final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private static void logAsync(Runnable logAction, String traceId, String userId) {
        executor.submit(() -> {
            if (traceId != null) MDC.put("traceId", traceId);
            if (userId != null) MDC.put("userId", userId);
            try {
                logAction.run();
            } finally {
                MDC.clear();
            }
        });
    }

    public static void logInfoAsync(String message, String traceId, String userId) {
        logAsync(() -> logger.info(message), traceId, userId);
    }

    public static void logErrorAsync(String message, String traceId, String userId, Throwable throwable) {
        logAsync(() -> logger.error(message, throwable), traceId, userId);
    }

    public static void logDebugAsync(String message, String traceId, String userId) {
        logAsync(() -> logger.debug(message), traceId, userId);
    }

    public static void logWarnAsync(String message, String traceId, String userId) {
        logAsync(() -> logger.warn(message), traceId, userId);
    }

    /**
     * Shutdown the executor service gracefully.
     */
    public static void shutdown() {
        executor.shutdown();
    }
}
