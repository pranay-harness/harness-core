// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package redis

import (
	"bufio"
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"log"
	"os"
	"testing"
	"time"

	"github.com/alicebob/miniredis/v2"
	"github.com/elliotchance/redismock/v7"
	"github.com/go-redis/redis/v7"
	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/product/log-service/stream"
)

var (
	client *redis.Client
)

// BufioWriterCloser combines a bufio Writer with a Closer
type BufioWriterCloser struct {
	*bufio.Writer
}

func (bwc *BufioWriterCloser) Close() error {
	if err := bwc.Flush(); err != nil {
		return err
	}
	return nil
}

func TestMain(m *testing.M) {
	mr, err := miniredis.Run()
	if err != nil {
		log.Fatalf("an error '%s' was not expected when opening a stub database connection", err)
	}
	defer mr.Close()

	client = redis.NewClient(&redis.Options{
		Addr: mr.Addr(),
	})

	code := m.Run()
	os.Exit(code)
}

func TestCreate(t *testing.T) {
	ctx := context.Background()
	key := "key"

	mr, err := miniredis.Run()
	if err != nil {
		log.Fatalf("an error '%s' was not expected when opening a stub database connection", err)
	}
	defer mr.Close()

	client = redis.NewClient(&redis.Options{
		Addr: mr.Addr(),
	})

	rdb := &Redis{
		Client: client,
	}

	rdb.Create(ctx, key)

	assert.Equal(t, mr.Exists(key), true)
}

func TestCreate_Error(t *testing.T) {

	ctx := context.Background()
	key := "key"

	var client *redis.Client
	mock := redismock.NewNiceMock(client)
	args := &redis.XAddArgs{
		Stream:       key,
		ID:           "*",
		MaxLenApprox: maxStreamSize,
		Values:       map[string]interface{}{entryKey: []byte{}},
	}
	mock.On("Exists", []string{key}).Return(redis.NewIntResult(1, nil))
	mock.On("Del", []string{key}).Return(redis.NewIntResult(1, nil))
	mock.On("XAdd", args).Return(redis.NewStringResult("failed", errors.New("err")))

	rdb := &Redis{
		Client: mock,
	}
	err := rdb.Create(ctx, key)
	assert.NotEqual(t, err, nil)
}

func TestDelete(t *testing.T) {
	ctx := context.Background()
	key := "key"

	mr, err := miniredis.Run()
	if err != nil {
		log.Fatalf("an error '%s' was not expected when opening a stub database connection", err)
	}
	defer mr.Close()

	client = redis.NewClient(&redis.Options{
		Addr: mr.Addr(),
	})

	rdb := &Redis{
		Client: client,
	}

	mr.XAdd(key, "*", []string{"k1", "v1"})

	err = rdb.Delete(ctx, key)

	assert.Equal(t, mr.Exists(key), false)
	assert.Equal(t, err, nil)
}

func TestDelete_DoesntExist(t *testing.T) {
	ctx := context.Background()
	key := "key"

	mr, err := miniredis.Run()
	if err != nil {
		log.Fatalf("an error '%s' was not expected when opening a stub database connection", err)
	}
	defer mr.Close()

	client = redis.NewClient(&redis.Options{
		Addr: mr.Addr(),
	})

	rdb := &Redis{
		Client: client,
	}
	err = rdb.Delete(ctx, key)
	assert.Equal(t, mr.Exists(key), false)
	assert.Equal(t, err, stream.ErrNotFound)
}

func TestWrite_Success(t *testing.T) {
	ctx := context.Background()
	key := "key"

	var client *redis.Client
	mock := redismock.NewNiceMock(client)
	line1 := &stream.Line{Level: "info", Number: 0, Message: "test message"}
	line2 := &stream.Line{Level: "warn", Number: 1, Message: "test message2"}
	bytes1, _ := json.Marshal(&line1)
	bytes2, _ := json.Marshal(&line2)
	args1 := &redis.XAddArgs{
		Stream:       key,
		ID:           "*",
		MaxLenApprox: maxStreamSize,
		Values:       map[string]interface{}{entryKey: bytes1},
	}
	args2 := &redis.XAddArgs{
		Stream:       key,
		ID:           "*",
		MaxLenApprox: maxStreamSize,
		Values:       map[string]interface{}{entryKey: bytes2},
	}
	mock.On("Exists", []string{key}).Return(redis.NewIntResult(1, nil))
	mock.On("XAdd", args1).Return(redis.NewStringResult("success", nil))
	mock.On("XAdd", args2).Return(redis.NewStringResult("success", nil))
	mock.On("TTL", key).Return(redis.NewDurationResult(20*time.Second, errors.New("could not get ttl")))

	rdb := &Redis{
		Client: mock,
	}
	err := rdb.Write(ctx, key, line1, line2)
	assert.Equal(t, err, nil)
}

