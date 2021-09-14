// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package memory

import (
	"sync"

	"github.com/wings-software/portal/product/log-service/stream"
)

type subscriber struct {
	sync.Mutex

	handler chan *stream.Line
	closec  chan struct{}
	closed  bool
}

func (s *subscriber) publish(line *stream.Line) {
	select {
	case <-s.closec:
	case s.handler <- line:
	default:
		// lines are sent on a buffered channel. If there
		// is a slow consumer that is not processing events,
		// the buffered channel will fill and newer messages
		// are ignored.
	}
}

func (s *subscriber) close() {
	s.Lock()
	if !s.closed {
		close(s.closec)
		s.closed = true
	}
	s.Unlock()
}
