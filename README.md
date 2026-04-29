# Webhook Service

## Overview
This service handles webhook dispatching with optimizations to reduce latency and improve performance.

## Changes Made
- Adjusted the retry logic to a fixed limit for better control over dispatch attempts.
- Implemented timeout settings to prevent indefinite waits during webhook dispatch.
- Introduced rate-limiting mechanisms to manage simultaneous dispatches efficiently.

## Setup Instructions
- Ensure the necessary configurations are set in the WebhookDispatcher.java file.

## Usage Examples
- Refer to the source code for usage examples of the new dispatch methods.

## Troubleshooting Tips
- Monitor the logs for timeout exceptions and adjust timeout durations as necessary.

## Contributing Guidelines
- Follow the standard contribution process for any changes made to this repository.