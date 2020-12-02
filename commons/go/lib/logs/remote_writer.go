// This code has been adapted from Drone
// https://github.com/drone/runner-go/blob/master/livelog/livelog.go

package logs

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
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
	prev     []byte

	closed bool
	close  chan struct{}
	ready  chan struct{}
}

// NewWriter returns a new writer
func NewRemoteWriter(client client.Client, key string) *RemoteWriter {
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

// Convert converts a byte slice to a line format used by the log service
func (b *RemoteWriter) Convert(p string) *stream.Line {
	var jsonMap map[string]interface{}
	args := make(map[string]string)
	err := json.Unmarshal([]byte(p), &jsonMap)
	msg, ok1 := jsonMap[messageKey]
	level, ok2 := jsonMap[levelKey]
	if err != nil || !ok1 || !ok2 {
		// If the message is not in JSON, just use the bytes as the `Message` field
		return &stream.Line{
			Level:     defaultLevel,
			Message:   p,
			Number:    b.num,
			Timestamp: time.Now(),
			Args:      args,
		}
	}
	// Parse all the arguments into Args
	for k, v := range jsonMap {
		args[k] = fmt.Sprintf("%v", v)
	}
	return &stream.Line{
		Level:     fmt.Sprintf("%v", level),
		Message:   fmt.Sprintf("%v", msg),
		Number:    b.num,
		Timestamp: time.Now(),
		Args:      args,
	}
}

// Write uploads the live log stream to the server.
func (b *RemoteWriter) Write(p []byte) (n int, err error) {
	var res []byte
	// Return if a new line character is not present in the input.
	// Commands like `mvn` flush character by character so this prevents
	// spamming of single-character logs.
	if !bytes.Contains(p, []byte("\n")) {
		b.prev = append(b.prev, p...)
		return len(p), nil
	}

	res = append(b.prev, p...)
	b.prev = []byte{}

	for _, part := range split(res) {
		line := b.Convert(part)

		for b.size+len(p) > b.limit {
			// Keep streaming even after the limit, but only upload last `b.limit` data to the store
			if len(b.history) == 0 {
				break
			}
			b.size -= len(b.history[0].Message)
			b.history = b.history[1:]
		}

		b.size = b.size + len(part)
		b.num++

		if !b.stopped() {
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
	err := b.client.Open(context.Background(), b.key)
	if err != nil {
		b.stop() // stop trying to stream if we could not open the stream
		return err
	}
	return nil
}

// Close closes the writer and uploads the full contents to
// the server.
func (b *RemoteWriter) Close() error {
	if b.stop() {
		// Flush anything waiting on a new line
		if len(b.prev) > 0 {
			b.Write([]byte("\n"))
		}
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
		if err := json.NewEncoder(buf).Encode(line); err != nil {
			return err
		}
		data.Write(buf.Bytes())
		data.Write([]byte("\n"))
	}
	link, err := b.client.UploadLink(context.Background(), b.key)
	if err != nil {
		return err
	}
	err = b.client.UploadUsingLink(context.Background(), link.Value, data)
	return err
}

// flush batch uploads all buffered logs to the server.
func (b *RemoteWriter) flush() error {
	// If the stream was not opened successfully, skip trying to flush logs to the stream
	if b.closed {
		return errors.New("stream is not open")
	}
	b.Lock()
	lines := b.copy()
	b.clear()
	b.Unlock()
	if len(lines) == 0 {
		return nil
	}
	// ONLY FOR DEBUG PURPOSES (TEMPORARY)
	for _, line := range lines {
		jsonLine, _ := json.Marshal(line)
		fmt.Println(string(jsonLine))
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
	if !b.closed {
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

// Start starts a periodic loop to flush logs to the live stream
func (b *RemoteWriter) Start() error {
	intervalTimer := time.NewTimer(b.interval)
	for {
		select {
		case <-b.close:
			return nil
		case <-b.ready:
			intervalTimer.Reset(b.interval)
			select {
			case <-b.close:
				return nil
			case <-intervalTimer.C:
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
