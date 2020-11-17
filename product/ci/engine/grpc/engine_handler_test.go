package grpc

import (
	"context"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/logs"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

func TestUpdateUnknownStatus(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	arg := &pb.UpdateStateRequest{}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	h := NewEngineHandler(log.Sugar())
	_, err := h.UpdateState(ctx, arg)
	assert.NotNil(t, err)
}

func TestUpdateToPause(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	arg := &pb.UpdateStateRequest{
		Action: pb.UpdateStateRequest_PAUSE,
	}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	h := NewEngineHandler(log.Sugar())
	_, err := h.UpdateState(ctx, arg)
	assert.Nil(t, err)
}

func TestUpdateToResume(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	arg := &pb.UpdateStateRequest{
		Action: pb.UpdateStateRequest_RESUME,
	}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	h := NewEngineHandler(log.Sugar())
	_, err := h.UpdateState(ctx, arg)
	assert.Nil(t, err)
}
