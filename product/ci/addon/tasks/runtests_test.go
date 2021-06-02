package tasks

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	mexec "github.com/wings-software/portal/commons/go/lib/exec"
	"os"
	"testing"
	"time"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/exec"
	"github.com/wings-software/portal/commons/go/lib/filesystem"
	"github.com/wings-software/portal/commons/go/lib/logs"
	"github.com/wings-software/portal/commons/go/lib/utils"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"github.com/wings-software/portal/product/ci/ti-service/types"
	"go.uber.org/zap"
)

func TestCreateJavaAgentArg(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	fs := filesystem.NewMockFileSystem(ctrl)

	tmpFilePath := "/test/tmp"
	annotations := "a1, a2, a3"
	packages := "p1, p2, p3"

	r := runTestsTask{
		id:                   "id",
		runOnlySelectedTests: true,
		fs:                   fs,
		tmpFilePath:          tmpFilePath,
		annotations:          annotations,
		packages:             packages,
		log:                  log.Sugar(),
		addonLogger:          log.Sugar(),
	}

	expDir := fmt.Sprintf(outDir, tmpFilePath)
	expData := `outDir: /test/tmp/ti/callgraph/
logLevel: 0
logConsole: false
writeTo: COVERAGE_JSON
instrPackages: p1, p2, p3
testAnnotations: a1, a2, a3`
	fs.EXPECT().MkdirAll(expDir, os.ModePerm).Return(nil)
	mf := filesystem.NewMockFile(ctrl)
	mf.EXPECT().Write([]byte(expData)).Return(0, nil)
	fs.EXPECT().Create("/test/tmp/config.ini").Return(mf, nil)

	arg, err := r.createJavaAgentArg()
	assert.Nil(t, err)
	assert.Equal(t, arg, fmt.Sprintf(javaAgentArg, "/test/tmp/config.ini"))
}

func TestCreateJavaAgentArg_WithWriteFailure(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	fs := filesystem.NewMockFileSystem(ctrl)

	tmpFilePath := "/test/tmp"
	packages := "p1, p2, p3"

	r := runTestsTask{
		id:                   "id",
		runOnlySelectedTests: true,
		fs:                   fs,
		tmpFilePath:          tmpFilePath,
		packages:             packages,
		log:                  log.Sugar(),
		addonLogger:          log.Sugar(),
	}

	expDir := fmt.Sprintf(outDir, tmpFilePath)
	expData := `outDir: /test/tmp/ti/callgraph/
logLevel: 0
logConsole: false
writeTo: COVERAGE_JSON
instrPackages: p1, p2, p3`
	fs.EXPECT().MkdirAll(expDir, os.ModePerm).Return(nil)
	mf := filesystem.NewMockFile(ctrl)
	mf.EXPECT().Write([]byte(expData)).Return(0, errors.New("could not write data"))
	fs.EXPECT().Create("/test/tmp/config.ini").Return(mf, nil)

	_, err := r.createJavaAgentArg()
	assert.NotNil(t, err)
}

