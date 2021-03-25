package state

import (
	"sync"
)

var (
	s    *executionState
	once sync.Once
)

type executionState struct {
	mu                sync.Mutex
	runningExecutions map[string]bool
}

// If a job with an execution ID is already running, it return false.
// Otherwise returns true.
func (s *executionState) CanRun(executionID string) bool {
	s.mu.Lock()
	defer s.mu.Unlock()

	if _, ok := s.runningExecutions[executionID]; ok {
		return false
	} else {
		s.runningExecutions[executionID] = true
		return true
	}
}

// ExecutionState returns execution state
func ExecutionState() *executionState {
	once.Do(func() {
		s = &executionState{}
		s.runningExecutions = make(map[string]bool)
	})
	return s
}
