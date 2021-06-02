// Package redis provides a log streaming engine backed by
// a Redis database
package redis

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"time"

	"github.com/wings-software/portal/product/log-service/stream"
	// TODO (vistaar): Move to redis v8. v8 accepts ctx in all calls.
	// There is some bazel issue with otel library with v8, need to move it once that is resolved.
	"github.com/go-redis/redis/v7"
	"github.com/hashicorp/go-multierror"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
)

const (
	keyExpiryTimeSeconds = 5 * 60 * 60 * (time.Second) // How long each key exists in redis
	// Polling time for each thread to wait for read before getting freed up. This should not be too large to avoid
	// redis clients getting occupied for long.
	readPollTime  = 100 * time.Millisecond
	tailMaxTime   = 1 * time.Hour // maximum duration a tail can last
	bufferSize    = 50            // buffer for slow consumers
	maxStreamSize = 5000          // Maximum number of entries in each stream (ring buffer)
	// max. number of concurrent connections that Redis can handle. This limit is set to 10k by default on the latest
	// Redis servers. To increase it, make sure it gets increased on the server side as well.
	connectionPool = 5000
	entryKey       = "line"
)

type Redis struct {
	Client redis.Cmdable
}

func New(endpoint, password string) *Redis {
	rdb := redis.NewClient(&redis.Options{
		Addr:     endpoint,
		Password: password,
		DB:       0,
		PoolSize: connectionPool,
	})
	return &Redis{
		Client: rdb,
	}
}

// Create creates a redis stream and sets an expiry on it.
func (r *Redis) Create(ctx context.Context, key string) error {
	// Delete if a stream already exists with the same key
	r.Delete(ctx, key)

	// Insert a dummy entry into the stream
	// Trimming with MaxLen can be expensive. We use MaxLenApprox here -
	// trimming is done in the radix tree only when we can remove a whole
	// macro node. MaxLen will always be >= 5000 but can be a few tens of entries
	// more as well.
	args := &redis.XAddArgs{
		Stream:       key,
		ID:           "*",
		MaxLenApprox: maxStreamSize,
		Values:       map[string]interface{}{entryKey: []byte{}},
	}
	resp := r.Client.XAdd(args)
	if err := resp.Err(); err != nil {
		return errors.Wrap(err, fmt.Sprintf("could not create stream with key: %s", key))
	}

	// Set a TTL for the stream
	res := r.Client.Expire(key, keyExpiryTimeSeconds)
	if err := res.Err(); err != nil {
		return errors.Wrap(err, fmt.Sprintf("could not set expiry for key: %s", key))
	}
	return nil
}

// Delete deletes a stream
func (r *Redis) Delete(ctx context.Context, key string) error {
	exists := r.Client.Exists(key)
	if exists.Err() != nil || exists.Val() == 0 {
		return stream.ErrNotFound
	}

	resp := r.Client.Del(key)
	if err := resp.Err(); err != nil {
		return errors.Wrap(err, fmt.Sprintf("could not delete stream with key: %s", key))
	}
	return nil
}

// Write writes information into the Redis stream
func (r *Redis) Write(ctx context.Context, key string, lines ...*stream.Line) error {
	var errors error
	exists := r.Client.Exists(key)
	if exists.Err() != nil || exists.Val() == 0 {
		return stream.ErrNotFound
	}

	// Write input to redis stream. "*" tells Redis to auto-generate a unique incremental ID.
	for _, line := range lines {
		bytes, _ := json.Marshal(line)
		arg := &redis.XAddArgs{
			Stream:       key,
			Values:       map[string]interface{}{entryKey: bytes},
			MaxLenApprox: maxStreamSize,
			ID:           "*",
		}
		resp := r.Client.XAdd(arg)
		if err := resp.Err(); err != nil {
			errors = multierror.Append(errors, err)
		}
	}
	return errors
}

