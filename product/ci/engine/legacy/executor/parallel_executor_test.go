// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package executor

import (
	"context"
	"fmt"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/logs"
	mexecutor "github.com/wings-software/portal/product/ci/engine/legacy/executor/mocks"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

func testGetRunStep(id string, cmd string) *pb.UnitStep {
	ctx := &pb.StepContext{
		NumRetries: 2,
	}

	return &pb.UnitStep{
		Id:          id,
		DisplayName: fmt.Sprintf("step %s", id),
		Step: &pb.UnitStep_Run{
			Run: &pb.RunStep{
				Context: ctx,
				Command: cmd,
			},
		},
	}
}

func TestParallelExecutorRun(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	accountID := "test"
	tmpFilePath := "/tmp"
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
			name: "successful single unit step",
			step: &pb.ParallelStep{
				Id: "ptest1",
				Steps: []*pb.UnitStep{
					testGetRunStep("ptest1_1", "ls")},
			},
			expectedErr: false,
		},
		{
			name: "successful nultiple unit steps",
			step: &pb.ParallelStep{
				Id: "ptest2",
				Steps: []*pb.UnitStep{
					testGetRunStep("ptest2_1", "ls"),
					testGetRunStep("ptest2_2", "ls -l"),
					testGetRunStep("ptest2_3", "ls")},
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
			tmpFilePath:  tmpFilePath,
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
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	unitStepErr := fmt.Errorf("failed to execute step")

	step := &pb.ParallelStep{
		Id: "ptest",
		Steps: []*pb.UnitStep{
			testGetRunStep("ptest3_1", "la"),
			testGetRunStep("ptest3_3", "l")},
	}

	// Mock out unit Executor run step
	mockUnitExecutor := mexecutor.NewMockUnitExecutor(ctrl)
	e := &parallelExecutor{
		unitExecutor: mockUnitExecutor,
		tmpFilePath:  tmpFilePath,
		log:          log.Sugar(),
	}
	mockUnitExecutor.EXPECT().Run(ctx, gomock.Any(), gomock.Any(), accountID).Return(nil, unitStepErr).AnyTimes()

	_, got := e.Run(ctx, step, nil, accountID)
	assert.NotEqual(t, got, nil)
}

func TestParallelCleanupSuccess(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	tmpFilePath := "/tmp"
	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	step := &pb.ParallelStep{
		Id: "ptest",
		Steps: []*pb.UnitStep{
			testGetRunStep("ptest4_1", "pwd"),
			testGetRunStep("ptest4_2", "ls")},
	}

	// Mock out unit Executor run step
	mockUnitExecutor := mexecutor.NewMockUnitExecutor(ctrl)
	e := &parallelExecutor{
		unitExecutor: mockUnitExecutor,
		tmpFilePath:  tmpFilePath,
		log:          log.Sugar(),
	}
	mockUnitExecutor.EXPECT().Cleanup(ctx, gomock.Any()).Return(nil).AnyTimes()

	err := e.Cleanup(ctx, step)
	assert.Equal(t, err, nil)
}

func TestParallelCleanupErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	tmpFilePath := "/tmp"
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	cleanupErr := fmt.Errorf("failed to create client")

	step := &pb.ParallelStep{
		Id: "ptest",
		Steps: []*pb.UnitStep{
			testGetRunStep("ptest4_1", "pwd"),
			testGetRunStep("ptest4_2", "ls")},
	}

	// Mock out unit Executor run step
	mockUnitExecutor := mexecutor.NewMockUnitExecutor(ctrl)
	e := &parallelExecutor{
		unitExecutor: mockUnitExecutor,
		tmpFilePath:  tmpFilePath,
		log:          log.Sugar(),
	}
	mockUnitExecutor.EXPECT().Cleanup(ctx, gomock.Any()).Return(cleanupErr).AnyTimes()

	err := e.Cleanup(ctx, step)
	assert.NotEqual(t, err, nil)
}
