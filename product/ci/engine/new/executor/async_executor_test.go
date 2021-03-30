package executor

import (
	"context"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/pkg/errors"
	"github.com/wings-software/portal/commons/go/lib/logs"
	mexecutor "github.com/wings-software/portal/product/ci/engine/new/executor/mocks"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

func TestExecuteStepInAsync(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	arg := &pb.ExecuteStepRequest{
		ExecutionId: "test",
		Step: &pb.UnitStep{
			Id: "test2",
			Step: &pb.UnitStep_Run{
				Run: &pb.RunStep{
					Command: "ls",
				},
			},
			ContainerPort: uint32(8000),
		},
	}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	ExecuteStepInAsync(ctx, arg, log.Sugar())
}

func TestExecuteStepSuccess(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	arg := &pb.ExecuteStepRequest{
		ExecutionId: "test",
		Step: &pb.UnitStep{
			Id: "test2",
			Step: &pb.UnitStep_Run{
				Run: &pb.RunStep{
					Command: "ls",
				},
			},
			ContainerPort: uint32(8000),
		},
	}

	mockStepExecutor := mexecutor.NewMockStepExecutor(ctrl)
	mockStepExecutor.EXPECT().Run(gomock.Any(), gomock.Any()).AnyTimes().Return(nil)

	oldStepExecutor := newStepExecutor
	defer func() { newStepExecutor = oldStepExecutor }()
	newStepExecutor = func(tmpFilePath string, log *zap.SugaredLogger) StepExecutor {
		return mockStepExecutor
	}
	executeStep(arg, log.Sugar())
}

func TestExecuteStepFail(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	arg := &pb.ExecuteStepRequest{
		ExecutionId: "test",
		Step: &pb.UnitStep{
			Id: "test2",
			Step: &pb.UnitStep_Run{
				Run: &pb.RunStep{
					Command: "ls",
				},
			},
			ContainerPort: uint32(8000),
		},
	}

	mockStepExecutor := mexecutor.NewMockStepExecutor(ctrl)
	mockStepExecutor.EXPECT().Run(gomock.Any(), gomock.Any()).AnyTimes().Return(errors.New("failed"))

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	oldStepExecutor := newStepExecutor
	defer func() { newStepExecutor = oldStepExecutor }()
	newStepExecutor = func(tmpFilePath string, log *zap.SugaredLogger) StepExecutor {
		return mockStepExecutor
	}
	executeStep(arg, log.Sugar())
}
