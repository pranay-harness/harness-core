package cihandler

import (
	"context"
	"encoding/json"
	"io"
	"net/http"
	"time"

	"github.com/wings-software/portal/product/log-service/handler/util"
	"github.com/wings-software/portal/product/log-service/logger"
	"github.com/wings-software/portal/product/log-service/stream"
)

var pingInterval = time.Second * 30

// HandleOpen returns an http.HandlerFunc that opens
// the live stream.
func HandleOpen(stream stream.Stream) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		ctx := r.Context()

		key := ParseKeyFromURL(r)

		if err := stream.Create(ctx, key); err != nil {
			util.WriteInternalError(w, err)
			logger.FromRequest(r).
				WithError(err).
				WithField("key", key).
				Errorln("api: cannot create stream")
			return
		}

		w.WriteHeader(http.StatusNoContent)
	}
}

// HandleClose returns an http.HandlerFunc that closes
// the live stream.
func HandleClose(stream stream.Stream) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		ctx := r.Context()

		key := ParseKeyFromURL(r)

		if err := stream.Delete(ctx, key); err != nil {
			util.WriteInternalError(w, err)
			logger.FromRequest(r).
				WithError(err).
				WithField("key", key).
				Errorln("api: cannot close stream")
			return
		}

		w.WriteHeader(http.StatusNoContent)
	}
}

// HandleWrite returns an http.HandlerFunc that writes
// to the live stream.
func HandleWrite(s stream.Stream) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		ctx := r.Context()

		key := ParseKeyFromURL(r)

		in := []*stream.Line{}
		if err := json.NewDecoder(r.Body).Decode(&in); err != nil {
			util.WriteBadRequest(w, err)
			logger.FromRequest(r).
				WithError(err).
				WithField("key", key).
				Errorln("api: cannot unmsrshal input")
			return
		}
		// TODO(bradrydzewski) we should alter the Write method to
		// accept a slice of log entries instead of a single log entry.
		if err := s.Write(ctx, key, in...); err != nil {
			if err != nil {
				util.WriteInternalError(w, err)
				logger.FromRequest(r).
					WithError(err).
					WithField("key", key).
					Errorln("api: cannot write to stream")
				return
			}
		}

		w.WriteHeader(http.StatusNoContent)
	}
}

// HandleTail returns an http.HandlerFunc that tails
// the live stream.
func HandleTail(s stream.Stream) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		key := ParseKeyFromURL(r)

		h := w.Header()
		h.Set("Content-Type", "text/event-stream")
		h.Set("Cache-Control", "no-cache")
		h.Set("Connection", "keep-alive")
		h.Set("X-Accel-Buffering", "no")

		f, ok := w.(http.Flusher)
		if !ok {
			logger.FromRequest(r).
				Warnln("stream: request does not implement http.Flusher")
			return
		}

		io.WriteString(w, ": ping\n\n")
		f.Flush()

		ctx, cancel := context.WithCancel(r.Context())
		defer cancel()

		enc := json.NewEncoder(w)
		linec, errc := s.Tail(ctx, key)
		if errc == nil {
			io.WriteString(w, "event: error\ndata: eof\n\n")
			return
		}

	L:
		for {
			select {
			case <-ctx.Done():
				break L
			case <-errc:
				break L
			case <-time.After(time.Hour):
				break L
			case <-time.After(pingInterval):
				io.WriteString(w, ": ping\n\n")
				f.Flush()
			case line := <-linec:
				io.WriteString(w, "data: ")
				enc.Encode(line)
				io.WriteString(w, "\n\n")
				f.Flush()
			}
		}

		io.WriteString(w, "event: error\ndata: eof\n\n")
		f.Flush()
	}
}

// HandleInfo returns an http.HandlerFunc that writes the
// stream information to the http.Response.
func HandleInfo(stream stream.Stream) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		ctx := context.Background()
		inf := stream.Info(ctx)
		enc := json.NewEncoder(w)
		enc.SetIndent("", "  ")
		enc.Encode(inf)
	}
}
