// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package grpc

import (
	"context"
	"fmt"
	"io"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/logs"
	addonpb "github.com/wings-software/portal/product/ci/addon/proto"
	"github.com/wings-software/portal/product/ci/addon/tasks"
	mtasks "github.com/wings-software/portal/product/ci/addon/tasks/mocks"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

func TestSignalStop(t *testing.T) {
	stopCh := make(chan bool)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	h := NewAddonHandler(stopCh, false, log.Sugar())
	_, err := h.SignalStop(nil, nil)
	assert.Nil(t, err)
}

func TestExecuteNilStep(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	stopCh := make(chan bool)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	oldLogger := newGrpcRemoteLogger
	defer func() { newGrpcRemoteLogger = oldLogger }()
	newGrpcRemoteLogger = func(key string) (rl *logs.RemoteLogger, err error) {
		return &logs.RemoteLogger{BaseLogger: log.Sugar(), Writer: logs.NopWriter()}, nil
	}

	h := NewAddonHandler(stopCh, true, log.Sugar())
	_, err := h.ExecuteStep(ctx, nil)
	assert.NotNil(t, err)
}

func TestExecuteLoggerErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	stopCh := make(chan bool)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	oldLogger := newGrpcRemoteLogger
	defer func() { newGrpcRemoteLogger = oldLogger }()
	newGrpcRemoteLogger = func(key string) (rl *logs.RemoteLogger, err error) {
		return nil, fmt.Errorf("remote logger not found")
	}

	h := NewAddonHandler(stopCh, false, log.Sugar())
	_, err := h.ExecuteStep(ctx, nil)
	assert.NotNil(t, err)
}

func TestExecuteRunStep(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	stopCh := make(chan bool)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	mockStep := mtasks.NewMockRunTask(ctrl)

	in := &addonpb.ExecuteStepRequest{
		Step: &pb.UnitStep{
			Id: "step1",
			Step: &pb.UnitStep_Run{
				Run: &pb.RunStep{
					Command: "ls",
				},
			},
		},
	}

	oldLogger := newGrpcRemoteLogger
	defer func() { newGrpcRemoteLogger = oldLogger }()
	newGrpcRemoteLogger = func(key string) (rl *logs.RemoteLogger, err error) {
		return &logs.RemoteLogger{BaseLogger: log.Sugar(), Writer: logs.NopWriter()}, nil
	}

	oldRunTask := newRunTask
	defer func() { newRunTask = oldRunTask }()
	newRunTask = func(step *pb.UnitStep, so map[string]*pb.StepOutput, tmpFilePath string, log *zap.SugaredLogger,
		w io.Writer, logMetrics bool, addonLogger *zap.SugaredLogger) tasks.RunTask {
		return mockStep
	}

	mockStep.EXPECT().Run(ctx).Return(nil, int32(1), nil)
	h := NewAddonHandler(stopCh, false, log.Sugar())
	_, err := h.ExecuteStep(ctx, in)
	assert.Nil(t, err)
}

func TestExecutePluginStep(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	stopCh := make(chan bool)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	mockStep := mtasks.NewMockPluginTask(ctrl)

	in := &addonpb.ExecuteStepRequest{
		Step: &pb.UnitStep{
			Id: "step1",
			Step: &pb.UnitStep_Plugin{
				Plugin: &pb.PluginStep{
					Image: "plugin/drone-git",
				},
			},
		},
	}

	oldLogger := newGrpcRemoteLogger
	defer func() { newGrpcRemoteLogger = oldLogger }()
	newGrpcRemoteLogger = func(key string) (rl *logs.RemoteLogger, err error) {
		return &logs.RemoteLogger{BaseLogger: log.Sugar(), Writer: logs.NopWriter()}, nil
	}

	oldPluginTask := newPluginTask
	defer func() { newPluginTask = oldPluginTask }()
	newPluginTask = func(step *pb.UnitStep, so map[string]*pb.StepOutput, log *zap.SugaredLogger, w io.Writer, logMetrics bool, addonLogger *zap.SugaredLogger) tasks.PluginTask {
		return mockStep
	}

	mockStep.EXPECT().Run(ctx).Return(nil, int32(1), nil)
	h := NewAddonHandler(stopCh, false, log.Sugar())
	_, err := h.ExecuteStep(ctx, in)
	assert.Nil(t, err)
}
