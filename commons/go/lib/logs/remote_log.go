// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package logs

import (
	"go.uber.org/zap"
	"go.uber.org/zap/zapcore"
)

// defaultLimit is the default maximum log size in bytes.
const (
	messageKey = "msg"
	levelKey   = "level"
	nameKey    = "logger"
)

type RemoteLogger struct {
	BaseLogger *zap.SugaredLogger
	// This Writer is used inside the logger implementation and can be used
	// directly as well for streaming in subprocesses
	Writer StreamWriter
}

// NewRemoteLogger returns an instance of RemoteLogger
func NewRemoteLogger(writer StreamWriter) (*RemoteLogger, error) {
	ws := zapcore.AddSync(writer)
	encoderCfg := zapcore.EncoderConfig{
		MessageKey:     messageKey,
		LevelKey:       levelKey,
		NameKey:        nameKey,
		EncodeLevel:    zapcore.LowercaseLevelEncoder,
		EncodeTime:     zapcore.ISO8601TimeEncoder,
		EncodeDuration: zapcore.StringDurationEncoder,
	}
	core := zapcore.NewCore(zapcore.NewJSONEncoder(encoderCfg), ws, zap.InfoLevel)
	logger := zap.New(core)
	log := logger.Sugar()
	rl := &RemoteLogger{log, writer}
	// Try to open the stream. Continue using the writer even if it's unsuccessful
	go rl.Writer.Open()
	return rl, nil
}
