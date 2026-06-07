package config

// RetryConfig holds retry configuration for the webhook service.
type RetryConfig struct {
	MaxDuration int // Maximum duration in seconds for retries
}

// DefaultRetryConfig returns a default retry configuration.
func DefaultRetryConfig() RetryConfig {
	return RetryConfig{MaxDuration: 300} // 5 minutes
}