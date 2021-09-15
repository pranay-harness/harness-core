package state

import (
	"sync"
)

type status int

const (
	notStarted status = iota
	running
	complete
)

var (
	s    *executionState
	once sync.Once
)

type executionState struct {
	mu              sync.Mutex
	executionStatus map[string]status
}

// If a job with an execution ID is already running or completed, it returns false.
// Otherwise returns true and marks the execution status as running.
func (s *executionState) CanRun(executionID string) bool {
	s.mu.Lock()
	defer s.mu.Unlock()

	if st, ok := s.executionStatus[executionID]; ok && st != notStarted {
		return false
	} else {
		s.executionStatus[executionID] = running
		return true
	}
}

func (s *executionState) MarkAsComplete(executionID string) {
	s.mu.Lock()
	defer s.mu.Unlock()

	s.executionStatus[executionID] = complete
}

func (s *executionState) IsAnyExecutionRunning() bool {
	s.mu.Lock()
	defer s.mu.Unlock()

	for _, status := range s.executionStatus {
		if status == running {
			return true
		}
	}
	return false
}

// ExecutionState returns execution state
func ExecutionState() *executionState {
	once.Do(func() {
		s = &executionState{}
		s.executionStatus = make(map[string]status)
	})
	return s
}
