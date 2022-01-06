// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

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