func TestWrite_Failure(t *testing.T) {
	ctx := context.Background()
	key := "key"

	var client *redis.Client
	mock := redismock.NewNiceMock(client)
	line1 := &stream.Line{Level: "info", Number: 0, Message: "test message"}
	line2 := &stream.Line{Level: "warn", Number: 1, Message: "test message2"}
	line3 := &stream.Line{Level: "error", Number: 2, Message: "test message3"}
	bytes1, _ := json.Marshal(&line1)
	bytes2, _ := json.Marshal(&line2)
	bytes3, _ := json.Marshal(&line3)
	args1 := &redis.XAddArgs{
		Stream:       key,
		ID:           "*",
		MaxLenApprox: maxStreamSize,
		Values:       map[string]interface{}{entryKey: bytes1},
	}
	args2 := &redis.XAddArgs{
		Stream:       key,
		ID:           "*",
		MaxLenApprox: maxStreamSize,
		Values:       map[string]interface{}{entryKey: bytes2},
	}
	args3 := &redis.XAddArgs{
		Stream:       key,
		ID:           "*",
		MaxLenApprox: maxStreamSize,
		Values:       map[string]interface{}{entryKey: bytes3},
	}
	mock.On("Exists", []string{key}).Return(redis.NewIntResult(1, nil))
	mock.On("XAdd", args1).Return(redis.NewStringResult("success", nil))
	mock.On("XAdd", args2).Return(redis.NewStringResult("", errors.New("err")))
	mock.On("XAdd", args3).Return(redis.NewStringResult("success", nil))
	mock.On("TTL", key).Return(redis.NewDurationResult(20*time.Second, errors.New("could not get ttl")))
	rdb := &Redis{
		Client: mock,
	}
	err := rdb.Write(ctx, key, line1, line2, line3)
	assert.NotEqual(t, err, nil)
}

func TestTail_Single_Success(t *testing.T) {
	ctx, cancel := context.WithCancel(context.Background())
	key := "key"

	var client *redis.Client
	mock := redismock.NewNiceMock(client)
	line1 := &stream.Line{Level: "info", Number: 0, Message: "test message"}
	line2 := &stream.Line{Level: "warn", Number: 1, Message: "test message2"}
	bytes1, _ := json.Marshal(&line1)
	bytes2, _ := json.Marshal(&line2)
	args := &redis.XReadArgs{
		Streams: append([]string{key}, "0"),
		Block:   readPollTime,
	}

	stream := []redis.XStream{
		{
			Stream: key,
			Messages: []redis.XMessage{
				{ID: "1", Values: map[string]interface{}{entryKey: string(bytes1)}},
				{ID: "2", Values: map[string]interface{}{entryKey: string(bytes2)}},
			},
		},
	}

	streamSlice := redis.NewXStreamSliceCmdResult(stream, nil)
	mock.On("Exists", []string{key}).Return(redis.NewIntResult(1, nil))
	mock.On("XRead", args).Return(streamSlice).After(1 * time.Second)

	rdb := &Redis{
		Client: mock,
	}
	linec, _ := rdb.Tail(ctx, key)
	time.Sleep(100 * time.Millisecond)
	cancel()
	lineResponse1 := <-linec
	assert.Equal(t, lineResponse1, line1)
	lineResponse2 := <-linec
	assert.Equal(t, lineResponse2, line2)
}

func TestTail_Multiple(t *testing.T) {
	ctx, _ := context.WithCancel(context.Background())
	key := "key"

	var client *redis.Client
	mock := redismock.NewNiceMock(client)
	line1 := &stream.Line{Level: "info", Number: 0, Message: "test message"}
	line2 := &stream.Line{Level: "warn", Number: 1, Message: "test message2"}
	bytes1, _ := json.Marshal(&line1)
	bytes2, _ := json.Marshal(&line2)

	args1 := &redis.XReadArgs{
		Streams: append([]string{key}, "0"),
		Block:   readPollTime,
	}
	args2 := &redis.XReadArgs{
		Streams: append([]string{key}, "1"),
		Block:   readPollTime,
	}

	stream := []redis.XStream{
		{
			Stream: key,
			Messages: []redis.XMessage{
				{ID: "0", Values: map[string]interface{}{entryKey: string(bytes1)}},
				{ID: "1", Values: map[string]interface{}{entryKey: string(bytes2)}},
			},
		},
	}

	streamSlice1 := redis.NewXStreamSliceCmdResult(stream, nil)
	streamSlice2 := redis.NewXStreamSliceCmdResult(nil, errors.New("err"))
	mock.On("Exists", []string{key}).Return(redis.NewIntResult(1, nil))
	mock.On("XRead", args1).Return(streamSlice1)
	mock.On("XRead", args2).Return(streamSlice2)

	rdb := &Redis{
		Client: mock,
	}

	linec, errc := rdb.Tail(ctx, key)
	lineResponse1 := <-linec
	assert.Equal(t, lineResponse1, line1)
	lineResponse2 := <-linec
	assert.Equal(t, lineResponse2, line2)
	err := <-errc
	assert.Equal(t, err, errors.New("err"))
}