func TestGetMavenCmd(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	fs := filesystem.NewMockFileSystem(ctrl)

	tmpFilePath := "/test/tmp"
	packages := "p1, p2, p3"

	expDir := fmt.Sprintf(outDir, tmpFilePath)
	expData := `outDir: /test/tmp/ti/callgraph/
logLevel: 0
logConsole: false
writeTo: COVERAGE_JSON
instrPackages: p1, p2, p3`
	fs.EXPECT().MkdirAll(expDir, os.ModePerm).Return(nil).AnyTimes()
	mf := filesystem.NewMockFile(ctrl)
	mf.EXPECT().Write([]byte(expData)).Return(0, nil).AnyTimes()
	fs.EXPECT().Create("/test/tmp/config.ini").Return(mf, nil).AnyTimes()

	t1 := types.RunnableTest{Pkg: "pkg1", Class: "cls1", Method: "m1"}
	t2 := types.RunnableTest{Pkg: "pkg2", Class: "cls2", Method: "m2"}

	tests := []struct {
		name                 string // description of test
		args                 string
		runOnlySelectedTests bool
		want                 string
		expectedErr          bool
		tests                []types.RunnableTest
	}{
		{
			name:                 "run all tests with non-empty test list and -Duser parameters",
			args:                 "clean test -Duser.timezone=US/Mountain -Duser.locale=en/US",
			runOnlySelectedTests: false,
			want:                 "mvn -am -DargLine=\"-Duser.timezone=US/Mountain -Duser.locale=en/US -javaagent:/addon/bin/java-agent.jar=/test/tmp/config.ini\" clean test",
			expectedErr:          false,
			tests:                []types.RunnableTest{t1, t2},
		},
		{
			name:                 "run all tests with empty test list and no -Duser parameters",
			args:                 "clean test",
			runOnlySelectedTests: false,
			want:                 "mvn -am -DargLine=-javaagent:/addon/bin/java-agent.jar=/test/tmp/config.ini clean test",
			expectedErr:          false,
			tests:                []types.RunnableTest{},
		},
		{
			name:                 "run selected tests with given test list and -Duser parameters",
			args:                 "clean test -Duser.timezone=US/Mountain -Duser.locale=en/US",
			runOnlySelectedTests: true,
			want:                 "mvn -Dtest=pkg1.cls1,pkg2.cls2 -am -DargLine=\"-Duser.timezone=US/Mountain -Duser.locale=en/US -javaagent:/addon/bin/java-agent.jar=/test/tmp/config.ini\" clean test",
			expectedErr:          false,
			tests:                []types.RunnableTest{t1, t2},
		},
		{
			name:                 "run selected tests with zero tests and -Duser parameters",
			args:                 "clean test -Duser.timezone=US/Mountain -Duser.locale=en/US",
			runOnlySelectedTests: true,
			want:                 "echo \"Skipping test run, received no tests to execute\"",
			expectedErr:          false,
			tests:                []types.RunnableTest{},
		},
		{
			name:                 "run selected tests with repeating test list and -Duser parameters",
			args:                 "clean test -B -2C-Duser.timezone=US/Mountain -Duser.locale=en/US",
			runOnlySelectedTests: true,
			want:                 "mvn -Dtest=pkg1.cls1,pkg2.cls2 -am -DargLine=\"-Duser.timezone=US/Mountain -Duser.locale=en/US -javaagent:/addon/bin/java-agent.jar=/test/tmp/config.ini\" clean test -B -2C",
			expectedErr:          false,
			tests:                []types.RunnableTest{t1, t2, t1, t2},
		},
		{
			name:                 "run selected tests with single test and -Duser parameters and or condition",
			args:                 "clean test -B -2C -Duser.timezone=US/Mountain -Duser.locale=en/US || true",
			runOnlySelectedTests: true,
			want:                 "mvn -Dtest=pkg2.cls2 -am -DargLine=\"-Duser.timezone=US/Mountain -Duser.locale=en/US -javaagent:/addon/bin/java-agent.jar=/test/tmp/config.ini\" clean test -B -2C   || true",
			expectedErr:          false,
			tests:                []types.RunnableTest{t2},
		},
	}

	for _, tc := range tests {
		r := runTestsTask{
			id:                   "id",
			runOnlySelectedTests: tc.runOnlySelectedTests,
			fs:                   fs,
			tmpFilePath:          tmpFilePath,
			args:                 tc.args,
			packages:             packages,
			log:                  log.Sugar(),
			addonLogger:          log.Sugar(),
		}

		got, err := r.getMavenCmd(tc.tests)
		if tc.expectedErr == (err == nil) {
			t.Fatalf("%s: expected error: %v, got: %v", tc.name, tc.expectedErr, got)
		}
		assert.Equal(t, got, tc.want)
	}
}

func TestGetBazelCmd(t *testing.T) {
	// Bazel impl is pretty hacky right now and tailored to running portal.
	// Will add this once we have a more generic implementation.
}

