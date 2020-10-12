package logs

import (
	"context"
	"testing"
	"time"

	gomock "github.com/golang/mock/gomock"
	"github.com/pkg/errors"
	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/product/log-service/mock"
)

func Test_GetRemoteWriter_Success(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	mclient := mock.NewMockClient(ctrl)
	rw := NewRemoteWriter(mclient, "key")
	assert.NotEqual(t, rw, nil)
}

func Test_RemoteWriter_Open_Success(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	mclient := mock.NewMockClient(ctrl)
	mclient.EXPECT().Open(context.Background(), "key").Return(nil)
	rw := NewRemoteWriter(mclient, "key")
	err := rw.Open()
	assert.Equal(t, err, nil)
}

func Test_RemoteWriter_Open_Failure(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	mclient := mock.NewMockClient(ctrl)
	mclient.EXPECT().Open(context.Background(), "key").Return(errors.New("err"))
	rw := NewRemoteWriter(mclient, "key")
	err := rw.Open()
	assert.NotEqual(t, err, nil)
}

func Test_RemoteWriter_WriteSingleLine(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	msg := "Write data 1\n"
	key := "key"

	mclient := mock.NewMockClient(ctrl)
	mclient.EXPECT().Write(context.Background(), key, gomock.Any())
	rw := NewRemoteWriter(mclient, key)
	rw.SetInterval(time.Duration(100))
	rw.Write([]byte(msg))
	rw.flush() // Force write to the remote
	assert.Equal(t, len(rw.history), 1)
	assert.Equal(t, rw.history[0].Level, "info")
	assert.Equal(t, rw.history[0].Number, 0)
	assert.Equal(t, rw.history[0].Message, msg)
	assert.Equal(t, rw.history[0].Args, map[string]string{})
}

func Test_RemoteWriter_WriteMultiple(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	msg1 := "Write data 1\n"
	msg2 := "Write data 2\n"
	key := "key"

	mclient := mock.NewMockClient(ctrl)
	mclient.EXPECT().Write(context.Background(), key, gomock.Any())
	mclient.EXPECT().Write(context.Background(), key, gomock.Any())
	rw := NewRemoteWriter(mclient, key)
	rw.SetInterval(time.Duration(100))
	rw.Write([]byte(msg1))
	rw.flush() // Force write to the remote
	rw.Write([]byte(msg2))
	rw.flush() // Force write to the remote
	assert.Equal(t, len(rw.history), 2)
	// Ensure strict ordering
	assert.Equal(t, rw.history[0].Level, "info")
	assert.Equal(t, rw.history[0].Number, 0)
	assert.Equal(t, rw.history[0].Message, msg1)
	assert.Equal(t, rw.history[0].Args, map[string]string{})

	assert.Equal(t, rw.history[1].Level, "info")
	assert.Equal(t, rw.history[1].Number, 1)
	assert.Equal(t, rw.history[1].Message, msg2)
	assert.Equal(t, rw.history[1].Args, map[string]string{})
}

func Test_RemoteWriter_MultipleCharacters(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	// Commands like `mvn run` flush single characters multiple times. We ensure here
	// that lines are still created only with \n

	msg1 := "Write data 1"
	msg2 := "Write data 2"

	key := "key"

	mclient := mock.NewMockClient(ctrl)
	mclient.EXPECT().Write(context.Background(), key, gomock.Any())
	mclient.EXPECT().Write(context.Background(), key, gomock.Any())
	rw := NewRemoteWriter(mclient, key)
	rw.SetInterval(time.Duration(100))

	// Write character by character followed by new line
	for _, c := range msg1 {
		rw.Write([]byte(string(c)))
		rw.flush()
	}
	rw.Write([]byte("\n"))
	for _, c := range msg2 {
		rw.Write([]byte(string(c)))
		rw.flush()
	}
	rw.Write([]byte("\n"))
	rw.flush()

	assert.Equal(t, len(rw.history), 2)
	// Ensure strict ordering
	assert.Equal(t, rw.history[0].Level, "info")
	assert.Equal(t, rw.history[0].Number, 0)
	assert.Equal(t, rw.history[0].Message, msg1+"\n")
	assert.Equal(t, rw.history[0].Args, map[string]string{})

	assert.Equal(t, rw.history[1].Level, "info")
	assert.Equal(t, rw.history[1].Number, 1)
	assert.Equal(t, rw.history[1].Message, msg2+"\n")
	assert.Equal(t, rw.history[1].Args, map[string]string{})
}

func Test_RemoteWriter_JSON(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	key := "key"

	msg1 := `{"level":"warn","msg":"Testing","k1":"v1","k2":"v2"}`
	msg1 = msg1 + "\n"

	mclient := mock.NewMockClient(ctrl)
	mclient.EXPECT().Write(context.Background(), key, gomock.Any())
	rw := NewRemoteWriter(mclient, key)
	rw.SetInterval(time.Duration(100))

	rw.Write([]byte(msg1))
	rw.flush()

	assert.Equal(t, len(rw.history), 1)
	// Ensure strict ordering
	assert.Equal(t, rw.history[0].Level, "warn")
	assert.Equal(t, rw.history[0].Number, 0)
	assert.Equal(t, rw.history[0].Message, "Testing")
	assert.Equal(t, rw.history[0].Args, map[string]string{"level": "warn", "msg": "Testing", "k1": "v1", "k2": "v2"})
}

func Test_RemoteWriter_Close(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	key := "key"

	msg1 := `{"level":"warn","msg":"Testing","k1":"v1","k2":"v2"}`
	msg1 = msg1 + "\n"
	msg2 := "Another message" // Ensure this gets flushed on close

	mclient := mock.NewMockClient(ctrl)
	mclient.EXPECT().Write(context.Background(), key, gomock.Any())
	mclient.EXPECT().Upload(context.Background(), key, gomock.Any())
	mclient.EXPECT().Close(context.Background(), key)
	rw := NewRemoteWriter(mclient, key)
	rw.SetInterval(time.Duration(100))

	rw.Write([]byte(msg1))
	rw.flush()

	rw.Write([]byte(msg2))
	rw.flush()

	assert.Equal(t, rw.prev, []byte(msg2))

	assert.Equal(t, len(rw.history), 1)
	// Ensure strict ordering
	assert.Equal(t, rw.history[0].Level, "warn")
	assert.Equal(t, rw.history[0].Number, 0)
	assert.Equal(t, rw.history[0].Message, "Testing")
	assert.Equal(t, rw.history[0].Args, map[string]string{"level": "warn", "msg": "Testing", "k1": "v1", "k2": "v2"})

	rw.Close()
	assert.Equal(t, rw.prev, []byte{}) // Ensure existing data gets flushed
	assert.Equal(t, rw.closed, true)
}
