package tasks

import (
	"bytes"
	"context"
	"fmt"
	"os"
	"os/exec"
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
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	e := pluginTask{
		id:                "step1",
		image:             "plugin/drone-git",
		timeoutSecs:       5,
		numRetries:        numRetries,
		log:               log.Sugar(),
		cmdContextFactory: cmdFactory,
		procWriter:        &buf,
	}

	oldImgEntrypoint := getPublicEntrypoint
	getPublicEntrypoint = func(image string) ([]string, error) {
		return commands, nil
	}
	defer func() { getPublicEntrypoint = oldImgEntrypoint }()

	cmdFactory.EXPECT().CmdContextWithSleep(gomock.Any(), cmdExitWaitTime, gomock.Any()).Return(cmd)
	cmd.EXPECT().WithStdout(&buf).Return(cmd)
	cmd.EXPECT().WithStderr(&buf).Return(cmd)
	cmd.EXPECT().WithEnvVarsMap(nil).Return(cmd)
	cmd.EXPECT().Run().Return(nil)

	retries, err := e.Run(ctx)
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
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	e := pluginTask{
		id:                "step1",
		image:             "plugin/drone-git",
		timeoutSecs:       5,
		numRetries:        numRetries,
		log:               log.Sugar(),
		cmdContextFactory: cmdFactory,
		procWriter:        &buf,
	}

	oldImgEntrypoint := getPublicEntrypoint
	getPublicEntrypoint = func(image string) ([]string, error) {
		return commands, nil
	}
	defer func() { getPublicEntrypoint = oldImgEntrypoint }()

	cmdFactory.EXPECT().CmdContextWithSleep(gomock.Any(), cmdExitWaitTime, gomock.Any()).Return(cmd)
	cmd.EXPECT().WithStdout(&buf).Return(cmd)
	cmd.EXPECT().WithStderr(&buf).Return(cmd)
	cmd.EXPECT().WithEnvVarsMap(nil).Return(cmd)
	cmd.EXPECT().Run().Return(&exec.ExitError{})

	retries, err := e.Run(ctx)
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
	executor := NewPluginTask(step, log.Sugar(), &buf)
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

	oldImgEntrypoint := getPublicEntrypoint
	getPublicEntrypoint = func(image string) ([]string, error) {
		return nil, fmt.Errorf("entrypoint not found")
	}
	defer func() { getPublicEntrypoint = oldImgEntrypoint }()

	var buf bytes.Buffer
	executor := NewPluginTask(step, log.Sugar(), &buf)
	_, err := executor.Run(ctx)
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

	oldImgEntrypoint := getPublicEntrypoint
	getPublicEntrypoint = func(image string) ([]string, error) {
		return nil, nil
	}
	defer func() { getPublicEntrypoint = oldImgEntrypoint }()

	var buf bytes.Buffer
	executor := NewPluginTask(step, log.Sugar(), &buf)
	_, err := executor.Run(ctx)
	assert.NotNil(t, err)
}

func TestPrivatePluginEnvNotFoundErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	imgSecretEnv := "DOCKER_CONFIG"
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	step := &pb.UnitStep{
		Id: "test",
		Step: &pb.UnitStep_Plugin{
			Plugin: &pb.PluginStep{
				Image:          "plugin/drone-git",
				ImageSecretEnv: imgSecretEnv,
			},
		},
	}

	var buf bytes.Buffer
	executor := NewPluginTask(step, log.Sugar(), &buf)
	_, err := executor.Run(ctx)
	assert.NotNil(t, err)
}

func TestPrivatePluginErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	imgSecretEnv := "DOCKER_CONFIG"
	imgSecret := "test"
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	step := &pb.UnitStep{
		Id: "test",
		Step: &pb.UnitStep_Plugin{
			Plugin: &pb.PluginStep{
				Image:          "plugin/drone-git",
				ImageSecretEnv: imgSecretEnv,
			},
		},
	}

	if err := os.Setenv(imgSecretEnv, imgSecret); err != nil {
		t.Fatalf("failed to set environment variable")
	}
	defer os.Unsetenv(imgSecretEnv)

	var buf bytes.Buffer
	executor := NewPluginTask(step, log.Sugar(), &buf)
	_, err := executor.Run(ctx)
	assert.NotNil(t, err)
}