func TestGetCmd(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	fs := filesystem.NewMockFileSystem(ctrl)

	t1 := types.RunnableTest{Pkg: "pkg1", Class: "cls1", Method: "m1"}
	t2 := types.RunnableTest{Pkg: "pkg2", Class: "cls1", Method: "m2"}

	tmpFilePath := "/test/tmp"
	packages := "p1, p2, p3"

	expDir := fmt.Sprintf(outDir, tmpFilePath)
	expData := `outDir: /test/tmp/ti/callgraph/
logLevel: 0
logConsole: false
writeTo: COVERAGE_JSON
instrPackages: p1, p2, p3`
	fs.EXPECT().MkdirAll(expDir, os.ModePerm).Return(nil).AnyTimes()
	mf := filesystem.NewMockFile(ctrl)
	mf.EXPECT().Write([]byte(expData)).Return(0, nil).AnyTimes()
	fs.EXPECT().Create("/test/tmp/config.ini").Return(mf, nil).AnyTimes()

	diffFiles, _ := json.Marshal([]types.File{})

	r := runTestsTask{
		id:                   "id",
		runOnlySelectedTests: true,
		fs:                   fs,
		preCommand:           "echo x",
		diffFiles:            string(diffFiles),
		args:                 "clean test",
		postCommand:          "echo y",
		buildTool:            "maven",
		tmpFilePath:          tmpFilePath,
		packages:             packages,
		log:                  log.Sugar(),
		addonLogger:          log.Sugar(),
	}

	oldSelect := selectTestsFn
	defer func() {
		selectTestsFn = oldSelect
	}()
	selectTestsFn = func(ctx context.Context, f []types.File, id string, log *zap.SugaredLogger, fs filesystem.FileSystem) (types.SelectTestsResp, error) {
		return types.SelectTestsResp{
			SelectAll: false,
			Tests:     []types.RunnableTest{t1, t2}}, nil
	}

	want, err := utils.GetLoggableCmd(`set -e
export TMPDIR=/test/tmp
echo x
mvn -Dtest=pkg1.cls1,pkg2.cls1 -am -DargLine=-javaagent:/addon/bin/java-agent.jar=/test/tmp/config.ini clean test
echo y`)
	if err != nil {
		t.Fatalf("could not get loggable cmd for %s", want)
	}
	got, err := r.getCmd(ctx)
	assert.Nil(t, err)
	assert.Equal(t, r.runOnlySelectedTests, true) // If no errors, we should run only selected tests
	assert.Equal(t, got, want)
}

func TestGetCmd_SelectAll(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	fs := filesystem.NewMockFileSystem(ctrl)

	t1 := types.RunnableTest{Pkg: "pkg1", Class: "cls1", Method: "m1"}
	t2 := types.RunnableTest{Pkg: "pkg2", Class: "cls2", Method: "m2"}

	tmpFilePath := "/test/tmp"
	packages := "p1, p2, p3"

	expDir := fmt.Sprintf(outDir, tmpFilePath)
	expData := `outDir: /test/tmp/ti/callgraph/
logLevel: 0
logConsole: false
writeTo: COVERAGE_JSON
instrPackages: p1, p2, p3`
	fs.EXPECT().MkdirAll(expDir, os.ModePerm).Return(nil).AnyTimes()
	mf := filesystem.NewMockFile(ctrl)
	mf.EXPECT().Write([]byte(expData)).Return(0, nil).AnyTimes()
	fs.EXPECT().Create("/test/tmp/config.ini").Return(mf, nil).AnyTimes()

	diffFiles, _ := json.Marshal([]types.File{})

	r := runTestsTask{
		id:                   "id",
		runOnlySelectedTests: true,
		fs:                   fs,
		preCommand:           "echo x",
		diffFiles:            string(diffFiles),
		args:                 "clean test",
		postCommand:          "echo y",
		buildTool:            "maven",
		tmpFilePath:          tmpFilePath,
		packages:             packages,
		log:                  log.Sugar(),
		addonLogger:          log.Sugar(),
	}

	oldSelect := selectTestsFn
	defer func() {
		selectTestsFn = oldSelect
	}()
	selectTestsFn = func(ctx context.Context, f []types.File, id string, log *zap.SugaredLogger, fs filesystem.FileSystem) (types.SelectTestsResp, error) {
		return types.SelectTestsResp{
			SelectAll: true,
			Tests:     []types.RunnableTest{t1, t2}}, nil
	}

	want, err := utils.GetLoggableCmd(`set -e
export TMPDIR=/test/tmp
echo x
mvn -am -DargLine=-javaagent:/addon/bin/java-agent.jar=/test/tmp/config.ini clean test
echo y`)
	if err != nil {
		t.Fatalf("could not get loggable cmd for %s", want)
	}
	got, err := r.getCmd(ctx)
	assert.Nil(t, err)
	assert.Equal(t, r.runOnlySelectedTests, false) // Since selection returns all the tests
	assert.Equal(t, got, want)
}

