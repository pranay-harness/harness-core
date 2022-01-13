// Copyright 2020 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package memory

import (
	"context"
	"sync"

	"github.com/wings-software/portal/product/log-service/stream"
)

// this is the amount of items that are stored in memory
// in the buffer. This should result in approximately 10kb
// of memory allocated per-stream and per-subscriber, not
// including any logdata stored in these structures.
const bufferSize = 5000

type memoryStream struct {
	sync.Mutex

	hist []*stream.Line
	list map[*subscriber]struct{}
}

func newStream() *memoryStream {
	return &memoryStream{
		list: map[*subscriber]struct{}{},
	}
}

func (s *memoryStream) write(lines ...*stream.Line) error {
	s.Lock()
	s.hist = append(s.hist, lines...)
	for l := range s.list {
		for _, line := range lines {
			l.publish(line)
		}
	}
	// the history should not be unbounded. The history
	// slice is capped and items are removed in a FIFO
	// ordering when capacity is reached.
	if size := len(s.hist); size >= bufferSize {
		s.hist = s.hist[size-bufferSize:]
	}
	s.Unlock()
	return nil
}

func (s *memoryStream) subscribe(ctx context.Context) (<-chan *stream.Line, <-chan error) {
	sub := &subscriber{
		handler: make(chan *stream.Line, bufferSize),
		closec:  make(chan struct{}),
	}
	err := make(chan error)

	s.Lock()
	for _, line := range s.hist {
		sub.publish(line)
	}
	s.list[sub] = struct{}{}
	s.Unlock()

	go func() {
		defer close(err)
		select {
		case <-sub.closec:
		case <-ctx.Done():
			s.Lock()
			delete(s.list, sub)
			s.Unlock()
			sub.close()
		}
	}()
	return sub.handler, err
}

func (s *memoryStream) close() error {
	s.Lock()
	defer s.Unlock()
	for sub := range s.list {
		delete(s.list, sub)
		sub.close()
	}
	return nil
}
