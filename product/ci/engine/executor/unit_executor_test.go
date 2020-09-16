package executor

import (
	"context"
	"encoding/base64"
	"testing"
	"time"

	"github.com/golang/mock/gomock"
	"github.com/golang/protobuf/proto"
	"github.com/pkg/errors"
	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/filesystem"
	"github.com/wings-software/portal/commons/go/lib/logs"
	pb2 "github.com/wings-software/portal/product/ci/addon/proto"
	"github.com/wings-software/portal/product/ci/engine/output"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"github.com/wings-software/portal/product/ci/engine/steps"
	msteps "github.com/wings-software/portal/product/ci/engine/steps/mocks"
	"go.uber.org/zap"
)

func getEncodedExecuteStepProto(t *testing.T, r *pb.ExecuteStepRequest) string {
	data, err := proto.Marshal(r)
	if err != nil {
		t.Fatalf("marshaling error: %v", err)
	}
	return base64.StdEncoding.EncodeToString(data)
}

func TestStepRun(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	accountID := "test"
	callbackToken := "token"
	taskID := "taskID"
	tmpFilePath := "/tmp"
	stepProto1 := &pb.UnitStep{
		Step: &pb.UnitStep_Run{
			Run: &pb.RunStep{},
		},
	}
	stepProto2 := &pb.UnitStep{
		Id: "test1",
		Step: &pb.UnitStep_Run{
			Run: &pb.RunStep{},
		},
	}
	stepProto3 := &pb.UnitStep{
		Id: "test1",
	}

	stepProto4 := &pb.UnitStep{
		Id:            "test1",
		CallbackToken: callbackToken,
	}

	stepProto5 := &pb.UnitStep{
		Id:            "test1",
		CallbackToken: callbackToken,
		TaskId:        taskID,
	}

	logPath := "/a/b"
	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	tests := []struct {
		name        string
		step        *pb.UnitStep
		logPath     string
		expectedErr bool
	}{
		{
			name:        "step ID is not set",
			step:        stepProto1,
			logPath:     logPath,
			expectedErr: true,
		},
		{
			name:        "empty log path",
			step:        stepProto2,
			logPath:     "",
			expectedErr: true,
		},
		{
			name:        "stage with error in run step",
			step:        stepProto2,
			logPath:     logPath,
			expectedErr: true,
		},
		{
			name:        "callback token is not set",
			step:        stepProto3,
			logPath:     logPath,
			expectedErr: true,
		},
		{
			name:        "task ID is not set",
			step:        stepProto4,
			logPath:     logPath,
			expectedErr: true,
		},
		{
			name:        "empty step success",
			step:        stepProto5,
			logPath:     logPath,
			expectedErr: false,
		},
	}

	oldSendStepStatus := sendStepStatus
	defer func() { sendStepStatus = oldSendStepStatus }()
	sendStepStatus = func(ctx context.Context, accountID, callbackToken, taskID string, numRetries int32, timeTaken time.Duration,
		stepOutput *output.StepOutput, stepErr error, log *zap.SugaredLogger) error {
		return nil
	}
	for _, tc := range tests {
		e := NewUnitExecutor(tc.logPath, tmpFilePath, log.Sugar())
		_, got := e.Run(ctx, tc.step, nil, accountID)
		if tc.expectedErr == (got == nil) {
			t.Fatalf("%s: expected error: %v, got: %v", tc.name, tc.expectedErr, got)
		}
	}
}

func TestStepSaveCacheError(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	accountID := "test"
	callbackToken := "token"
	taskID := "taskID"
	tmpFilePath := "/tmp"
	stepProto := &pb.UnitStep{
		Id: "test2",
		Step: &pb.UnitStep_SaveCache{
			SaveCache: &pb.SaveCacheStep{
				Key:   "key",
				Paths: []string{"/tmp/m2"},
			},
		},
		CallbackToken: callbackToken,
		TaskId:        taskID,
	}
	logPath := "/a/b"
	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	oldSendStepStatus := sendStepStatus
	defer func() { sendStepStatus = oldSendStepStatus }()
	sendStepStatus = func(ctx context.Context, accountID, callbackToken, taskID string, numRetries int32, timeTaken time.Duration,
		stepOutput *output.StepOutput, stepErr error, log *zap.SugaredLogger) error {
		return nil
	}

	mockStep := msteps.NewMockSaveCacheStep(ctrl)
	mockStep.EXPECT().Run(ctx).Return(nil, errors.New("caching failed"))

	oldStep := saveCacheStep
	defer func() { saveCacheStep = oldStep }()
	saveCacheStep = func(step *pb.UnitStep, tmpFilePath string, so output.StageOutput,
		fs filesystem.FileSystem, log *zap.SugaredLogger) steps.SaveCacheStep {
		return mockStep
	}

	e := NewUnitExecutor(logPath, tmpFilePath, log.Sugar())
	_, err := e.Run(ctx, stepProto, nil, accountID)
	assert.NotEqual(t, err, nil)
}

