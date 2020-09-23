// Package stream defines the live log streaming interface.
package stream

import (
	"context"
	"errors"
)

// ErrNotFound is returned when a stream is not registered
// with the Streamer.
var ErrNotFound = errors.New("stream: not found")

// Stream defines the live log streaming interface.
type Stream interface {
	// Create creates the log stream for the string key.
	Create(context.Context, string) error

	// Delete deletes the log stream for the string key.
	Delete(context.Context, string) error

	// Write writes to the log stream.
	// TODO(bradrydzewski) change *Line to a proper slice.
	Write(context.Context, string, ...*Line) error

	// Tail tails the log stream.
	Tail(context.Context, string) (<-chan *Line, <-chan error)

	// Info returns internal stream information.
	Info(context.Context) *Info
}

// Line represents a line in the logs.
type Line struct {
	Number    int    `json:"pos"`
	Message   string `json:"out"`
	Timestamp int64  `json:"time"`
}

// Info provides internal stream information. This can be
// used to monitor the number of registered streams and
// subscribers.
type Info struct {
	// Streams is a key-value pair mapping the unique key to
	// the count of subscribers
	Streams map[string]Stats `json:"streams"`
}

// Stats provides statistics about an individual stream,
// including the size of the stream and the number of
// subscribers.
type Stats struct {
	Size int `json:"size"`
	Subs int `json:"subscribers"`
}
