// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package executor

import (
	"bytes"
	"context"
	"io"
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
	ExecuteStepInAsync(ctx, arg, log.Sugar(), new(bytes.Buffer))
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
	newStepExecutor = func(tmpFilePath, delegateSvcEndpoint string, log *zap.SugaredLogger, buf io.Writer) StepExecutor {
		return mockStepExecutor
	}
	executeStep(arg, log.Sugar(), new(bytes.Buffer))
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
	newStepExecutor = func(tmpFilePath, delegateSvcEndpoint string, log *zap.SugaredLogger, buf io.Writer) StepExecutor {
		return mockStepExecutor
	}
	executeStep(arg, log.Sugar(), new(bytes.Buffer))
}
