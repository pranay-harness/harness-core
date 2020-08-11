package executor

import (
	"context"
	"fmt"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/pkg/errors"
	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/filesystem"
	"github.com/wings-software/portal/commons/go/lib/logs"
	cengine "github.com/wings-software/portal/product/ci/engine/grpc/client"
	emgrpc "github.com/wings-software/portal/product/ci/engine/grpc/client/mocks"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"github.com/wings-software/portal/product/ci/engine/steps"
	emsteps "github.com/wings-software/portal/product/ci/engine/steps/mocks"
	"go.uber.org/zap"
	"google.golang.org/grpc"
)

func testGetRunStep(id string, cmd []string) *pb.UnitStep {
	ctx := &pb.StepContext{
		NumRetries: 2,
	}

	return &pb.UnitStep{
		Id:          id,
		DisplayName: fmt.Sprintf("step %s", id),
		Step: &pb.UnitStep_Run{
			Run: &pb.RunStep{
				Context:  ctx,
				Commands: cmd,
			},
		},
	}
}

func TestParallelExecutorRun(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	tmpFilePath := "/tmp"
	logPath := "/tmp"
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	errSaveCacheStep := &pb.UnitStep{
		Step: &pb.UnitStep_SaveCache{
			SaveCache: &pb.SaveCacheStep{
				Key:   "key",
				Paths: []string{"/tmp/m2"},
			},
		},
	}

	tests := []struct {
		name        string
		step        *pb.ParallelStep
		expectedErr bool
	}{
		{
			name: "parallel step ID is not set",
			step: &pb.ParallelStep{
				DisplayName: "parallel",
			},
			expectedErr: true,
		},
		{
			name: "successful main-only parallel single unit step",
			step: &pb.ParallelStep{
				Id: "ptest1",
				Steps: []*pb.UnitStep{
					testGetRunStep("ptest1_1", []string{"ls"})},
			},
			expectedErr: false,
		},
		{
			name: "successful main-only parallel nultiple unit steps",
			step: &pb.ParallelStep{
				Id: "ptest2",
				Steps: []*pb.UnitStep{
					testGetRunStep("ptest2_1", []string{"ls"}),
					testGetRunStep("ptest2_2", []string{"ls -l"}),
					testGetRunStep("ptest2_3", []string{"ls"})},
			},
			expectedErr: false,
		},
		{
			name: "main-only parallel with error in non-run unit steps",
			step: &pb.ParallelStep{
				Id: "ptest4",
				Steps: []*pb.UnitStep{
					testGetRunStep("ptest3_1", []string{"ls"}),
					errSaveCacheStep,
					testGetRunStep("ptest3_3", []string{"l"})},
			},
			expectedErr: true,
		},
	}

	// Mock out unit Executor run step
	oldRunStep := runStep
	defer func() { runStep = oldRunStep }()
	mockedRunStep := emsteps.NewMockRunStep(ctrl)
	mockedRunStep.EXPECT().Run(ctx).Return(nil).AnyTimes()

	runStep = func(step *pb.UnitStep, stepLogPath string, tmpFilePath string,
		fs filesystem.FileSystem, log *zap.SugaredLogger) steps.RunStep {
		return mockedRunStep
	}
	for _, tc := range tests {
		e := NewParallelExecutor(logPath, tmpFilePath, nil, log.Sugar())
		got := e.Run(ctx, tc.step)
		if tc.expectedErr == (got == nil) {
			t.Fatalf("%s: expected error: %v, got: %v", tc.name, tc.expectedErr, got)
		}
	}
}

func TestParallelExecutorGetWorkers(t *testing.T) {
	tmpFilePath := "/tmp"
	logPath := "/tmp"
	ports := []uint{9000, 9001}
	numRunSteps := 3
	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	e := &parallelExecutor{
		stepLogPath: logPath,
		tmpFilePath: tmpFilePath,
		mainOnly:    false,
		workerPorts: ports,
		log:         log.Sugar()}
	workers := e.getWorkers(3)
	assert.Equal(t, len(workers), numRunSteps)
}

func TestRemoteStepClientCreateErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	tmpFilePath := "/tmp"
	logPath := "/tmp"
	ports := []uint{9000, 9001}
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	e := &parallelExecutor{
		stepLogPath: logPath,
		tmpFilePath: tmpFilePath,
		mainOnly:    false,
		workerPorts: ports,
		log:         log.Sugar(),
	}

	oldClient := newLiteEngineClient
	defer func() { newLiteEngineClient = oldClient }()
	newLiteEngineClient = func(port uint, log *zap.SugaredLogger) (cengine.LiteEngineClient, error) {
		return nil, errors.New("Could not create client")
	}

	err := e.executeRemoteStep(ctx, worker{}, nil)
	assert.NotNil(t, err)
}

func TestRemoteStepClientErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	conn, err := grpc.DialContext(ctx, "bufnet", grpc.WithContextDialer(bufDialer), grpc.WithInsecure())
	if err != nil {
		t.Fatalf("Failed to dial bufnet: %v", err)
	}
	defer conn.Close()
	client := pb.NewLiteEngineClient(conn)
	oldClient := newLiteEngineClient
	defer func() { newLiteEngineClient = oldClient }()
	// Initialize a mock Lite engine
	mockClient := emgrpc.NewMockLiteEngineClient(ctrl)
	mockClient.EXPECT().CloseConn().Return(nil)
	mockClient.EXPECT().Client().Return(client)
	newLiteEngineClient = func(port uint, log *zap.SugaredLogger) (cengine.LiteEngineClient, error) {
		return mockClient, nil
	}

	tmpFilePath := "/tmp"
	logPath := "/tmp"
	ports := []uint{9000, 9001}
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	e := &parallelExecutor{
		stepLogPath: logPath,
		tmpFilePath: tmpFilePath,
		mainOnly:    false,
		workerPorts: ports,
		log:         log.Sugar(),
	}
	err = e.executeRemoteStep(ctx, worker{}, nil)
	assert.NotNil(t, err)
}
