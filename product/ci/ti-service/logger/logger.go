package logger

import (
	"context"
	"go.uber.org/zap"
)

type loggerKey struct{}

// L is an alias for the the standard logger.
var L *zap.SugaredLogger

// WithContext returns a new context with the provided logger. Use in
// combination with logger.WithField(s) for great effect.
func WithContext(ctx context.Context, logger *zap.SugaredLogger) context.Context {
	return context.WithValue(ctx, loggerKey{}, logger)
}

// FromContext retrieves the current logger from the context. If no
// logger is available, the default logger is returned.
func FromContext(ctx context.Context) *zap.SugaredLogger {
	logger := ctx.Value(loggerKey{})
	if logger == nil {
		return L
	}
	return logger.(*zap.SugaredLogger)
}

// FromContext retrieves the current logger from the context. If no
// logger is available, the default logger is returned.
func InitLogger(zap *zap.SugaredLogger) {
	L = zap
}