func TestTail_Failure(t *testing.T) {
	ctx := context.Background()
	key := "key"

	var client *redis.Client
	mock := redismock.NewNiceMock(client)
	line1 := &stream.Line{Level: "info", Number: 0, Message: "test message"}
	line2 := &stream.Line{Level: "warn", Number: 1, Message: "test message2"}
	lines := []*stream.Line{line1, line2}
	bytes, _ := json.Marshal(&lines)
	args := &redis.XReadArgs{
		Streams: append([]string{key}, "0"),
		Block:   readPollTime,
	}

	stream := []redis.XStream{
		{
			Stream: key,
			Messages: []redis.XMessage{
				{ID: "1", Values: map[string]interface{}{entryKey: string(bytes)}},
			},
		},
	}

	streamSlice := redis.NewXStreamSliceCmdResult(stream, errors.New("err"))
	mock.On("Exists", []string{key}).Return(redis.NewIntResult(1, nil))
	mock.On("XRead", args).Return(streamSlice)

	rdb := &Redis{
		Client: mock,
	}
	_, errc := rdb.Tail(ctx, key)
	err := <-errc
	assert.Equal(t, err, errors.New("err"))
}

func TestCopyTo(t *testing.T) {
	ctx, _ := context.WithCancel(context.Background())
	key := "key"

	var client *redis.Client
	mock := redismock.NewNiceMock(client)
	line1 := &stream.Line{Level: "info", Number: 0, Message: "test message"}
	line2 := &stream.Line{Level: "warn", Number: 1, Message: "test message1"}
	bytes1, _ := json.Marshal(&line1)
	bytes2, _ := json.Marshal(&line2)
	args := &redis.XReadArgs{
		Streams: append([]string{key}, "0"),
		Block:   readPollTime,
	}

	stream := []redis.XStream{
		{
			Stream: key,
			Messages: []redis.XMessage{
				{ID: "0", Values: map[string]interface{}{entryKey: string(bytes1)}},
				{ID: "1", Values: map[string]interface{}{entryKey: string(bytes2)}},
			},
		},
	}

	streamSlice := redis.NewXStreamSliceCmdResult(stream, nil)
	mock.On("Exists", []string{key}).Return(redis.NewIntResult(1, nil))
	mock.On("XRead", args).Return(streamSlice)

	rdb := &Redis{
		Client: mock,
	}
	w := new(bytes.Buffer)
	bwc := BufioWriterCloser{bufio.NewWriter(w)}
	err := rdb.CopyTo(ctx, key, &bwc)

	assert.Equal(t, err, nil)
	bytes1 = append(bytes1, []byte("\n")...)
	bytes2 = append(bytes2, []byte("\n")...)
	assert.Equal(t, w.Bytes(), append(bytes1, bytes2...))
}

func TestInfo(t *testing.T) {
	ctx := context.Background()
	key := "key"

	mr, err := miniredis.Run()
	if err != nil {
		log.Fatalf("an error '%s' was not expected when opening a stub database connection", err)
	}
	defer mr.Close()

	client = redis.NewClient(&redis.Options{
		Addr: mr.Addr(),
	})

	rdb := &Redis{
		Client: client,
	}
	mr.XAdd(key, "*", []string{"k1", "v1"})

	info := rdb.Info(ctx)
	assert.Equal(t, info.Streams[key].Size, 1)
	assert.Equal(t, info.Streams[key].Subs, -1) // No subs information for Redis
}

func TestExists(t *testing.T) {
	ctx := context.Background()
	key := "key"

	mr, err := miniredis.Run()
	if err != nil {
		log.Fatalf("an error '%s' was not expected when opening a stub database connection", err)
	}
	defer mr.Close()

	client = redis.NewClient(&redis.Options{
		Addr: mr.Addr(),
	})

	rdb := &Redis{
		Client: client,
	}

	mr.XAdd(key, "*", []string{"k1", "v1"})
	assert.Equal(t, mr.Exists(key), true)
	assert.Nil(t, rdb.Exists(ctx, key))
	err = rdb.Delete(ctx, key)

	assert.Equal(t, mr.Exists(key), false)
	assert.NotNil(t, rdb.Exists(ctx, key))
}

func TestListPrefixes(t *testing.T) {
	ctx := context.Background()

	mr, err := miniredis.Run()
	if err != nil {
		log.Fatalf("an error '%s' was not expected when opening a stub database connection", err)
	}
	defer mr.Close()

	client = redis.NewClient(&redis.Options{
		Addr: mr.Addr(),
	})

	rdb := &Redis{
		Client: client,
	}

	mr.XAdd("key1", "*", []string{"k1", "v1"})
	mr.XAdd("key2", "*", []string{"k1", "v1"})
	mr.XAdd("differentPrefix", "*", []string{"k1", "v1"})

	l, err := rdb.ListPrefix(ctx, "key")
	assert.Nil(t, err)
	assert.Equal(t, len(l), 2)
	assert.Contains(t, l, "key1")
	assert.Contains(t, l, "key2")
}
