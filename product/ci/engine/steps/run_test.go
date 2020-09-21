package steps

import (
	"bytes"
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
	"github.com/wings-software/portal/product/ci/engine/output"
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
	o, err := e.execute(ctx, retryCount)
	assert.Equal(t, err, os.ErrPermission)
	assert.Nil(t, o)
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

	oldStartTailFn := startTailFn
	startTailFn = func(ctx context.Context, log *zap.SugaredLogger, filename string, additionalFields map[string]string) error {
		return nil
	}
	defer func() { startTailFn = oldStartTailFn }()

	oldStopTailFn := stopTailFn
	stopTailFn = func(ctx context.Context, log *zap.SugaredLogger, filename string, wait bool) error {
		return nil
	}
	defer func() { stopTailFn = oldStopTailFn }()

	fs.EXPECT().Create(gomock.Any()).Return(logFile, nil)
	logFile.EXPECT().Close().Return(nil)

	o, err := e.execute(ctx, retryCount)
	assert.Equal(t, err, context.DeadlineExceeded)
	assert.Nil(t, o)
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

	oldStartTailFn := startTailFn
	startTailFn = func(ctx context.Context, log *zap.SugaredLogger, filename string, additionalFields map[string]string) error {
		return nil
	}
	defer func() { startTailFn = oldStartTailFn }()

	oldStopTailFn := stopTailFn
	stopTailFn = func(ctx context.Context, log *zap.SugaredLogger, filename string, wait bool) error {
		return nil
	}
	defer func() { stopTailFn = oldStopTailFn }()

	fs.EXPECT().Create(gomock.Any()).Return(logFile, nil)
	logFile.EXPECT().Close().Return(nil)

	o, err := e.execute(ctx, retryCount)
	assert.NotNil(t, err)
	if err, ok := err.(*exec.ExitError); ok {
		assert.Equal(t, err.ExitCode(), mockedExitStatus)
	} else {
		t.Fatalf("Expected err of type exec.ExitError")
	}
	assert.Nil(t, o)
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

	oldStartTailFn := startTailFn
	startTailFn = func(ctx context.Context, log *zap.SugaredLogger, filename string, additionalFields map[string]string) error {
		return nil
	}
	defer func() { startTailFn = oldStartTailFn }()

	oldStopTailFn := stopTailFn
	stopTailFn = func(ctx context.Context, log *zap.SugaredLogger, filename string, wait bool) error {
		return nil
	}
	defer func() { stopTailFn = oldStopTailFn }()

	fs.EXPECT().Create(gomock.Any()).Return(logFile, nil)
	logFile.EXPECT().Close().Return(nil)

	o, err := e.execute(ctx, retryCount)
	assert.Nil(t, err)
	assert.Nil(t, o)
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

	oldStartTailFn := startTailFn
	startTailFn = func(ctx context.Context, log *zap.SugaredLogger, filename string, additionalFields map[string]string) error {
		return nil
	}
	defer func() { startTailFn = oldStartTailFn }()

	oldStopTailFn := stopTailFn
	stopTailFn = func(ctx context.Context, log *zap.SugaredLogger, filename string, wait bool) error {
		return nil
	}
	defer func() { stopTailFn = oldStopTailFn }()

	fs.EXPECT().Create(gomock.Any()).Return(logFile, nil)
	fs.EXPECT().Open(gomock.Any()).Return(nil, errors.New(
		fmt.Sprintf("Error while opening file")))
	logFile.EXPECT().Close().Return(nil)

	o, err := e.execute(ctx, retryCount)
	assert.NotNil(t, err)
	assert.Nil(t, o)
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

	envVar := "abc"
	envVal := "xyz"
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

	oldStartTailFn := startTailFn
	startTailFn = func(ctx context.Context, log *zap.SugaredLogger, filename string, additionalFields map[string]string) error {
		return nil
	}
	defer func() { startTailFn = oldStartTailFn }()

	oldStopTailFn := stopTailFn
	stopTailFn = func(ctx context.Context, log *zap.SugaredLogger, filename string, wait bool) error {
		return nil
	}
	defer func() { stopTailFn = oldStopTailFn }()

	fs.EXPECT().Open(gomock.Any()).Return(f1, nil)
	fs.EXPECT().Create(gomock.Any()).Return(logFile, nil)
	logFile.EXPECT().Close().Return(nil)

	o, errExec := e.execute(ctx, retryCount)
	assert.Nil(t, errExec)
	assert.Equal(t, o.Output[envVar], envVal)
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

	var buf bytes.Buffer
	executor := NewRunStep(nil, logFilePath, tmpPath, nil, fs, log.Sugar(), &buf)
	o, numRetries, err := executor.Run(ctx)
	assert.NotNil(t, err)
	assert.Nil(t, o)
	assert.Equal(t, numRetries, int32(1))
}

func TestRunExecuteErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()
	tmpPath := "/tmp/"
	logFilePath := "/a/"
	fs := filesystem.NewMockFileSystem(ctrl)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	step := &pb.UnitStep{
		Id: "test",
		Step: &pb.UnitStep_Run{
			Run: &pb.RunStep{
				Commands: []string{"cd .", "ls"},
			},
		},
	}

	fs.EXPECT().Create(gomock.Any()).Return(nil, os.ErrPermission)

	var buf bytes.Buffer
	executor := NewRunStep(step, logFilePath, tmpPath, nil, fs, log.Sugar(), &buf)
	o, numRetries, err := executor.Run(ctx)
	assert.NotNil(t, err)
	assert.Nil(t, o)
	assert.Equal(t, numRetries, int32(1))
}

func TestRunStepResolveJEXL(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	jCmd1 := "${step1.output.foo}"
	cmd1Val := "bar"

	tests := []struct {
		name         string
		commands     []string
		resolvedCmds []string
		jexlEvalRet  map[string]string
		jexlEvalErr  error
		expectedErr  bool
	}{
		{
			name:        "jexl evaluate error",
			commands:    []string{jCmd1},
			jexlEvalRet: nil,
			jexlEvalErr: errors.New("evaluation failed"),
			expectedErr: true,
		},
		{
			name:         "jexl successfully evaluated",
			commands:     []string{jCmd1},
			jexlEvalRet:  map[string]string{jCmd1: cmd1Val},
			jexlEvalErr:  nil,
			resolvedCmds: []string{cmd1Val},
			expectedErr:  false,
		},
	}
	oldJEXLEval := evaluateJEXL
	defer func() { evaluateJEXL = oldJEXLEval }()
	for _, tc := range tests {
		s := &runStep{
			commands: tc.commands,
			log:      log.Sugar(),
		}
		// Initialize a mock CI addon
		evaluateJEXL = func(ctx context.Context, expressions []string, o output.StageOutput,
			log *zap.SugaredLogger) (map[string]string, error) {
			return tc.jexlEvalRet, tc.jexlEvalErr
		}
		got := s.resolveJEXL(ctx)
		if tc.expectedErr == (got == nil) {
			t.Fatalf("%s: expected error: %v, got: %v", tc.name, tc.expectedErr, got)
		}

		if got == nil {
			assert.Equal(t, s.commands, tc.resolvedCmds)
		}
	}
}
