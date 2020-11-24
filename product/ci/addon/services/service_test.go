package services

import (
	"bytes"
	"context"
	"fmt"
	"os/exec"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	mexec "github.com/wings-software/portal/commons/go/lib/exec"
	"github.com/wings-software/portal/commons/go/lib/logs"
	"go.uber.org/zap"
)

func TestIntegrationSvcSuccess(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	var buf bytes.Buffer
	entrypoint := []string{"git"}
	args := []string{"status"}
	svcID := "git-clone"
	image := "alpine/git"

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	cmdFactory := mexec.NewMockCommandFactory(ctrl)
	svc := &integrationSvc{
		id:         svcID,
		image:      image,
		entrypoint: entrypoint,
		args:       args,
		log:        log.Sugar(),
		procWriter: &buf,
		cmdFactory: cmdFactory,
	}

	cmd := mexec.NewMockCommand(ctrl)
	cmdFactory.EXPECT().Command(entrypoint[0], args[0]).Return(cmd)
	cmd.EXPECT().WithStdout(&buf).Return(cmd)
	cmd.EXPECT().WithStderr(&buf).Return(cmd)
	cmd.EXPECT().WithEnvVarsMap(nil).Return(cmd)
	cmd.EXPECT().Run().Return(nil)

	err := svc.Run()
	assert.Nil(t, err)
}

func TestIntegrationSvcNonZeroStatus(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	var buf bytes.Buffer
	entrypoint := []string{"git"}
	args := []string{"status"}
	svcID := "git-clone"
	image := "alpine/git"

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	cmdFactory := mexec.NewMockCommandFactory(ctrl)
	svc := &integrationSvc{
		id:         svcID,
		image:      image,
		entrypoint: entrypoint,
		args:       args,
		log:        log.Sugar(),
		procWriter: &buf,
		cmdFactory: cmdFactory,
	}

	cmd := mexec.NewMockCommand(ctrl)
	cmdFactory.EXPECT().Command(entrypoint[0], args[0]).Return(cmd)
	cmd.EXPECT().WithStdout(&buf).Return(cmd)
	cmd.EXPECT().WithStderr(&buf).Return(cmd)
	cmd.EXPECT().WithEnvVarsMap(nil).Return(cmd)
	cmd.EXPECT().Run().Return(&exec.ExitError{})

	err := svc.Run()
	assert.NotNil(t, err)
	if _, ok := err.(*exec.ExitError); !ok {
		t.Fatalf("Expected err of type exec.ExitError")
	}
}

func TestServiceCreate(t *testing.T) {
	var buf bytes.Buffer
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	entrypoint := []string{"git"}
	args := []string{"status"}
	svcID := "git-clone"
	image := "alpine/git"

	executor := NewIntegrationSvc(svcID, image, entrypoint, args, log.Sugar(), &buf)
	assert.NotNil(t, executor)
}

func TestServiceEntrypointErr(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	entrypoint := []string{}
	args := []string{}
	svcID := "git-clone"
	image := "alpine-git"

	oldImgMetadata := getImgMetadata
	getImgMetadata = func(ctx context.Context, id, image, secret string, log *zap.SugaredLogger) ([]string, []string, error) {
		return nil, nil, fmt.Errorf("entrypoint not found")
	}
	defer func() { getImgMetadata = oldImgMetadata }()

	var buf bytes.Buffer
	executor := NewIntegrationSvc(svcID, image, entrypoint, args, log.Sugar(), &buf)
	err := executor.Run()
	assert.NotNil(t, err)
}

func TestServiceEmptyEntrypointErr(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	entrypoint := []string{}
	args := []string{}
	svcID := "git-clone"
	image := "alpine-git"

	oldImgMetadata := getImgMetadata
	getImgMetadata = func(ctx context.Context, id, image, secret string, log *zap.SugaredLogger) ([]string, []string, error) {
		return nil, nil, nil
	}
	defer func() { getImgMetadata = oldImgMetadata }()

	var buf bytes.Buffer
	executor := NewIntegrationSvc(svcID, image, entrypoint, args, log.Sugar(), &buf)
	err := executor.Run()
	assert.NotNil(t, err)
}
