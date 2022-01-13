// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package tasks

import (
	"bytes"
	"context"
	"fmt"
	"os/exec"
	"syscall"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	mexec "github.com/wings-software/portal/commons/go/lib/exec"
	"github.com/wings-software/portal/commons/go/lib/logs"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

func TestPluginSuccess(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	var buf bytes.Buffer
	numRetries := int32(1)
	commands := []string{"git"}
	cmdFactory := mexec.NewMockCmdContextFactory(ctrl)
	cmd := mexec.NewMockCommand(ctrl)
	pstate := mexec.NewMockProcessState(ctrl)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	e := pluginTask{
		id:                "step1",
		image:             "plugin/drone-git",
		timeoutSecs:       5,
		numRetries:        numRetries,
		log:               log.Sugar(),
		addonLogger:       log.Sugar(),
		cmdContextFactory: cmdFactory,
		procWriter:        &buf,
	}

	oldImgMetadata := getImgMetadata
	getImgMetadata = func(ctx context.Context, id, image, secret string, log *zap.SugaredLogger) ([]string, []string, error) {
		return commands, nil, nil
	}
	defer func() { getImgMetadata = oldImgMetadata }()

	cmdFactory.EXPECT().CmdContextWithSleep(gomock.Any(), cmdExitWaitTime, gomock.Any()).Return(cmd)
	cmd.EXPECT().WithStdout(&buf).Return(cmd)
	cmd.EXPECT().WithStderr(&buf).Return(cmd)
	cmd.EXPECT().WithEnvVarsMap(gomock.Any()).Return(cmd)
	cmd.EXPECT().Start().Return(nil)
	cmd.EXPECT().ProcessState().Return(pstate)
	pstate.EXPECT().SysUsageUnit().Return(&syscall.Rusage{Maxrss: 100}, nil)
	cmd.EXPECT().Wait().Return(nil)

	_, retries, err := e.Run(ctx)
	assert.Nil(t, err)
	assert.Equal(t, retries, numRetries)
}

func TestPluginNonZeroStatus(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	numRetries := int32(1)
	var buf bytes.Buffer
	commands := []string{"git"}
	cmdFactory := mexec.NewMockCmdContextFactory(ctrl)
	cmd := mexec.NewMockCommand(ctrl)
	pstate := mexec.NewMockProcessState(ctrl)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	e := pluginTask{
		id:                "step1",
		image:             "plugin/drone-git",
		timeoutSecs:       5,
		numRetries:        numRetries,
		log:               log.Sugar(),
		addonLogger:       log.Sugar(),
		cmdContextFactory: cmdFactory,
		procWriter:        &buf,
	}

	oldImgMetadata := getImgMetadata
	getImgMetadata = func(ctx context.Context, id, image, secret string, log *zap.SugaredLogger) ([]string, []string, error) {
		return commands, nil, nil
	}
	defer func() { getImgMetadata = oldImgMetadata }()

	cmdFactory.EXPECT().CmdContextWithSleep(gomock.Any(), cmdExitWaitTime, gomock.Any()).Return(cmd)
	cmd.EXPECT().WithStdout(&buf).Return(cmd)
	cmd.EXPECT().WithStderr(&buf).Return(cmd)
	cmd.EXPECT().WithEnvVarsMap(gomock.Any()).Return(cmd)
	cmd.EXPECT().Start().Return(nil)
	cmd.EXPECT().ProcessState().Return(pstate)
	pstate.EXPECT().SysUsageUnit().Return(&syscall.Rusage{Maxrss: 100}, nil)
	cmd.EXPECT().Wait().Return(&exec.ExitError{})

	_, retries, err := e.Run(ctx)
	assert.NotNil(t, err)
	if _, ok := err.(*exec.ExitError); !ok {
		t.Fatalf("Expected err of type exec.ExitError")
	}
	assert.Equal(t, retries, numRetries)
}

func TestPluginTaskCreate(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	step := &pb.UnitStep{
		Id: "test",
		Step: &pb.UnitStep_Plugin{
			Plugin: &pb.PluginStep{
				Image: "plugin/drone-git",
			},
		},
	}

	var buf bytes.Buffer
	executor := NewPluginTask(step, nil, log.Sugar(), &buf, false, log.Sugar())
	assert.NotNil(t, executor)
}

func TestPluginEntrypointErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	step := &pb.UnitStep{
		Id: "test",
		Step: &pb.UnitStep_Plugin{
			Plugin: &pb.PluginStep{
				Image: "plugin/drone-git",
			},
		},
	}

	oldImgMetadata := getImgMetadata
	getImgMetadata = func(ctx context.Context, id, image, secret string, log *zap.SugaredLogger) ([]string, []string, error) {
		return nil, nil, fmt.Errorf("entrypoint not found")
	}
	defer func() { getImgMetadata = oldImgMetadata }()

	var buf bytes.Buffer
	executor := NewPluginTask(step, nil, log.Sugar(), &buf, false, log.Sugar())
	_, _, err := executor.Run(ctx)
	assert.NotNil(t, err)
}

func TestPluginEmptyEntrypointErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	step := &pb.UnitStep{
		Id: "test",
		Step: &pb.UnitStep_Plugin{
			Plugin: &pb.PluginStep{
				Image: "plugin/drone-git",
			},
		},
	}

	oldImgMetadata := getImgMetadata
	getImgMetadata = func(ctx context.Context, id, image, secret string, log *zap.SugaredLogger) ([]string, []string, error) {
		return nil, nil, nil
	}
	defer func() { getImgMetadata = oldImgMetadata }()

	var buf bytes.Buffer
	executor := NewPluginTask(step, nil, log.Sugar(), &buf, false, log.Sugar())
	_, _, err := executor.Run(ctx)
	assert.NotNil(t, err)
}
