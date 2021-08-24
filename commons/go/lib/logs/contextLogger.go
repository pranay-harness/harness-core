package logs

import (
	"context"
	"go.uber.org/zap"
)

type correlationIdType int

const (
	requestIdKey correlationIdType = iota
	sessionIdKey
)

var logger *zap.SugaredLogger

func InitContextLogger(depName, appName string) {
	// a fallback/root logger for events without context
	logBuilder := NewBuilder().Verbose(true).WithDeployment(depName).
		WithFields("application_name", appName)
	logger = logBuilder.MustBuild().Sugar()
}

// WithRqId returns a context which knows its request ID
func WithRqId(ctx context.Context, rqId string) context.Context {
	if len(rqId) == 0 {
		rqId = "defaultId"
	}
	return context.WithValue(ctx, requestIdKey, rqId)
}

// WithSessionId returns a context which knows its session ID
func WithSessionId(ctx context.Context, sessionId string) context.Context {
	if len(sessionId) == 0 {
		sessionId = "defaultId"
	}
	return context.WithValue(ctx, sessionIdKey, sessionId)
}

// Logger returns a zap logger with as much context as possible
func Logger(ctx context.Context) *zap.SugaredLogger {
	newLogger := logger
	if ctx != nil {
		if ctxRqId, ok := ctx.Value(requestIdKey).(string); ok {
			newLogger = newLogger.With(zap.String("rqId", ctxRqId))
		}
		if ctxSessionId, ok := ctx.Value(sessionIdKey).(string); ok {
			newLogger = newLogger.With(zap.String("sessionId", ctxSessionId))
		}
	}
	return newLogger
}