func TestGetCmd_RunAll(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	fs := filesystem.NewMockFileSystem(ctrl)

	tmpFilePath := "/test/tmp"
	packages := "p1, p2, p3"

	expDir := fmt.Sprintf(outDir, tmpFilePath)
	expData := `outDir: /test/tmp/ti/callgraph/
logLevel: 0
logConsole: false
writeTo: COVERAGE_JSON
instrPackages: p1, p2, p3`
	fs.EXPECT().MkdirAll(expDir, os.ModePerm).Return(nil).AnyTimes()
	mf := filesystem.NewMockFile(ctrl)
	mf.EXPECT().Write([]byte(expData)).Return(0, nil).AnyTimes()
	fs.EXPECT().Create("/test/tmp/config.ini").Return(mf, nil).AnyTimes()

	diffFiles, _ := json.Marshal([]types.File{})

	r := runTestsTask{
		id:                   "id",
		runOnlySelectedTests: true,
		fs:                   fs,
		preCommand:           "echo x",
		diffFiles:            string(diffFiles),
		args:                 "clean test",
		postCommand:          "echo y",
		buildTool:            "maven",
		tmpFilePath:          tmpFilePath,
		packages:             packages,
		log:                  log.Sugar(),
		addonLogger:          log.Sugar(),
	}

	oldSelect := selectTestsFn
	defer func() {
		selectTestsFn = oldSelect
	}()
	selectTestsFn = func(ctx context.Context, f []types.File, id string, log *zap.SugaredLogger, fs filesystem.FileSystem) (types.SelectTestsResp, error) {
		return types.SelectTestsResp{}, errors.New("error in selection")
	}

	want, err := utils.GetLoggableCmd(`set -e
export TMPDIR=/test/tmp
echo x
mvn -am -DargLine=-javaagent:/addon/bin/java-agent.jar=/test/tmp/config.ini clean test
echo y`)
	if err != nil {
		t.Fatalf("could not get loggable cmd for %s", want)
	}
	got, err := r.getCmd(ctx)
	assert.Nil(t, err)
	assert.Equal(t, r.runOnlySelectedTests, false) // Since there was an error in execution
	assert.Equal(t, got, want)
}

func TestGetCmd_ErrorIncorrectBuildTool(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	fs := filesystem.NewMockFileSystem(ctrl)

	tmpFilePath := "/test/tmp"
	packages := "p1, p2, p3"

	expDir := fmt.Sprintf(outDir, tmpFilePath)
	expData := `outDir: /test/tmp/ti/callgraph/
logLevel: 0
logConsole: false
writeTo: COVERAGE_JSON
instrPackages: p1, p2, p3`
	fs.EXPECT().MkdirAll(expDir, os.ModePerm).Return(nil).AnyTimes()
	mf := filesystem.NewMockFile(ctrl)
	mf.EXPECT().Write([]byte(expData)).Return(0, nil).AnyTimes()
	fs.EXPECT().Create("/test/tmp/config.ini").Return(mf, nil).AnyTimes()

	diffFiles, _ := json.Marshal([]types.File{})

	r := runTestsTask{
		id:                   "id",
		runOnlySelectedTests: true,
		fs:                   fs,
		preCommand:           "echo x",
		diffFiles:            string(diffFiles),
		args:                 "clean test",
		postCommand:          "echo y",
		buildTool:            "random",
		tmpFilePath:          tmpFilePath,
		packages:             packages,
		log:                  log.Sugar(),
		addonLogger:          log.Sugar(),
	}

	oldSelect := selectTestsFn
	defer func() {
		selectTestsFn = oldSelect
	}()
	selectTestsFn = func(ctx context.Context, f []types.File, id string, log *zap.SugaredLogger, fs filesystem.FileSystem) (types.SelectTestsResp, error) {
		return types.SelectTestsResp{}, nil
	}

	_, err := r.getCmd(ctx)
	assert.NotNil(t, err)
}