func TestStepSaveCacheSuccess(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	accountID := "test"
	taskID := "taskID"
	callbackToken := "token"
	tmpFilePath := "/tmp"
	key := "key"
	stepProto := &pb.UnitStep{
		Id: "test2",
		Step: &pb.UnitStep_SaveCache{
			SaveCache: &pb.SaveCacheStep{
				Key:   key,
				Paths: []string{"/tmp/m2"},
			},
		},
		CallbackToken: callbackToken,
		TaskId:        taskID,
	}
	logPath := "/a/b"
	o := &output.StepOutput{
		Output: map[string]string{"key": key},
	}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	mockStep := msteps.NewMockSaveCacheStep(ctrl)

	oldSendStepStatus := sendStepStatus
	defer func() { sendStepStatus = oldSendStepStatus }()
	sendStepStatus = func(ctx context.Context, accountID, callbackToken, taskID string, numRetries int32, timeTaken time.Duration,
		stepOutput *output.StepOutput, stepErr error, log *zap.SugaredLogger) error {
		return nil
	}

	oldStep := saveCacheStep
	defer func() { saveCacheStep = oldStep }()
	saveCacheStep = func(step *pb.UnitStep, tmpFilePath string, so output.StageOutput,
		fs filesystem.FileSystem, log *zap.SugaredLogger) steps.SaveCacheStep {
		return mockStep
	}

	mockStep.EXPECT().Run(ctx).Return(o, nil)

	e := NewUnitExecutor(logPath, tmpFilePath, log.Sugar())
	_, err := e.Run(ctx, stepProto, nil, accountID)
	assert.Equal(t, err, nil)
}

func TestStepRestoreCacheErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	accountID := "test"
	taskID := "taskID"
	callbackToken := "token"
	tmpFilePath := "/tmp"
	stepProto := &pb.UnitStep{
		Id: "test3",
		Step: &pb.UnitStep_RestoreCache{
			RestoreCache: &pb.RestoreCacheStep{
				Key: "key",
			},
		},
		CallbackToken: callbackToken,
		TaskId:        taskID,
	}
	logPath := "/a/b"
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	mockStep := msteps.NewMockRestoreCacheStep(ctrl)

	oldSendStepStatus := sendStepStatus
	defer func() { sendStepStatus = oldSendStepStatus }()
	sendStepStatus = func(ctx context.Context, accountID, callbackToken, taskID string, numRetries int32, timeTaken time.Duration,
		stepOutput *output.StepOutput, stepErr error, log *zap.SugaredLogger) error {
		return nil
	}

	oldStep := restoreCacheStep
	defer func() { restoreCacheStep = oldStep }()
	restoreCacheStep = func(step *pb.UnitStep, tmpFilePath string, so output.StageOutput,
		fs filesystem.FileSystem, log *zap.SugaredLogger) steps.RestoreCacheStep {
		return mockStep
	}

	mockStep.EXPECT().Run(ctx).Return(errors.New("restore failed"))

	e := NewUnitExecutor(logPath, tmpFilePath, log.Sugar())
	_, err := e.Run(ctx, stepProto, nil, accountID)
	assert.NotEqual(t, err, nil)
}

func TestStepRestoreCacheSuccess(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	accountID := "test"
	taskID := "taskID"
	callbackToken := "token"
	tmpFilePath := "/tmp"
	stepProto := &pb.UnitStep{
		Id: "test3",
		Step: &pb.UnitStep_RestoreCache{
			RestoreCache: &pb.RestoreCacheStep{
				Key: "key",
			},
		},
		CallbackToken: callbackToken,
		TaskId:        taskID,
	}
	logPath := "/a/b"
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	mockStep := msteps.NewMockRestoreCacheStep(ctrl)

	oldSendStepStatus := sendStepStatus
	defer func() { sendStepStatus = oldSendStepStatus }()
	sendStepStatus = func(ctx context.Context, accountID, callbackToken, taskID string, numRetries int32, timeTaken time.Duration,
		stepOutput *output.StepOutput, stepErr error, log *zap.SugaredLogger) error {
		return nil
	}

	oldStep := restoreCacheStep
	defer func() { restoreCacheStep = oldStep }()
	restoreCacheStep = func(step *pb.UnitStep, tmpFilePath string, so output.StageOutput,
		fs filesystem.FileSystem, log *zap.SugaredLogger) steps.RestoreCacheStep {
		return mockStep
	}

	mockStep.EXPECT().Run(ctx).Return(nil)

	e := NewUnitExecutor(logPath, tmpFilePath, log.Sugar())
	_, err := e.Run(ctx, stepProto, nil, accountID)
	assert.Equal(t, err, nil)
}

