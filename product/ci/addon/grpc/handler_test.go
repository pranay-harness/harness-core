package grpc

import (
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/logs"
	pb "github.com/wings-software/portal/product/ci/addon/proto"
	"go.uber.org/zap"
	"golang.org/x/net/context"
)

func Test_handler_PublishArtifacts(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	h := NewCIAddonHandler(log.Sugar())
	taskID := "test-task"

	tests := []struct {
		name        string
		input       *pb.PublishArtifactsRequest
		expectedErr bool
	}{
		{
			name:        "validation failed",
			input:       nil,
			expectedErr: true,
		},
		{
			name: "success",
			input: &pb.PublishArtifactsRequest{
				TaskId: &pb.TaskId{Id: taskID},
			},
			expectedErr: false,
		},
	}
	for _, tc := range tests {
		_, got := h.PublishArtifacts(ctx, tc.input)
		if tc.expectedErr == (got == nil) {
			t.Fatalf("%s: expected error: %v, got: %v", tc.name, tc.expectedErr, got)
		}
	}
}

func Test_TaskProgress(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	h := NewCIAddonHandler(log.Sugar())
	_, err := h.TaskProgress(ctx, nil)
	assert.Nil(t, err)
}

func Test_TaskProgressUpdates(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	h := NewCIAddonHandler(log.Sugar())
	err := h.TaskProgressUpdates(nil, nil)
	assert.Nil(t, err)
}