func TestNewRunTestsTask(t *testing.T) {
	diff := "diff"
	preCommand := "pre"
	postCommand := "post"
	lang := "java"
	buildTool := "maven"
	args := "args"
	packages := "packages"
	annotations := "annotations"
	runOnlySelectedTests := false

	runTests := &pb.UnitStep_RunTests{RunTests: &pb.RunTestsStep{
		Args:                 args,
		Language:             lang,
		BuildTool:            buildTool,
		RunOnlySelectedTests: runOnlySelectedTests,
		PreTestCommand:       preCommand,
		PostTestCommand:      postCommand,
		DiffFiles:            diff,
		Packages:             packages,
		TestAnnotations:      annotations,
	}}
	step := &pb.UnitStep{
		Id: "id", Step: runTests}

	task := NewRunTestsTask(step, "/tmp", nil, nil, false, nil)
	assert.Equal(t, task.args, args)
	assert.Equal(t, task.language, lang)
	assert.Equal(t, task.buildTool, buildTool)
	assert.Equal(t, task.runOnlySelectedTests, runOnlySelectedTests)
	assert.Equal(t, task.preCommand, preCommand)
	assert.Equal(t, task.postCommand, postCommand)
	assert.Equal(t, task.diffFiles, diff)
	assert.Equal(t, task.packages, packages)
	assert.Equal(t, task.annotations, annotations)
}

func TestRun_Success(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	fs := filesystem.NewMockFileSystem(ctrl)

	cmdFactory := mexec.NewMockCmdContextFactory(ctrl)
	cmd := mexec.NewMockCommand(ctrl)

	var buf bytes.Buffer

	t1 := types.RunnableTest{Pkg: "pkg1", Class: "cls1", Method: "m1"}
	t2 := types.RunnableTest{Pkg: "pkg2", Class: "cls2", Method: "m2"}

	tmpFilePath := "/test/tmp"
	packages := "p1, p2, p3"

	expDir := fmt.Sprintf(outDir, tmpFilePath)
	expData := `outDir: /test/tmp/ti/callgraph/
logLevel: 0
logConsole: false
writeTo: COVERAGE_JSON
instrPackages: p1, p2, p3`
	fs.EXPECT().MkdirAll(expDir, os.ModePerm).Return(nil).AnyTimes()
	mf := filesystem.NewMockFile(ctrl)
	mf.EXPECT().Write([]byte(expData)).Return(0, nil).AnyTimes()
	fs.EXPECT().Create("/test/tmp/config.ini").Return(mf, nil).AnyTimes()

	diffFiles, _ := json.Marshal([]types.File{})

	cmdFactory.EXPECT().CmdContextWithSleep(gomock.Any(), cmdExitWaitTime, "sh", gomock.Any(), gomock.Any()).Return(cmd)
	cmd.EXPECT().WithStdout(&buf).Return(cmd)
	cmd.EXPECT().WithStderr(&buf).Return(cmd)
	cmd.EXPECT().WithEnvVarsMap(gomock.Any()).Return(cmd)

	oldRunCmd := runCmdFn
	defer func() {
		runCmdFn = oldRunCmd
	}()
	runCmdFn = func(ctx context.Context, cmd exec.Command, stepID string, commands []string, retryCount int32, startTime time.Time,
		logMetrics bool, addonLogger *zap.SugaredLogger) error {
		return nil
	}

	r := runTestsTask{
		id:                   "id",
		runOnlySelectedTests: true,
		fs:                   fs,
		preCommand:           "echo x",
		diffFiles:            string(diffFiles),
		args:                 "clean test",
		postCommand:          "echo y",
		buildTool:            "maven",
		tmpFilePath:          tmpFilePath,
		logMetrics:           false,
		packages:             packages,
		procWriter:           &buf,
		numRetries:           1,
		cmdContextFactory:    cmdFactory,
		log:                  log.Sugar(),
		addonLogger:          log.Sugar(),
	}

	// Mock test selection
	oldSelect := selectTestsFn
	defer func() {
		selectTestsFn = oldSelect
	}()
	selectTestsFn = func(ctx context.Context, f []types.File, id string, log *zap.SugaredLogger, fs filesystem.FileSystem) (types.SelectTestsResp, error) {
		return types.SelectTestsResp{
			SelectAll: false,
			Tests:     []types.RunnableTest{t1, t2}}, nil
	}
	called := 0

	// Mock collectCg
	oldCollectCg := collectCgFn
	defer func() {
		collectCgFn = oldCollectCg
	}()
	collectCgFn = func(ctx context.Context, stepID, collectcgDir string, log *zap.SugaredLogger) error {
		called += 1
		return nil
	}

	// Mock test reports
	oldReports := collectTestReportsFn
	defer func() {
		collectTestReportsFn = oldReports
	}()
	collectTestReportsFn = func(ctx context.Context, reports []*pb.Report, stepID string, log *zap.SugaredLogger) error {
		called += 1
		return nil
	}

	_, err := r.Run(ctx)
	assert.Nil(t, err)
	assert.Equal(t, called, 2) // Make sure both CG collection and report collection are called
}

