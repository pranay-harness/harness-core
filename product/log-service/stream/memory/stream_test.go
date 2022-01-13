// Copyright 2020 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package memory

import (
	"context"
	"sync"
	"testing"
	"time"

	"github.com/wings-software/portal/product/log-service/stream"
	streampkg "github.com/wings-software/portal/product/log-service/stream"
)

func TestStream(t *testing.T) {
	w := sync.WaitGroup{}

	s := newStream()

	// test ability to replay history. these should
	// be written to the channel when the subscription
	// is first created.

	s.write(&streampkg.Line{Number: 1})
	s.write(&streampkg.Line{Number: 2})
	s.write(&streampkg.Line{Number: 3})
	w.Add(3)

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	stream, errc := s.subscribe(ctx)

	w.Add(4)
	go func() {
		s.write(&streampkg.Line{Number: 4})
		s.write(&streampkg.Line{Number: 5})
		s.write(&streampkg.Line{Number: 6})
		w.Done()
	}()

	// the code above adds 6 lines to the log stream.
	// the wait group blocks until all 6 items are
	// received.

	go func() {
		for {
			select {
			case <-errc:
				return
			case <-stream:
				w.Done()
			}
		}
	}()

	w.Wait()
}

func TestStream_Close(t *testing.T) {
	s := newStream()
	s.hist = []*stream.Line{
		{},
	}

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	s.subscribe(ctx)
	if got, want := len(s.list), 1; got != want {
		t.Errorf("Want %d subscribers before close, got %d", want, got)
	}

	var sub *subscriber
	for sub = range s.list {
	}

	if got, want := sub.closed, false; got != want {
		t.Errorf("Want subscriber open")
	}

	if err := s.close(); err != nil {
		t.Error(err)
	}

	if got, want := len(s.list), 0; got != want {
		t.Errorf("Want %d subscribers after close, got %d", want, got)
	}

	<-time.After(time.Millisecond)

	if got, want := sub.closed, true; got != want {
		t.Errorf("Want subscriber closed")
	}
}

func TestStream_BufferHistory(t *testing.T) {
	s := newStream()

	// exceeds the history buffer by +10
	x := new(stream.Line)
	for i := 0; i < bufferSize+10; i++ {
		s.write(x)
	}

	if got, want := len(s.hist), bufferSize; got != want {
		t.Errorf("Want %d history items, got %d", want, got)
	}

	latest := &stream.Line{Number: 1}
	s.write(latest)

	if got, want := s.hist[len(s.hist)-1], latest; got != want {
		t.Errorf("Expect history stored in FIFO order")
	}
}
