package grpc

import (
	"context"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/logs"
	mexecutor "github.com/wings-software/portal/product/ci/engine/executor/mocks"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

func Test_SignalStop(t *testing.T) {
	tmpPath := "/tmp"
	stopCh := make(chan bool)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	h := NewLiteEngineHandler(tmpPath, tmpPath, stopCh, log.Sugar())
	_, err := h.SignalStop(nil, nil)
	assert.Nil(t, err)
}

func Test_ExecuteStep(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	mockExecutor := mexecutor.NewMockUnitExecutor(ctrl)

	in := &pb.ExecuteStepRequest{}
	tmpPath := "/tmp"
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	stopCh := make(chan bool)
	h := &handler{tmpPath, tmpPath, stopCh, mockExecutor, log.Sugar()}

	mockExecutor.EXPECT().Run(ctx, in.GetStep()).Return(nil)
	_, err := h.ExecuteStep(ctx, in)
	assert.Nil(t, err)
}