func TestRun_Execution_Failure(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	fs := filesystem.NewMockFileSystem(ctrl)

	cmdFactory := mexec.NewMockCmdContextFactory(ctrl)
	cmd := mexec.NewMockCommand(ctrl)

	var buf bytes.Buffer

	t1 := types.RunnableTest{Pkg: "pkg1", Class: "cls1", Method: "m1"}
	t2 := types.RunnableTest{Pkg: "pkg2", Class: "cls2", Method: "m2"}

	tmpFilePath := "/test/tmp"
	packages := "p1, p2, p3"

	expDir := fmt.Sprintf(outDir, tmpFilePath)
	expData := `outDir: /test/tmp/ti/callgraph/
logLevel: 0
logConsole: false
writeTo: COVERAGE_JSON
instrPackages: p1, p2, p3`
	fs.EXPECT().MkdirAll(expDir, os.ModePerm).Return(nil)
	mf := filesystem.NewMockFile(ctrl)
	mf.EXPECT().Write([]byte(expData)).Return(0, nil)
	fs.EXPECT().Create("/test/tmp/config.ini").Return(mf, nil)

	diffFiles, _ := json.Marshal([]types.File{})

	cmdFactory.EXPECT().CmdContextWithSleep(gomock.Any(), cmdExitWaitTime, "sh", gomock.Any(), gomock.Any()).Return(cmd)
	cmd.EXPECT().WithStdout(&buf).Return(cmd)
	cmd.EXPECT().WithStderr(&buf).Return(cmd)
	cmd.EXPECT().WithEnvVarsMap(gomock.Any()).Return(cmd)
	expErr := errors.New("could not run command")
	oldRunCmd := runCmdFn
	defer func() {
		runCmdFn = oldRunCmd
	}()
	runCmdFn = func(ctx context.Context, cmd exec.Command, stepID string, commands []string, retryCount int32, startTime time.Time,
		logMetrics bool, addonLogger *zap.SugaredLogger) error {
		return expErr
	}

	r := runTestsTask{
		id:                   "id",
		runOnlySelectedTests: true,
		fs:                   fs,
		preCommand:           "echo x",
		diffFiles:            string(diffFiles),
		args:                 "clean test",
		postCommand:          "echo y",
		buildTool:            "maven",
		tmpFilePath:          tmpFilePath,
		logMetrics:           false,
		packages:             packages,
		procWriter:           &buf,
		numRetries:           1,
		cmdContextFactory:    cmdFactory,
		log:                  log.Sugar(),
		addonLogger:          log.Sugar(),
	}

	called := 0

	// Mock test selection
	oldSelect := selectTestsFn
	defer func() {
		selectTestsFn = oldSelect
	}()
	selectTestsFn = func(ctx context.Context, f []types.File, id string, log *zap.SugaredLogger, fs filesystem.FileSystem) (types.SelectTestsResp, error) {
		return types.SelectTestsResp{
			SelectAll: false,
			Tests:     []types.RunnableTest{t1, t2}}, nil
	}

	// Mock collectCg
	oldCollectCg := collectCgFn
	defer func() {
		collectCgFn = oldCollectCg
	}()
	collectCgFn = func(ctx context.Context, stepID, collectcgDir string, log *zap.SugaredLogger) error {
		called += 1
		return nil
	}

	// Mock test reports
	oldReports := collectTestReportsFn
	defer func() {
		collectTestReportsFn = oldReports
	}()
	collectTestReportsFn = func(ctx context.Context, reports []*pb.Report, stepID string, log *zap.SugaredLogger) error {
		called += 1
		return nil
	}

	_, err := r.Run(ctx)
	assert.Equal(t, err, expErr)
	assert.Equal(t, called, 2) // makes ure both functions are called even on failure
}

