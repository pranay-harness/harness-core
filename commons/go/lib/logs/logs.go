// Copyright 2020 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package logs

import (
	"context"
	"fmt"
	"net/http"
	"os"

	"github.com/gofrs/uuid"
	"go.uber.org/zap"
	"go.uber.org/zap/zapcore"
	"go.uber.org/zap/zaptest/observer"
)

type loggerKey struct{}

// L is an alias for the the standard logger.
var L *zap.SugaredLogger

// GetLogger returns a configured zap.Logger configuration
func GetLogger(deployment string, deployable string, taskID string, customerID string, sessionID string, applicationID string, verbose bool) (*zap.Logger, error) {
	return NewBuilder().
		Verbose(verbose).
		WithDeployment(deployment).
		WithFields("task_id", taskID, "deployable", deployable, "customer_id", customerID,
			"session_id", sessionID, "application_id", applicationID).
		Build()
}

// GetObservedLogger returns *zap.Logger and corresponding in memory recorded observed logs
// for testing logging events in unit tests
func GetObservedLogger(enab zapcore.LevelEnabler) (*zap.Logger, *observer.ObservedLogs) {
	core, observedLogs := observer.New(enab)
	obsOption := zap.WrapCore(func(zapcore.Core) zapcore.Core {
		return core
	})
	logger, err := zap.NewDevelopment()
	if err != nil {
		fmt.Println("error initializing logger", err)
		os.Exit(1)
	}
	l := logger.WithOptions(obsOption)
	return l, observedLogs
}

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

// Middleware provides logging middleware.
func Middleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		id := r.Header.Get("X-Request-ID")
		if id == "" {
			uuid, _ := uuid.NewV4()
			id = uuid.String()
		}
		ctx := r.Context()
		log := FromContext(ctx).With(zap.String("request-id", id))
		accountID := r.FormValue("accountId")
		log = log.With(
			"accountId", accountID,
			"method", r.Method,
			"request", r.RequestURI,
			"remote", r.RemoteAddr,
		)

		ctx = WithContext(ctx, log)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}