# Logging and Observability Standards

This document outlines the standardized logging practices and observability enhancements implemented across all backend repositories to ensure consistency, maintainability, and effective monitoring.

## Common Logging Practices

1. **Structured Logging with JSON**
   - All repositories use `LogstashEncoder` for JSON-formatted logs.
   - This facilitates easy parsing and integration with log aggregation and analysis tools.

2. **MDC Support for Context Propagation**
   - Mapped Diagnostic Context (MDC) is enabled in all appenders.
   - This allows propagation of contextual information (e.g., request IDs, user IDs) across log statements.

3. **Caller Data Inclusion**
   - Logs include caller data (class, method, line number) to enhance traceability.

4. **Appenders Configuration**
   - Console and file appenders are configured with `LogstashEncoder`.
   - File appenders use time-based rolling policy with a 30-day retention to manage log size.
   - Asynchronous appenders wrap console and file appenders to ensure non-blocking logging.

5. **Centralized Log Management**
   - Logs are sent to a centralized log management system using `LogstashTcpSocketAppender` targeting a common Logstash host and port.

6. **Root Logger Settings**
   - Root logger level is set to `INFO`.
   - All async appenders (console, file, logstash) are attached to the root logger.

7. **Library Loggers**
   - Loggers for common libraries (e.g., Spring, Hibernate) are set to `ERROR` level to reduce noise.

8. **Dynamic Log Level Control**
   - JMX configurator is enabled to allow runtime dynamic control of log levels.

9. **Custom Fields**
   - Each repository sets a custom `appname` field in logs to identify the source application.

## Observability Enhancements

- Unified and consistent logging format improves observability and troubleshooting.
- Asynchronous logging improves application performance by reducing logging overhead.
- Centralized log management enables consolidated monitoring and alerting.
- Dynamic log level control via JMX facilitates quick diagnostics in production without redeployments.

## Repository Specific Notes

- Custom `appname` field in logs corresponds to the repository/service name (e.g., `payments-core`, `transaction-engine`).

---

This standard should be followed in all future development and whenever logging configurations are updated to maintain consistency and support effective monitoring and incident response.