// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package state

import (
	"fmt"
	"sync"
)

var (
	s    *executionState
	once sync.Once
)

const (
	RUNNING = iota
	PAUSED  = iota

	bufferSize = 10
)

type executionState struct {
	mu           sync.Mutex
	resumeSignal chan bool
	state        int
}

func (s *executionState) SetState(state int) {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.state = state
}

func (s *executionState) GetState() int {
	s.mu.Lock()
	defer s.mu.Unlock()
	return s.state
}

func (s *executionState) ResumeSignal() chan bool {
	return s.resumeSignal
}

// ExecutionState returns execution state
func ExecutionState() *executionState {
	once.Do(func() {
		s = &executionState{
			state:        RUNNING,
			resumeSignal: make(chan bool, bufferSize),
		}
	})
	return s
}

// ExecutionStateStr returns execution state in string format
func ExecutionStateStr(state int) string {
	switch state {
	case RUNNING:
		return "running"
	case PAUSED:
		return "paused"
	default:
		return fmt.Sprintf("unknown:%d", state)
	}
}