// Read returns back all the lines in the stream. If tail is specifed as true, it keeps watching and doesn't
// close the channel.
func (r *Redis) Tail(ctx context.Context, key string) (<-chan *stream.Line, <-chan error) {
	handler := make(chan *stream.Line, bufferSize)
	err := make(chan error, 1)
	exists := r.Client.Exists(key)
	if exists.Err() != nil || exists.Val() == 0 {
		return nil, nil
	}
	go func() {
		// Keep reading from the stream and writing to the channel
		lastID := "0"
		defer close(err)
		defer close(handler)
		tailMaxTimeTimer := time.After(tailMaxTime) // polling should not last for longer than tailMaxTime
	L:
		for {
			select {
			case <-ctx.Done():
				break L
			case <-tailMaxTimeTimer:
				break L
			default:
				args := &redis.XReadArgs{
					Streams: append([]string{key}, lastID),
					Block:   readPollTime, // periodically check for ctx.Done
				}

				resp := r.Client.XRead(args)
				if resp.Err() != nil && resp.Err() != redis.Nil { // resp.Err() is sometimes set to "redis: nil" instead of nil
					logrus.WithError(resp.Err()).Errorln("received error on redis read call")
					err <- resp.Err()
					break L
				}

				for _, msg := range resp.Val() {
					b := msg.Messages
					if len(b) > 0 {
						lastID = b[len(b)-1].ID
					} else { // Should not happen
						break L
					}
					for _, message := range b {
						x := message.Values
						if val, ok := x[entryKey]; ok {
							var in *stream.Line
							if err := json.Unmarshal([]byte(val.(string)), &in); err != nil {
								// Ignore errors in the stream
								continue
							}
							handler <- in
						}
					}
				}
			}
		}
	}()
	return handler, err
}

// Exists checks whether the key exists in the stream
func (r *Redis) Exists(ctx context.Context, key string) error {
	exists := r.Client.Exists(key)
	if exists.Err() != nil || exists.Val() == 0 {
		return stream.ErrNotFound
	}
	return nil
}

// CopyTo copies the contents from the redis stream to the writer
func (r *Redis) CopyTo(ctx context.Context, key string, wc io.WriteCloser) error {
	defer wc.Close()
	exists := r.Client.Exists(key)
	if exists.Err() != nil || exists.Val() == 0 {
		return stream.ErrNotFound
	}

	lastID := "0"
	args := &redis.XReadArgs{
		Streams: append([]string{key}, lastID),
		Block:   readPollTime, // periodically check for ctx.Done
	}

	resp := r.Client.XRead(args)
	if resp.Err() != nil && resp.Err() != redis.Nil { // resp.Err() is sometimes set to "redis: nil" instead of nil
		logrus.WithError(resp.Err()).Errorln("received error on redis read call")
		return resp.Err()
	}

	for _, msg := range resp.Val() {
		b := msg.Messages
		if len(b) > 0 {
			lastID = b[len(b)-1].ID
		} else { // Should not happen
			break
		}
		for _, message := range b {
			x := message.Values
			if val, ok := x[entryKey]; ok && val.(string) != "" {
				wc.Write([]byte(val.(string)))
				wc.Write([]byte("\n"))
			}
		}
	}
	return nil
}

// Info returns back information like TTL, size of a stream
// NOTE: This is super slow for Redis and hogs up all the resources.
// TODO: (vistaar) Return only top x entries
func (r *Redis) Info(ctx context.Context) *stream.Info {
	resp := r.Client.Keys("*") // Get all keys
	info := &stream.Info{
		Streams: map[string]stream.Stats{},
	}
	for _, key := range resp.Val() {
		ttl := "-1" // default
		size := -1  // default
		ttlResp := r.Client.TTL(key)
		if err := ttlResp.Err(); err == nil {
			ttl = ttlResp.Val().String()
		}
		lenResp := r.Client.XLen(key)
		if err := lenResp.Err(); err == nil {
			size = int(lenResp.Val())
		}
		info.Streams[key] = stream.Stats{
			Size: size, // Note: this is not the actual number of lines. Each key-value pair consists of multiple lines.
			// This is done to prevent minimum number of calls to Redis.
			Subs: -1, // no sub information for redis streams
			TTL:  ttl,
		}
	}
	return info
}
