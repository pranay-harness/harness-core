package executor

import (
	"context"
	"fmt"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/pkg/errors"
	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/logs"
	mexecutor "github.com/wings-software/portal/product/ci/engine/executor/mocks"
	cengine "github.com/wings-software/portal/product/ci/engine/grpc/client"
	emgrpc "github.com/wings-software/portal/product/ci/engine/grpc/client/mocks"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
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

	accountID := "test"
	tmpFilePath := "/tmp"
	logPath := "/tmp"
	oldLogger := newRemoteLogger
	defer func() { newRemoteLogger = oldLogger }()
	newRemoteLogger = func(key string) (rl *logs.RemoteLogger, err error) {
		log, _ := logs.GetObservedLogger(zap.InfoLevel)
		return &logs.RemoteLogger{BaseLogger: log.Sugar(), Writer: logs.NopWriter()}, nil
	}
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
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
	}

	// Mock out unit Executor run step
	mockUnitExecutor := mexecutor.NewMockUnitExecutor(ctrl)
	mockUnitExecutor.EXPECT().Run(ctx, gomock.Any(), gomock.Any(), accountID).Return(nil, nil).AnyTimes()

	for _, tc := range tests {
		e := &parallelExecutor{
			unitExecutor: mockUnitExecutor,
			stepLogPath:  logPath,
			tmpFilePath:  tmpFilePath,
			mainOnly:     true,
			workerPorts:  nil,
			log:          log.Sugar(),
		}
		_, got := e.Run(ctx, tc.step, nil, accountID)
		if tc.expectedErr == (got == nil) {
			t.Fatalf("%s: expected error: %v, got: %v", tc.name, tc.expectedErr, got)
		}
	}
}

func TestParallelExecutorUnitStepErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	accountID := "test"
	tmpFilePath := "/tmp"
	logPath := "/tmp"
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	unitStepErr := fmt.Errorf("failed to execute step")

	step := &pb.ParallelStep{
		Id: "ptest",
		Steps: []*pb.UnitStep{
			testGetRunStep("ptest3_1", []string{"la"}),
			testGetRunStep("ptest3_3", []string{"l"})},
	}

	// Mock out unit Executor run step
	mockUnitExecutor := mexecutor.NewMockUnitExecutor(ctrl)
	e := &parallelExecutor{
		unitExecutor: mockUnitExecutor,
		stepLogPath:  logPath,
		tmpFilePath:  tmpFilePath,
		mainOnly:     true,
		workerPorts:  nil,
		log:          log.Sugar(),
	}
	mockUnitExecutor.EXPECT().Run(ctx, gomock.Any(), gomock.Any(), accountID).Return(nil, unitStepErr).AnyTimes()

	_, got := e.Run(ctx, step, nil, accountID)
	assert.NotEqual(t, got, nil)
}

func TestParallelExecutorGetWorkers(t *testing.T) {
	tmpFilePath := "/tmp"
	logPath := "/tmp"
	ports := []uint{9000, 9001}
	numRunSteps := 3
	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	oldLogger := newRemoteLogger
	defer func() { newRemoteLogger = oldLogger }()
	newRemoteLogger = func(key string) (rl *logs.RemoteLogger, err error) {
		log, _ := logs.GetObservedLogger(zap.InfoLevel)
		return &logs.RemoteLogger{BaseLogger: log.Sugar(), Writer: logs.NopWriter()}, nil
	}

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

	accountID := "test"
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

	_, err := e.executeRemoteStep(ctx, worker{}, nil, nil, accountID)
	assert.NotNil(t, err)
}

func TestRemoteStepClientErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	accountID := "test"
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
	_, err = e.executeRemoteStep(ctx, worker{}, nil, nil, accountID)
	assert.NotNil(t, err)
}
