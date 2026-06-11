// Implementation of error classification logic

public class ErrorClassification {
    public enum ErrorType {
        TRANSIENT,
        PERMANENT
    }

    public static ErrorType classifyError(int httpResponseCode) {
        if (httpResponseCode >= 500) {
            return ErrorType.TRANSIENT;
        } else {
            return ErrorType.PERMANENT;
        }
    }
}