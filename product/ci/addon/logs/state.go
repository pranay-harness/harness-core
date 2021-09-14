// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

// Package state contains state of addon logs which are not part of a step (right now, these are service and logs of the addon container itself).
// Since there is a dependency on the gRPC server running on lite engine, pending logs are stopped before the lite engine server is stopped.
package state

import (
	"github.com/wings-software/portal/commons/go/lib/logs"
	"sync"
)

var (
	s    *logState
	once sync.Once
)

// logState captures the state of different log objects
type logState struct {
	pendingLogs chan *logs.RemoteLogger // used as a thread-safe queue
}

// PendingLogs returns a channel containing the logs which need to be explicitly closed
func (s *logState) PendingLogs() chan *logs.RemoteLogger {
	return s.pendingLogs
}

// ClosePendingLogs closes all the streams present in the channel and returns back
func (s *logState) ClosePendingLogs() {
L:
	for {
		select {
		case log := <-s.pendingLogs:
			log.Writer.Close()
		default:
			break L
		}
	}
}

// LogState returns a singleton logState object to be used through the lifecycle of the addon
func LogState() *logState {
	once.Do(func() {
		s = &logState{
			pendingLogs: make(chan *logs.RemoteLogger, 100),
		}
	})
	return s
}