func TestRun_Execution_Cg_Failure(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	fs := filesystem.NewMockFileSystem(ctrl)

	cmdFactory := mexec.NewMockCmdContextFactory(ctrl)
	cmd := mexec.NewMockCommand(ctrl)

	var buf bytes.Buffer

	t1 := types.RunnableTest{Pkg: "pkg1", Class: "cls1", Method: "m1"}
	t2 := types.RunnableTest{Pkg: "pkg2", Class: "cls2", Method: "m2"}

	tmpFilePath := "/test/tmp"
	packages := "p1, p2, p3"

	expDir := fmt.Sprintf(outDir, tmpFilePath)
	expData := `outDir: /test/tmp/ti/callgraph/
logLevel: 0
logConsole: false
writeTo: COVERAGE_JSON
instrPackages: p1, p2, p3`
	fs.EXPECT().MkdirAll(expDir, os.ModePerm).Return(nil)
	mf := filesystem.NewMockFile(ctrl)
	mf.EXPECT().Write([]byte(expData)).Return(0, nil)
	fs.EXPECT().Create("/test/tmp/config.ini").Return(mf, nil)

	diffFiles, _ := json.Marshal([]types.File{})

	cmdFactory.EXPECT().CmdContextWithSleep(gomock.Any(), cmdExitWaitTime, "sh", gomock.Any(), gomock.Any()).Return(cmd)
	cmd.EXPECT().WithStdout(&buf).Return(cmd)
	cmd.EXPECT().WithStderr(&buf).Return(cmd)
	cmd.EXPECT().WithEnvVarsMap(gomock.Any()).Return(cmd)

	oldRunCmd := runCmdFn
	defer func() {
		runCmdFn = oldRunCmd
	}()
	runCmdFn = func(ctx context.Context, cmd exec.Command, stepID string, commands []string, retryCount int32, startTime time.Time,
		logMetrics bool, addonLogger *zap.SugaredLogger) error {
		return nil
	}

	r := runTestsTask{
		id:                   "id",
		runOnlySelectedTests: true,
		fs:                   fs,
		preCommand:           "echo x",
		diffFiles:            string(diffFiles),
		args:                 "clean test",
		postCommand:          "echo y",
		buildTool:            "maven",
		tmpFilePath:          tmpFilePath,
		logMetrics:           false,
		packages:             packages,
		procWriter:           &buf,
		numRetries:           1,
		cmdContextFactory:    cmdFactory,
		log:                  log.Sugar(),
		addonLogger:          log.Sugar(),
	}

	// Mock test selection
	oldSelect := selectTestsFn
	defer func() {
		selectTestsFn = oldSelect
	}()
	selectTestsFn = func(ctx context.Context, f []types.File, id string, log *zap.SugaredLogger, fs filesystem.FileSystem) (types.SelectTestsResp, error) {
		return types.SelectTestsResp{
			SelectAll: false,
			Tests:     []types.RunnableTest{t1, t2}}, nil
	}

	// Mock collectCg
	oldCollectCg := collectCgFn
	defer func() {
		collectCgFn = oldCollectCg
	}()
	errCg := errors.New("could not collect CG")
	collectCgFn = func(ctx context.Context, stepID, collectcgDir string, log *zap.SugaredLogger) error {
		return errCg
	}

	// Mock test reports
	oldReports := collectTestReportsFn
	defer func() {
		collectTestReportsFn = oldReports
	}()
	collectTestReportsFn = func(ctx context.Context, reports []*pb.Report, stepID string, log *zap.SugaredLogger) error {
		return nil
	}

	_, err := r.Run(ctx)
	assert.Equal(t, err, errCg)
}

