package redis

import (
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
		Values:       map[string]interface{}{"lines": []byte{}},
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

func TestWrite(t *testing.T) {
	ctx := context.Background()
	key := "key"

	var client *redis.Client
	mock := redismock.NewNiceMock(client)
	line1 := &stream.Line{Level: "info", Number: 0, Message: "test message"}
	line2 := &stream.Line{Level: "warn", Number: 1, Message: "test message2"}
	lines := []*stream.Line{line1, line2}
	bytes, _ := json.Marshal(&lines)
	args := &redis.XAddArgs{
		Stream:       key,
		ID:           "*",
		MaxLenApprox: maxStreamSize,
		Values:       map[string]interface{}{"lines": bytes},
	}
	mock.On("Exists", []string{key}).Return(redis.NewIntResult(1, nil))
	mock.On("XAdd", args).Return(redis.NewStringResult("success", nil))

	rdb := &Redis{
		Client: mock,
	}
	err := rdb.Write(ctx, key, line1, line2)
	assert.Equal(t, err, nil)
}

func TestTail_Single_Success(t *testing.T) {
	ctx, cancel := context.WithCancel(context.Background())
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
				{ID: "1", Values: map[string]interface{}{"lines": string(bytes)}},
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
	lines := []*stream.Line{line1, line2}
	bytes, _ := json.Marshal(&lines)

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
				{ID: "1", Values: map[string]interface{}{"lines": string(bytes)}},
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
				{ID: "1", Values: map[string]interface{}{"lines": string(bytes)}},
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
