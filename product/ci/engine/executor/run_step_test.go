package executor

import (
	"context"
	// "fmt"
	"os"
	"os/exec"
	"strconv"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/filesystem"
	"github.com/wings-software/portal/commons/go/lib/logs"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

var mockedExitStatus = 0

func TestExecCommandHelper(t *testing.T) {
	if os.Getenv("GO_WANT_HELPER_PROCESS") != "1" {
		return
	}

	i, _ := strconv.Atoi(os.Getenv("EXIT_STATUS"))
	os.Exit(i)
}

func fakeExecCommandWithContext(ctx context.Context, command string, args ...string) *exec.Cmd {
	cs := []string{"-test.run=TestExecCommandHelper", "--", command}
	cs = append(cs, args...)
	cmd := exec.Command(os.Args[0], cs...)
	es := strconv.Itoa(mockedExitStatus)
	cmd.Env = []string{"GO_WANT_HELPER_PROCESS=1",
		"STDOUT=" + "",
		"STDERR=" + "",
		"EXIT_STATUS=" + es}
	return cmd
}

func TestValidate(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	e := runStepExecutor{
		log: log.Sugar(),
	}
	err := e.validate()
	assert.NotNil(t, err)

	e = runStepExecutor{
		commands: []string{"ls"},
		log:      log.Sugar(),
	}
	err = e.validate()
	assert.Nil(t, err)
}

func TestExecuteFileNotFound(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	var retryCount int32 = 1
	fs := filesystem.NewMockFileSystem(ctrl)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	e := runStepExecutor{
		log:      log.Sugar(),
		commands: []string{"ls"},
		fs:       fs,
	}

	fs.EXPECT().Create(gomock.Any()).Return(nil, os.ErrPermission)
	err := e.execute(ctx, retryCount)
	assert.Equal(t, err, os.ErrPermission)
}

func TestExecuteDeadlineExceeded(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	originalExecCmdCtx := execCmdCtx
	defer func() { execCmdCtx = originalExecCmdCtx }()
	execCmdCtx = fakeExecCommandWithContext
	mockedExitStatus = 0

	var retryCount int32 = 1
	fs := filesystem.NewMockFileSystem(ctrl)
	logFile := filesystem.NewMockFile(ctrl)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	e := runStepExecutor{
		log:         log.Sugar(),
		commands:    []string{"l"},
		fs:          fs,
		timeoutSecs: 0,
	}

	fs.EXPECT().Create(gomock.Any()).Return(logFile, nil)
	logFile.EXPECT().Close().Return(nil)

	err := e.execute(ctx, retryCount)
	assert.Equal(t, err, context.DeadlineExceeded)
}

func TestExecuteNonZeroStatus(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	originalExecCmdCtx := execCmdCtx
	defer func() { execCmdCtx = originalExecCmdCtx }()
	execCmdCtx = fakeExecCommandWithContext
	mockedExitStatus = 1

	var retryCount int32 = 1
	fs := filesystem.NewMockFileSystem(ctrl)
	logFile := filesystem.NewMockFile(ctrl)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	e := runStepExecutor{
		log:         log.Sugar(),
		commands:    []string{"l"},
		fs:          fs,
		timeoutSecs: 5,
	}

	fs.EXPECT().Create(gomock.Any()).Return(logFile, nil)
	logFile.EXPECT().Close().Return(nil)

	err := e.execute(ctx, retryCount)
	assert.NotNil(t, err)
	if err, ok := err.(*exec.ExitError); ok {
		assert.Equal(t, err.ExitCode(), mockedExitStatus)
	} else {
		t.Fatalf("Expected err of type exec.ExitError")
	}
}

func TestExecuteSuccess(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	originalExecCmdCtx := execCmdCtx
	defer func() { execCmdCtx = originalExecCmdCtx }()
	execCmdCtx = fakeExecCommandWithContext
	mockedExitStatus = 0

	var retryCount int32 = 1
	fs := filesystem.NewMockFileSystem(ctrl)
	logFile := filesystem.NewMockFile(ctrl)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	e := runStepExecutor{
		log:         log.Sugar(),
		commands:    []string{"l"},
		fs:          fs,
		timeoutSecs: 5,
	}

	fs.EXPECT().Create(gomock.Any()).Return(logFile, nil)
	logFile.EXPECT().Close().Return(nil)

	err := e.execute(ctx, retryCount)
	assert.Nil(t, err)
}

func TestRunValidateErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	logFilePath := "/a/"
	fs := filesystem.NewMockFileSystem(ctrl)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	executor := NewRunStepExecutor(nil, logFilePath, fs, log.Sugar())
	err := executor.Run(ctx)
	assert.NotNil(t, err)
}

func TestRunExecuteErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	logFilePath := "/a/"
	fs := filesystem.NewMockFileSystem(ctrl)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	step := &pb.Step{
		Id: "test",
		Step: &pb.Step_Run{
			Run: &pb.RunStep{
				Commands: []string{"cd .", "ls"},
			},
		},
	}

	fs.EXPECT().Create(gomock.Any()).Return(nil, os.ErrPermission)

	executor := NewRunStepExecutor(step, logFilePath, fs, log.Sugar())
	err := executor.Run(ctx)
	assert.NotNil(t, err)
}
