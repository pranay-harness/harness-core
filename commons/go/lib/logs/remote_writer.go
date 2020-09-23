// This code has been adapted from Drone
// https://github.com/drone/runner-go/blob/master/livelog/livelog.go

package logs

import (
	"bytes"
	"context"
	"encoding/json"
	"strings"
	"sync"
	"time"

	"github.com/wings-software/portal/product/log-service/client"
	"github.com/wings-software/portal/product/log-service/stream"
)

// RemoteWriter is an io.Writer that sends logs to the server.
type RemoteWriter struct {
	sync.Mutex

	client client.Client // client

	key string // Unique key to identify in storage

	num   int
	now   time.Time
	size  int
	limit int

	interval time.Duration
	pending  []*stream.Line
	history  []*stream.Line

	closed bool
	close  chan struct{}
	ready  chan struct{}
}

// NewWriter returns a new writer
func NewRemoteWriter(client client.Client, key string) StreamWriter {
	b := &RemoteWriter{
		client:   client,
		key:      key,
		now:      time.Now(),
		limit:    defaultLimit,
		interval: time.Second,
		close:    make(chan struct{}),
		ready:    make(chan struct{}, 1),
	}
	go b.Start()
	return b
}

// SetLimit sets the Writer limit.
func (b *RemoteWriter) SetLimit(limit int) {
	b.limit = limit
}

// SetInterval sets the Writer flusher interval.
func (b *RemoteWriter) SetInterval(interval time.Duration) {
	b.interval = interval
}

// Write uploads the live log stream to the server.
func (b *RemoteWriter) Write(p []byte) (n int, err error) {
	for _, part := range split(p) {
		line := &stream.Line{
			Number:    b.num,
			Message:   part,
			Timestamp: int64(time.Since(b.now).Seconds()),
		}

		for b.size+len(p) > b.limit {
			b.stop() // buffer is full, stop streaming data
			b.size -= len(b.history[0].Message)
			b.history = b.history[1:]
		}

		b.size = b.size + len(part)
		b.num++

		if b.stopped() == false {
			b.Lock()
			b.pending = append(b.pending, line)
			b.Unlock()
		}

		b.Lock()
		b.history = append(b.history, line)
		b.Unlock()
	}

	select {
	case b.ready <- struct{}{}:
	default:
	}

	return len(p), nil
}

func (b *RemoteWriter) Open() error {
	return b.client.Open(context.Background(), b.key)
}

// Close closes the writer and uploads the full contents to
// the server.
func (b *RemoteWriter) Close() error {
	if b.stop() {
		b.flush()
	}
	err := b.upload()
	// Close the log stream once upload has completed
	b.client.Close(context.Background(), b.key)
	return err
}

// upload uploads the full log history to the server.
func (b *RemoteWriter) upload() error {
	// Write history to a file and use that for upload.
	data := new(bytes.Buffer)
	for _, line := range b.history {
		buf := new(bytes.Buffer)
		json.NewEncoder(buf).Encode(line)
		data.Write(buf.Bytes())
	}
	err := b.client.Upload(context.Background(), b.key, data)
	return err
}

// flush batch uploads all buffered logs to the server.
func (b *RemoteWriter) flush() error {
	b.Lock()
	lines := b.copy()
	b.clear()
	b.Unlock()
	if len(lines) == 0 {
		return nil
	}
	return b.client.Write(
		context.Background(), b.key, lines)

}

// copy returns a copy of the buffered lines.
func (b *RemoteWriter) copy() []*stream.Line {
	return append(b.pending[:0:0], b.pending...)
}

// clear clears the buffer.
func (b *RemoteWriter) clear() {
	b.pending = b.pending[:0]
}

func (b *RemoteWriter) stop() bool {
	b.Lock()
	var closed bool
	if b.closed == false {
		close(b.close)
		closed = true
		b.closed = true
	}
	b.Unlock()
	return closed
}

func (b *RemoteWriter) stopped() bool {
	b.Lock()
	closed := b.closed
	b.Unlock()
	return closed
}

func (b *RemoteWriter) Start() error {
	for {
		select {
		case <-b.close:
			return nil
		case <-b.ready:
			select {
			case <-b.close:
				return nil
			case <-time.After(b.interval):
				// we intentionally ignore errors. log streams
				// are ephemeral and are considered low priority
				b.flush()
			}
		}
	}
}

func split(p []byte) []string {
	s := string(p)
	v := []string{s}
	// kubernetes buffers the output and may combine
	// multiple lines into a single block of output.
	// Split into multiple lines.
	//
	// note that docker output always inclines a line
	// feed marker. This needs to be accounted for when
	// splitting the output into multiple lines.
	if strings.Contains(strings.TrimSuffix(s, "\n"), "\n") {
		v = strings.SplitAfter(s, "\n")
	}
	return v
}
