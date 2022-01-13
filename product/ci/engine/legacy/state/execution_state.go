// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

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
