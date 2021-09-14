// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

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
