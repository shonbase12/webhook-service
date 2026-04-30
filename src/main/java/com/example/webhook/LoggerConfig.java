import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Improved LoggerConfig for asynchronous and structured logging with context support.
 */
public class LoggerConfig {
    private static final Logger logger = LoggerFactory.getLogger(LoggerConfig.class);

    /**
     * Logs an info message asynchronously with optional context data.
     * @param message the log message
     * @param traceId optional trace ID for distributed tracing
     * @param userId optional user ID for context
     */
    public static void logInfoAsync(String message, String traceId, String userId) {
        new Thread(() -> {
            if (traceId != null) MDC.put("traceId", traceId);
            if (userId != null) MDC.put("userId", userId);
            logger.info(message);
            MDC.clear();
        }).start();
    }

    /**
     * Logs an error message asynchronously with optional context data.
     * @param message the log message
     * @param traceId optional trace ID for distributed tracing
     * @param userId optional user ID for context
     * @param throwable optional exception
     */
    public static void logErrorAsync(String message, String traceId, String userId, Throwable throwable) {
        new Thread(() -> {
            if (traceId != null) MDC.put("traceId", traceId);
            if (userId != null) MDC.put("userId", userId);
            logger.error(message, throwable);
            MDC.clear();
        }).start();
    }

    /**
     * Logs a debug message asynchronously with optional context data.
     * @param message the log message
     * @param traceId optional trace ID for distributed tracing
     * @param userId optional user ID for context
     */
    public static void logDebugAsync(String message, String traceId, String userId) {
        new Thread(() -> {
            if (traceId != null) MDC.put("traceId", traceId);
            if (userId != null) MDC.put("userId", userId);
            logger.debug(message);
            MDC.clear();
        }).start();
    }

    /**
     * Logs a warn message asynchronously with optional context data.
     * @param message the log message
     * @param traceId optional trace ID for distributed tracing
     * @param userId optional user ID for context
     */
    public static void logWarnAsync(String message, String traceId, String userId) {
        new Thread(() -> {
            if (traceId != null) MDC.put("traceId", traceId);
            if (userId != null) MDC.put("userId", userId);
            logger.warn(message);
            MDC.clear();
        }).start();
    }
}
