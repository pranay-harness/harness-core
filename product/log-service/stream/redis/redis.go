// Package redis provides a log streaming engine backed by
// a Redis database
package redis

import (
	"context"
	"encoding/json"
	"fmt"
	"time"

	// TODO (vistaar): Move to redis v8. v8 accepts ctx in all calls.
	// There is some bazel issue with otel library with v8, need to move it once that is resolved.
	"github.com/go-redis/redis/v7"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"github.com/wings-software/portal/product/log-service/stream"
)

// this is how long each key exists in Redis.
const (
	keyExpiryTimeSeconds = 5 * 60 * 60 * (time.Second)
	redisWaitTime        = 10 * time.Second
	tailMaxTime          = 1 * time.Hour
	bufferSize           = 50
)

type Redis struct {
	Client redis.Cmdable
}

func New(endpoint, password string) *Redis {
	rdb := redis.NewClient(&redis.Options{
		Addr:     endpoint,
		Password: password,
		DB:       0,
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
	args := &redis.XAddArgs{
		Stream: key,
		ID:     "*",
		Values: map[string]interface{}{"lines": []byte{}},
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
	exists := r.Client.Exists(key)
	if exists.Err() != nil || exists.Val() == 0 {
		return stream.ErrNotFound
	}

	bytes, err := json.Marshal(lines)
	if err != nil {
		return err
	}

	// Write input to redis stream. "*" tells Redis to auto-generate a unique incremental ID.
	args := &redis.XAddArgs{
		Stream: key,
		Values: map[string]interface{}{"lines": bytes},
		ID:     "*",
	}
	resp := r.Client.XAdd(args)
	if err := resp.Err(); err != nil {
		return errors.Wrap(err, fmt.Sprintf("could not write to stream with key: %s", key))
	}
	return nil
}

// Tail returns back all the lines in the stream and watches for new lines
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
	L:
		for {
			select {
			case <-ctx.Done():
				break L
			case <-time.After(tailMaxTime):
				break L
			default:
				args := &redis.XReadArgs{
					Streams: append([]string{key}, lastID),
					Block:   redisWaitTime, // periodically check for ctx.Done
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
						if val, ok := x["lines"]; ok {
							var in []*stream.Line
							err := json.Unmarshal([]byte(val.(string)), &in)
							if err != nil {
								// Ignore errors in the stream
								continue
							}
							for _, line := range in {
								handler <- line
							}
						}
					}
				}
			}
		}
	}()
	return handler, err
}

// Info returns back information like TTL, size of a stream
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
