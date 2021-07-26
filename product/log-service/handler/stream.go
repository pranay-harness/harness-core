package handler

import (
	"bufio"
	"context"
	"encoding/json"
	"io"
	"net/http"
	"time"

	"github.com/wings-software/portal/product/log-service/logger"
	"github.com/wings-software/portal/product/log-service/store"
	"github.com/wings-software/portal/product/log-service/stream"

	"golang.org/x/sync/errgroup"
)

var pingInterval = time.Second * 30
var tailMaxTime = time.Hour * 1

// BufioWriterCloser combines a bufio Writer with a Closer
type BufioWriterCloser struct {
	wc io.Closer
	*bufio.Writer
}

func (bwc *BufioWriterCloser) Close() error {
	if err := bwc.Flush(); err != nil {
		return err
	}
	return bwc.wc.Close()
}

// HandleOpen returns an http.HandlerFunc that opens
// the live stream.
func HandleOpen(stream stream.Stream) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		ctx := r.Context()

		accountID := r.FormValue(accountIDParam)
		key := CreateAccountSeparatedKey(accountID, r.FormValue(keyParam))

		if err := stream.Create(ctx, key); err != nil {
			WriteInternalError(w, err)
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
// the live stream and optionally snapshots the stream.
func HandleClose(logStream stream.Stream, store store.Store) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		ctx := r.Context()

		accountID := r.FormValue(accountIDParam)
		key := CreateAccountSeparatedKey(accountID, r.FormValue(keyParam))

		snapshot := r.FormValue(snapshotParam)
		if snapshot == "true" {
			if err := logStream.Exists(ctx, key); err != nil {
				WriteInternalError(w, err)
				logger.FromRequest(r).
					WithError(err).
					WithField("key", key).
					Errorln("api: key does not exist")
				return
			}
			pr, pw := io.Pipe()
			defer pr.Close()
			br := bufio.NewReader(pr)
			bwc := &BufioWriterCloser{pw, bufio.NewWriter(pw)}

			g := new(errgroup.Group)
			g.Go(func() error {
				return logStream.CopyTo(ctx, key, bwc)
			})

			g.Go(func() error {
				return store.Upload(ctx, key, br)
			})

			if err := g.Wait(); err != nil {
				WriteInternalError(w, err)
				logger.FromRequest(r).
					WithError(err).
					WithField("key", key).
					Errorln("api: could not snapshot stream to store")
				return // don't delete the stream if snapshotting failed
			}
		}

		if err := logStream.Delete(ctx, key); err != nil {
			WriteInternalError(w, err)
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

		accountID := r.FormValue(accountIDParam)
		key := CreateAccountSeparatedKey(accountID, r.FormValue(keyParam))

		in := []*stream.Line{}
		if err := json.NewDecoder(r.Body).Decode(&in); err != nil {
			WriteBadRequest(w, err)
			logger.FromRequest(r).
				WithError(err).
				WithField("key", key).
				Errorln("api: cannot unmarshal input")
			return
		}

		if err := s.Write(ctx, key, in...); err != nil {
			if err != nil {
				WriteInternalError(w, err)
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

// HandlePing returns an http.HandlerFunc that pings
// the live stream.
func HandlePing(s stream.Stream) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		ctx := r.Context()

		if err := s.Ping(ctx); err != nil {
			if err != nil {
				WriteInternalError(w, err)
				logger.FromRequest(r).
					WithError(err).
					Errorln("api: cannot ping the stream")
				return
			}
		}

		io.WriteString(w, "OK")
	}
}

// HandleTail returns an http.HandlerFunc that tails
// the live stream.
func HandleTail(s stream.Stream) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		accountID := r.FormValue(accountIDParam)
		key := CreateAccountSeparatedKey(accountID, r.FormValue(keyParam))

		h := w.Header()
		h.Set("Content-Type", "text/event-stream")
		h.Set("Cache-Control", "no-cache")
		h.Set("Connection", "keep-alive")
		h.Set("X-Accel-Buffering", "no")
		h.Set("Access-Control-Allow-Origin", "*")

		f, ok := w.(http.Flusher)
		if !ok {
			logger.FromRequest(r).
				Warnln("stream: request does not implement http.Flusher")
			return
		}

		io.WriteString(w, ": ping\n")
		f.Flush()

		ctx, cancel := context.WithCancel(r.Context())
		defer cancel()

		enc := json.NewEncoder(w)
		linec, errc := s.Tail(ctx, key)
		if errc == nil {
			io.WriteString(w, "event: error\ndata: eof\n")
			return
		}

		tailMaxTimeTimer := time.After(tailMaxTime)
		msgDelayTimer := time.NewTimer(pingInterval) // if time b/w messages takes longer, send a ping
		defer msgDelayTimer.Stop()

	L:
		for {
			msgDelayTimer.Reset(pingInterval)
			select {
			case <-ctx.Done():
				break L
			case <-errc:
				break L
			case <-tailMaxTimeTimer:
				break L
			case <-msgDelayTimer.C:
				io.WriteString(w, ": ping\n")
				f.Flush()
			case line := <-linec:
				io.WriteString(w, "data: ")
				enc.Encode(line)
				io.WriteString(w, "\n")
				f.Flush()
			}
		}

		io.WriteString(w, "event: error\ndata: eof\n")
		f.Flush()
	}
}

// HandleInfo returns an http.HandlerFunc that writes the
// stream information to the http.Response.
func HandleInfo(stream stream.Stream) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		h := w.Header()
		h.Set("Access-Control-Allow-Origin", "*")
		ctx := context.Background()
		inf := stream.Info(ctx)
		enc := json.NewEncoder(w)
		enc.SetIndent("", "  ")
		enc.Encode(inf)
	}
}
