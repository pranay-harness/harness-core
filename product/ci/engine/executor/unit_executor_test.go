package executor

import (
	"context"
	"encoding/base64"
	"testing"

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

func getEncodedStepProto(t *testing.T, step *pb.UnitStep) string {
	data, err := proto.Marshal(step)
	if err != nil {
		t.Fatalf("marshaling error: %v", err)
	}
	return base64.StdEncoding.EncodeToString(data)
}

func TestStepRun(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

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
			name:        "empty stage success",
			step:        stepProto3,
			logPath:     logPath,
			expectedErr: false,
		},
	}
	for _, tc := range tests {
		e := NewUnitExecutor(tc.logPath, tmpFilePath, log.Sugar())
		_, got := e.Run(ctx, tc.step, nil)
		if tc.expectedErr == (got == nil) {
			t.Fatalf("%s: expected error: %v, got: %v", tc.name, tc.expectedErr, got)
		}
	}
}

func TestStepSaveCacheError(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	tmpFilePath := "/tmp"
	stepProto := &pb.UnitStep{
		Id: "test2",
		Step: &pb.UnitStep_SaveCache{
			SaveCache: &pb.SaveCacheStep{
				Key:   "key",
				Paths: []string{"/tmp/m2"},
			},
		},
	}
	logPath := "/a/b"
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	mockStep := msteps.NewMockSaveCacheStep(ctrl)

	oldStep := saveCacheStep
	defer func() { saveCacheStep = oldStep }()
	saveCacheStep = func(step *pb.UnitStep, tmpFilePath string, so output.StageOutput,
		fs filesystem.FileSystem, log *zap.SugaredLogger) steps.SaveCacheStep {
		return mockStep
	}

	mockStep.EXPECT().Run(ctx).Return(errors.New("caching failed"))

	e := NewUnitExecutor(logPath, tmpFilePath, log.Sugar())
	_, err := e.Run(ctx, stepProto, nil)
	assert.NotEqual(t, err, nil)
}

func TestStepSaveCacheSuccess(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	tmpFilePath := "/tmp"
	stepProto := &pb.UnitStep{
		Id: "test2",
		Step: &pb.UnitStep_SaveCache{
			SaveCache: &pb.SaveCacheStep{
				Key:   "key",
				Paths: []string{"/tmp/m2"},
			},
		},
	}
	logPath := "/a/b"
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	mockStep := msteps.NewMockSaveCacheStep(ctrl)

	oldStep := saveCacheStep
	defer func() { saveCacheStep = oldStep }()
	saveCacheStep = func(step *pb.UnitStep, tmpFilePath string, so output.StageOutput,
		fs filesystem.FileSystem, log *zap.SugaredLogger) steps.SaveCacheStep {
		return mockStep
	}

	mockStep.EXPECT().Run(ctx).Return(nil)

	e := NewUnitExecutor(logPath, tmpFilePath, log.Sugar())
	_, err := e.Run(ctx, stepProto, nil)
	assert.Equal(t, err, nil)
}

func TestStepRestoreCacheErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	tmpFilePath := "/tmp"
	stepProto := &pb.UnitStep{
		Id: "test3",
		Step: &pb.UnitStep_RestoreCache{
			RestoreCache: &pb.RestoreCacheStep{
				Key: "key",
			},
		},
	}
	logPath := "/a/b"
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	mockStep := msteps.NewMockRestoreCacheStep(ctrl)

	oldStep := restoreCacheStep
	defer func() { restoreCacheStep = oldStep }()
	restoreCacheStep = func(step *pb.UnitStep, tmpFilePath string, so output.StageOutput,
		fs filesystem.FileSystem, log *zap.SugaredLogger) steps.RestoreCacheStep {
		return mockStep
	}

	mockStep.EXPECT().Run(ctx).Return(errors.New("restore failed"))

	e := NewUnitExecutor(logPath, tmpFilePath, log.Sugar())
	_, err := e.Run(ctx, stepProto, nil)
	assert.NotEqual(t, err, nil)
}