func TestRun_Execution_Reports_Failure(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	fs := filesystem.NewMockFileSystem(ctrl)

	cmdFactory := mexec.NewMockCmdContextFactory(ctrl)
	cmd := mexec.NewMockCommand(ctrl)

	var buf bytes.Buffer

	t1 := types.RunnableTest{Pkg: "pkg1", Class: "cls1", Method: "m1"}
	t2 := types.RunnableTest{Pkg: "pkg2", Class: "cls2", Method: "m2"}

	tmpFilePath := "/test/tmp"
	packages := "p1, p2, p3"

	expDir := fmt.Sprintf(outDir, tmpFilePath)
	expData := `outDir: /test/tmp/ti/callgraph/
logLevel: 0
logConsole: false
writeTo: COVERAGE_JSON
instrPackages: p1, p2, p3`
	fs.EXPECT().MkdirAll(expDir, os.ModePerm).Return(nil)
	mf := filesystem.NewMockFile(ctrl)
	mf.EXPECT().Write([]byte(expData)).Return(0, nil)
	fs.EXPECT().Create("/test/tmp/config.ini").Return(mf, nil)

	diffFiles, _ := json.Marshal([]types.File{})

	cmdFactory.EXPECT().CmdContextWithSleep(gomock.Any(), cmdExitWaitTime, "sh", gomock.Any(), gomock.Any()).Return(cmd)
	cmd.EXPECT().WithStdout(&buf).Return(cmd)
	cmd.EXPECT().WithStderr(&buf).Return(cmd)
	cmd.EXPECT().WithEnvVarsMap(gomock.Any()).Return(cmd)

	oldRunCmd := runCmdFn
	defer func() {
		runCmdFn = oldRunCmd
	}()
	runCmdFn = func(ctx context.Context, cmd exec.Command, stepID string, commands []string, retryCount int32, startTime time.Time,
		logMetrics bool, addonLogger *zap.SugaredLogger) error {
		return nil
	}

	r := runTestsTask{
		id:                   "id",
		runOnlySelectedTests: true,
		fs:                   fs,
		preCommand:           "echo x",
		diffFiles:            string(diffFiles),
		args:                 "clean test",
		postCommand:          "echo y",
		buildTool:            "maven",
		tmpFilePath:          tmpFilePath,
		logMetrics:           false,
		packages:             packages,
		procWriter:           &buf,
		numRetries:           1,
		cmdContextFactory:    cmdFactory,
		log:                  log.Sugar(),
		addonLogger:          log.Sugar(),
	}

	// Mock test selection
	oldSelect := selectTestsFn
	defer func() {
		selectTestsFn = oldSelect
	}()
	selectTestsFn = func(ctx context.Context, f []types.File, id string, log *zap.SugaredLogger, fs filesystem.FileSystem) (types.SelectTestsResp, error) {
		return types.SelectTestsResp{
			SelectAll: false,
			Tests:     []types.RunnableTest{t1, t2}}, nil
	}

	// Mock collectCg
	oldCollectCg := collectCgFn
	defer func() {
		collectCgFn = oldCollectCg
	}()
	collectCgFn = func(ctx context.Context, stepID, collectcgDir string, log *zap.SugaredLogger) error {
		return nil
	}

	// Mock test reports
	errReport := errors.New("could not collect reports")
	oldReports := collectTestReportsFn
	defer func() {
		collectTestReportsFn = oldReports
	}()
	collectTestReportsFn = func(ctx context.Context, reports []*pb.Report, stepID string, log *zap.SugaredLogger) error {
		return errReport
	}

	_, err := r.Run(ctx)
	assert.Equal(t, err, errReport)
}
