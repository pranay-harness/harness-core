package logs

import (
	"context"
	"testing"

	gomock "github.com/golang/mock/gomock"
	"github.com/pkg/errors"
	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/product/log-service/mock"
)

func Test_GetRemoteLogger_Success(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	mclient := mock.NewMockClient(ctrl)
	mclient.EXPECT().Open(context.Background(), "key").Return(nil)
	writer := NewRemoteWriter(mclient, "key")
	_, err := NewRemoteLogger(writer)
	assert.Equal(t, err, nil)
}

func Test_GetRemoteLogger_Failure(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	mclient := mock.NewMockClient(ctrl)
	mclient.EXPECT().Open(context.Background(), "key").Return(errors.New("err"))
	writer := NewRemoteWriter(mclient, "key")
	_, err := NewRemoteLogger(writer)
	assert.NotEqual(t, err, nil)
}
