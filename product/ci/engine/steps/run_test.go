package steps

import (
	"context"
	"fmt"
	"os"
	"os/exec"
	"strconv"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/pkg/errors"
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

func TestRunStepValidate(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	e := runStep{
		log: log.Sugar(),
	}
	err := e.validate()
	assert.NotNil(t, err)

	e = runStep{
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
	e := runStep{
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
	e := runStep{
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
	e := runStep{
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
	e := runStep{
		id:          "id",
		log:         log.Sugar(),
		commands:    []string{"l"},
		tmpFilePath: "/tmp",
		fs:          fs,
		timeoutSecs: 5,
	}

	fs.EXPECT().Create(gomock.Any()).Return(logFile, nil)
	logFile.EXPECT().Close().Return(nil)

	err := e.execute(ctx, retryCount)
	assert.Nil(t, err)
}

func TestExecuteErrorWithOutput(t *testing.T) {
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
	e := runStep{
		id:            "id",
		log:           log.Sugar(),
		commands:      []string{"l"},
		tmpFilePath:   "/tmp",
		envVarOutputs: []string{"abc", "abc1"},
		fs:            fs,
		timeoutSecs:   5,
	}

	fs.EXPECT().Create(gomock.Any()).Return(logFile, nil)
	fs.EXPECT().Open(gomock.Any()).Return(nil, errors.New(
		fmt.Sprintf("Error while opening file")))
	logFile.EXPECT().Close().Return(nil)

	err := e.execute(ctx, retryCount)
	assert.NotNil(t, err)
}

func TestExecuteSuccessWithOutput(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	filePath := "/tmp/idoutput.txt"
	defer ctrl.Finish()

	originalExecCmdCtx := execCmdCtx
	defer func() { execCmdCtx = originalExecCmdCtx }()
	execCmdCtx = fakeExecCommandWithContext
	mockedExitStatus = 0

	var retryCount int32 = 1
	fs := filesystem.NewMockFileSystem(ctrl)
	logFile := filesystem.NewMockFile(ctrl)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	f, err := os.Create(filePath)
	if err != nil {
		panic(err)
	}
	f.WriteString("abc xyz\n")
	f.WriteString("abc1")
	f.Close()

	f1, err := os.Open(filePath)
	e := runStep{
		id:            "id",
		log:           log.Sugar(),
		commands:      []string{"export abc=xyz"},
		tmpFilePath:   "/tmp",
		envVarOutputs: []string{"abc", "abc1"},
		fs:            fs,
		timeoutSecs:   5,
	}

	fs.EXPECT().Open(gomock.Any()).Return(f1, nil)
	fs.EXPECT().Create(gomock.Any()).Return(logFile, nil)
	logFile.EXPECT().Close().Return(nil)

	errExec := e.execute(ctx, retryCount)
	assert.Nil(t, errExec)
}

func TestFetchOutputVariables(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	filePath := "/tmp/idoutput.txt"
	defer ctrl.Finish()

	originalExecCmdCtx := execCmdCtx
	defer func() { execCmdCtx = originalExecCmdCtx }()
	execCmdCtx = fakeExecCommandWithContext
	mockedExitStatus = 0
	fs := filesystem.NewMockFileSystem(ctrl)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	f, err := os.Create(filePath)
	if err != nil {
		panic(err)
	}
	f.WriteString("abc xyz")
	f.Close()

	f1, err := os.Open(filePath)
	e := runStep{
		id:          "id",
		log:         log.Sugar(),
		commands:    []string{"l"},
		tmpFilePath: "tmp",
		fs:          fs,
		timeoutSecs: 5,
	}
	fs.EXPECT().Open(gomock.Any()).Return(f1, nil)
	expectedEnvVarMap := make(map[string]string)
	expectedEnvVarMap["abc"] = "xyz"
	envVarMap, err := e.fetchOutputVariables(filePath)
	assert.Nil(t, err)
	assert.Equal(t, expectedEnvVarMap, envVarMap)
	os.Remove(filePath)
}

func TestRunValidateErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	logFilePath := "/a/"
	tmpPath := "/tmp/"
	fs := filesystem.NewMockFileSystem(ctrl)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	executor := NewRunStep(nil, logFilePath, tmpPath, fs, log.Sugar())
	err := executor.Run(ctx)
	assert.NotNil(t, err)
}

func TestRunExecuteErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()
	tmpPath := "/tmp/"
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

	executor := NewRunStep(step, logFilePath, tmpPath, fs, log.Sugar())
	err := executor.Run(ctx)
	assert.NotNil(t, err)
}