func TestStepRestoreCacheSuccess(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	tmpFilePath := "/tmp"
	stepProto := &pb.UnitStep{
		Id: "test3",
		Step: &pb.UnitStep_RestoreCache{
			RestoreCache: &pb.RestoreCacheStep{
				Key: "key",
			},
		},
	}
	logPath := "/a/b"
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	mockStep := msteps.NewMockRestoreCacheStep(ctrl)

	oldStep := restoreCacheStep
	defer func() { restoreCacheStep = oldStep }()
	restoreCacheStep = func(step *pb.UnitStep, tmpFilePath string, so output.StageOutput,
		fs filesystem.FileSystem, log *zap.SugaredLogger) steps.RestoreCacheStep {
		return mockStep
	}

	mockStep.EXPECT().Run(ctx).Return(nil)

	e := NewUnitExecutor(logPath, tmpFilePath, log.Sugar())
	_, err := e.Run(ctx, stepProto, nil)
	assert.Equal(t, err, nil)
}

func TestPublishArtifactsSuccess(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	tmpFilePath := "/tmp"
	stepProto := &pb.UnitStep{
		Id: "test3",
		Step: &pb.UnitStep_PublishArtifacts{
			PublishArtifacts: &pb.PublishArtifactsStep{
				Images: []*pb2.BuildPublishImage{},
				Files:  []*pb2.UploadFile{},
			},
		},
	}
	logPath := "/a/b"
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	mockStep := msteps.NewMockPublishArtifactsStep(ctrl)

	oldStep := publishArtifactsStep
	defer func() { publishArtifactsStep = oldStep }()
	publishArtifactsStep = func(step *pb.UnitStep, so output.StageOutput,
		log *zap.SugaredLogger) steps.PublishArtifactsStep {
		return mockStep
	}

	mockStep.EXPECT().Run(ctx).Return(errors.New("Could not publish artifacts"))

	e := NewUnitExecutor(logPath, tmpFilePath, log.Sugar())
	_, err := e.Run(ctx, stepProto, nil)
	assert.NotEqual(t, err, nil)
}

func TestPublishArtifactsErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	tmpFilePath := "/tmp"
	stepProto := &pb.UnitStep{
		Id: "test3",
		Step: &pb.UnitStep_PublishArtifacts{
			PublishArtifacts: &pb.PublishArtifactsStep{
				Images: []*pb2.BuildPublishImage{},
				Files:  []*pb2.UploadFile{},
			},
		},
	}
	logPath := "/a/b"
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	mockStep := msteps.NewMockPublishArtifactsStep(ctrl)

	oldStep := publishArtifactsStep
	defer func() { publishArtifactsStep = oldStep }()
	publishArtifactsStep = func(step *pb.UnitStep, so output.StageOutput,
		log *zap.SugaredLogger) steps.PublishArtifactsStep {
		return mockStep
	}

	mockStep.EXPECT().Run(ctx).Return(nil)

	e := NewUnitExecutor(logPath, tmpFilePath, log.Sugar())
	_, err := e.Run(ctx, stepProto, nil)
	assert.Equal(t, err, nil)
}

func TestExecuteStep(t *testing.T) {
	logPath := "/a/b"
	tmpFilePath := "/tmp"
	stepProto := &pb.UnitStep{
		Id: "test4",
	}
	emptyStep := getEncodedStepProto(t, stepProto)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	ExecuteStep(emptyStep, logPath, tmpFilePath, log.Sugar())
}

func TestDecodeUnitStep(t *testing.T) {
	incorrectBase64Enc := "x"
	invalidStepEnc := "YWJjZA=="

	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	_, err := decodeUnitStep(incorrectBase64Enc, log.Sugar())
	assert.NotEqual(t, err, nil)

	_, err = decodeUnitStep(invalidStepEnc, log.Sugar())
	assert.NotEqual(t, err, nil)
}