func TestPublishArtifactsSuccess(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	accountID := "test"
	taskID := "taskID"
	callbackToken := "token"
	tmpFilePath := "/tmp"
	stepProto := &pb.UnitStep{
		Id: "test3",
		Step: &pb.UnitStep_PublishArtifacts{
			PublishArtifacts: &pb.PublishArtifactsStep{
				Images: []*pb2.BuildPublishImage{},
				Files:  []*pb2.UploadFile{},
			},
		},
		CallbackToken: callbackToken,
		TaskId:        taskID,
	}
	logPath := "/a/b"
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	mockStep := msteps.NewMockPublishArtifactsStep(ctrl)

	oldSendStepStatus := sendStepStatus
	defer func() { sendStepStatus = oldSendStepStatus }()
	sendStepStatus = func(ctx context.Context, accountID, callbackToken, taskID string, numRetries int32, timeTaken time.Duration,
		stepOutput *output.StepOutput, stepErr error, log *zap.SugaredLogger) error {
		return nil
	}

	oldStep := publishArtifactsStep
	defer func() { publishArtifactsStep = oldStep }()
	publishArtifactsStep = func(step *pb.UnitStep, so output.StageOutput,
		log *zap.SugaredLogger) steps.PublishArtifactsStep {
		return mockStep
	}

	mockStep.EXPECT().Run(ctx).Return(errors.New("Could not publish artifacts"))

	e := NewUnitExecutor(logPath, tmpFilePath, log.Sugar())
	_, err := e.Run(ctx, stepProto, nil, accountID)
	assert.NotEqual(t, err, nil)
}

func TestPublishArtifactsErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	accountID := "test"
	taskID := "taskID"
	callbackToken := "token"
	tmpFilePath := "/tmp"
	stepProto := &pb.UnitStep{
		Id: "test3",
		Step: &pb.UnitStep_PublishArtifacts{
			PublishArtifacts: &pb.PublishArtifactsStep{
				Images: []*pb2.BuildPublishImage{},
				Files:  []*pb2.UploadFile{},
			},
		},
		CallbackToken: callbackToken,
		TaskId:        taskID,
	}
	logPath := "/a/b"
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	mockStep := msteps.NewMockPublishArtifactsStep(ctrl)

	oldSendStepStatus := sendStepStatus
	defer func() { sendStepStatus = oldSendStepStatus }()
	sendStepStatus = func(ctx context.Context, accountID, callbackToken, taskID string, numRetries int32, timeTaken time.Duration,
		stepOutput *output.StepOutput, stepErr error, log *zap.SugaredLogger) error {
		return nil
	}

	oldStep := publishArtifactsStep
	defer func() { publishArtifactsStep = oldStep }()
	publishArtifactsStep = func(step *pb.UnitStep, so output.StageOutput,
		log *zap.SugaredLogger) steps.PublishArtifactsStep {
		return mockStep
	}

	mockStep.EXPECT().Run(ctx).Return(nil)

	e := NewUnitExecutor(logPath, tmpFilePath, log.Sugar())
	_, err := e.Run(ctx, stepProto, nil, accountID)
	assert.Equal(t, err, nil)
}

func TestExecuteStep(t *testing.T) {
	logPath := "/a/b"
	tmpFilePath := "/tmp"
	accountID := "test"
	taskID := "taskID"
	callbackToken := "token"

	r := &pb.ExecuteStepRequest{
		Step: &pb.UnitStep{
			Id:            "test4",
			CallbackToken: callbackToken,
			TaskId:        taskID,
		},
		AccountId: accountID,
	}
	emptyStep := getEncodedExecuteStepProto(t, r)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	oldSendStepStatus := sendStepStatus
	defer func() { sendStepStatus = oldSendStepStatus }()
	sendStepStatus = func(ctx context.Context, accountID, callbackToken, taskID string, numRetries int32, timeTaken time.Duration,
		stepOutput *output.StepOutput, stepErr error, log *zap.SugaredLogger) error {
		return nil
	}

	ExecuteStep(emptyStep, logPath, tmpFilePath, log.Sugar())
}

func TestDecodeExecuteStep(t *testing.T) {
	incorrectBase64Enc := "x"
	invalidStepEnc := "YWJjZA=="

	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	_, err := decodeExecuteStepRequest(incorrectBase64Enc, log.Sugar())
	assert.NotEqual(t, err, nil)

	_, err = decodeExecuteStepRequest(invalidStepEnc, log.Sugar())
	assert.NotEqual(t, err, nil)
}
