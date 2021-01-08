package tasks

import (
	"bytes"
	"context"
	"errors"
	"fmt"
	"os"
	"os/exec"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	"go.uber.org/zap"

	mexec "github.com/wings-software/portal/commons/go/lib/exec"
	"github.com/wings-software/portal/commons/go/lib/filesystem"
	"github.com/wings-software/portal/commons/go/lib/logs"
	"github.com/wings-software/portal/product/ci/addon/testreports"
	mreports "github.com/wings-software/portal/product/ci/addon/testreports/mocks"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	ticlient "github.com/wings-software/portal/product/ci/ti-service/client"
	mclient "github.com/wings-software/portal/product/ci/ti-service/client/mocks"
	"github.com/wings-software/portal/product/ci/ti-service/types"
)

func TestExecuteSuccess(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	numRetries := int32(1)
	var buf bytes.Buffer

	fs := filesystem.NewMockFileSystem(ctrl)
	cmdFactory := mexec.NewMockCmdContextFactory(ctrl)
	cmd := mexec.NewMockCommand(ctrl)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	e := runTask{
		id:                "step1",
		command:           "ls",
		timeoutSecs:       5,
		numRetries:        numRetries,
		tmpFilePath:       "/tmp",
		log:               log.Sugar(),
		fs:                fs,
		cmdContextFactory: cmdFactory,
		procWriter:        &buf,
	}

	cmdFactory.EXPECT().CmdContextWithSleep(gomock.Any(), cmdExitWaitTime, "sh", gomock.Any(), gomock.Any()).Return(cmd)
	cmd.EXPECT().WithStdout(&buf).Return(cmd)
	cmd.EXPECT().WithStderr(&buf).Return(cmd)
	cmd.EXPECT().WithEnvVarsMap(nil).Return(cmd)
	cmd.EXPECT().Run().Return(nil)

	o, retries, err := e.Run(ctx)
	assert.Nil(t, err)
	assert.Equal(t, len(o), 0)
	assert.Equal(t, retries, numRetries)
}

func TestExecuteSuccess_WithReports(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	stepID := "step1"
	paths := []string{"path1", "path2"}
	report := &pb.Report{
		Type:  pb.Report_JUNIT,
		Paths: paths,
	}
	reports := []*pb.Report{report}

	numRetries := int32(1)
	var buf bytes.Buffer

	fs := filesystem.NewMockFileSystem(ctrl)
	cmdFactory := mexec.NewMockCmdContextFactory(ctrl)
	cmd := mexec.NewMockCommand(ctrl)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	e := runTask{
		id:                stepID,
		command:           "ls",
		timeoutSecs:       5,
		numRetries:        numRetries,
		tmpFilePath:       "/tmp",
		log:               log.Sugar(),
		fs:                fs,
		reports:           reports,
		cmdContextFactory: cmdFactory,
		procWriter:        &buf,
	}

	cmdFactory.EXPECT().CmdContextWithSleep(gomock.Any(), cmdExitWaitTime, "sh", gomock.Any(), gomock.Any()).Return(cmd)
	cmd.EXPECT().WithStdout(&buf).Return(cmd)
	cmd.EXPECT().WithStderr(&buf).Return(cmd)
	cmd.EXPECT().WithEnvVarsMap(nil).Return(cmd)
	cmd.EXPECT().Run().Return(nil)

	tc := &types.TestCase{
		Name:      "test",
		ClassName: "class",
	}

	// Mock calls to get various identifiers
	org := "org"
	project := "project"
	pipeline := "pipeline"
	build := "build"
	stage := "stage"

	oldOrg := getOrgId
	oldProject := getProjectId
	oldPipeline := getPipelineId
	oldBuild := getBuildId
	oldStage := getStageId
	defer func() {
		getOrgId = oldOrg
		getProjectId = oldProject
		getPipelineId = oldPipeline
		getBuildId = oldBuild
		getStageId = oldStage
	}()
	getOrgId = func() (string, error) { return org, nil }
	getProjectId = func() (string, error) { return project, nil }
	getPipelineId = func() (string, error) { return pipeline, nil }
	getBuildId = func() (string, error) { return build, nil }
	getStageId = func() (string, error) { return stage, nil }

	mockReporter := mreports.NewMockTestReporter(ctrl)
	mockTIClient := mclient.NewMockClient(ctrl)

	// Mock test reporter
	oldJunit := newJunit
	defer func() { newJunit = oldJunit }()
	newJunit = func(paths []string, log *zap.SugaredLogger) testreports.TestReporter {
		return mockReporter
	}
	tests := make(chan *types.TestCase, 1)
	tests <- tc
	close(tests)
	mockReporter.EXPECT().GetTests(ctx).Return(tests, nil)

	// Mock TI client
	oldTiClient := getTIClient
	defer func() { getTIClient = oldTiClient }()
	getTIClient = func() (ticlient.Client, error) {
		return mockTIClient, nil
	}
	var expectedTests []*types.TestCase
	expectedTests = append(expectedTests, tc)
	mockTIClient.EXPECT().Write(ctx, org, project, pipeline, build, stage, stepID, "junit", expectedTests).Return(nil)

	o, retries, err := e.Run(ctx)
	assert.Nil(t, err)
	assert.Equal(t, len(o), 0)
	assert.Equal(t, retries, numRetries)
}

func TestExecuteFailure_WithReports(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	stepID := "step1"
	paths := []string{"path1", "path2"}
	report := &pb.Report{
		Type:  pb.Report_JUNIT,
		Paths: paths,
	}
	reports := []*pb.Report{report}

	numRetries := int32(1)
	var buf bytes.Buffer

	fs := filesystem.NewMockFileSystem(ctrl)
	cmdFactory := mexec.NewMockCmdContextFactory(ctrl)
	cmd := mexec.NewMockCommand(ctrl)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	e := runTask{
		id:                stepID,
		command:           "ls",
		timeoutSecs:       5,
		numRetries:        numRetries,
		tmpFilePath:       "/tmp",
		log:               log.Sugar(),
		fs:                fs,
		reports:           reports,
		cmdContextFactory: cmdFactory,
		procWriter:        &buf,
	}

	cmdFactory.EXPECT().CmdContextWithSleep(gomock.Any(), cmdExitWaitTime, "sh", gomock.Any(), gomock.Any()).Return(cmd)
	cmd.EXPECT().WithStdout(&buf).Return(cmd)
	cmd.EXPECT().WithStderr(&buf).Return(cmd)
	cmd.EXPECT().WithEnvVarsMap(nil).Return(cmd)
	cmd.EXPECT().Run().Return(nil)

	tc := &types.TestCase{
		Name:      "test",
		ClassName: "class",
	}

	// Mock calls to get various identifiers
	org := "org"
	project := "project"
	pipeline := "pipeline"
	build := "build"
	stage := "stage"

	oldOrg := getOrgId
	oldProject := getProjectId
	oldPipeline := getPipelineId
	oldBuild := getBuildId
	oldStage := getStageId
	defer func() {
		getOrgId = oldOrg
		getProjectId = oldProject
		getPipelineId = oldPipeline
		getBuildId = oldBuild
		getStageId = oldStage
	}()
	getOrgId = func() (string, error) { return org, nil }
	getProjectId = func() (string, error) { return project, nil }
	getPipelineId = func() (string, error) { return pipeline, nil }
	getBuildId = func() (string, error) { return build, nil }
	getStageId = func() (string, error) { return stage, nil }

	mockReporter := mreports.NewMockTestReporter(ctrl)
	mockTIClient := mclient.NewMockClient(ctrl)

	// Mock test reporter
	oldJunit := newJunit
	defer func() { newJunit = oldJunit }()
	newJunit = func(paths []string, log *zap.SugaredLogger) testreports.TestReporter {
		return mockReporter
	}
	tests := make(chan *types.TestCase, 1)
	tests <- tc
	close(tests)
	mockReporter.EXPECT().GetTests(ctx).Return(tests, nil)

	// Mock TI client
	oldTiClient := getTIClient
	defer func() { getTIClient = oldTiClient }()
	getTIClient = func() (ticlient.Client, error) {
		return mockTIClient, nil
	}
	var expectedTests []*types.TestCase
	expectedTests = append(expectedTests, tc)
	mockTIClient.EXPECT().Write(ctx, org, project, pipeline, build, stage, stepID, "junit", expectedTests).Return(errors.New("err"))

	o, retries, err := e.Run(ctx)
	assert.NotNil(t, err)
	assert.Equal(t, len(o), 0)
	assert.Equal(t, retries, numRetries)
}

func TestExecuteNonZeroStatus(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	numRetries := int32(1)
	var buf bytes.Buffer

	fs := filesystem.NewMockFileSystem(ctrl)
	cmdFactory := mexec.NewMockCmdContextFactory(ctrl)
	cmd := mexec.NewMockCommand(ctrl)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	e := runTask{
		id:                "step1",
		command:           "ls",
		timeoutSecs:       5,
		numRetries:        numRetries,
		tmpFilePath:       "/tmp",
		log:               log.Sugar(),
		fs:                fs,
		cmdContextFactory: cmdFactory,
		procWriter:        &buf,
	}

	cmdFactory.EXPECT().CmdContextWithSleep(gomock.Any(), cmdExitWaitTime, "sh", gomock.Any(), gomock.Any()).Return(cmd)
	cmd.EXPECT().WithStdout(&buf).Return(cmd)
	cmd.EXPECT().WithStderr(&buf).Return(cmd)
	cmd.EXPECT().WithEnvVarsMap(nil).Return(cmd)
	cmd.EXPECT().Run().Return(&exec.ExitError{})

	o, retries, err := e.Run(ctx)
	assert.NotNil(t, err)
	if _, ok := err.(*exec.ExitError); !ok {
		t.Fatalf("Expected err of type exec.ExitError")
	}
	assert.Equal(t, len(o), 0)
	assert.Equal(t, retries, numRetries)
}

func TestExecuteSuccessWithOutput(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	var buf bytes.Buffer
	numRetries := int32(1)
	filePath := "/tmp/idoutput.txt"
	envVar := "abc"
	envVal := "xyz"

	fs := filesystem.NewMockFileSystem(ctrl)
	cmdFactory := mexec.NewMockCmdContextFactory(ctrl)
	cmd := mexec.NewMockCommand(ctrl)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	f, err := os.Create(filePath)
	if err != nil {
		panic(err)
	}
	f.WriteString("abc xyz\n")
	f.WriteString("abc1")
	f.Close()

	f1, err := os.Open(filePath)
	e := runTask{
		id:                "step1",
		command:           "ls",
		timeoutSecs:       5,
		numRetries:        numRetries,
		envVarOutputs:     []string{"abc", "abc1"},
		tmpFilePath:       "/tmp",
		log:               log.Sugar(),
		fs:                fs,
		cmdContextFactory: cmdFactory,
		procWriter:        &buf,
	}

	cmdFactory.EXPECT().CmdContextWithSleep(gomock.Any(), cmdExitWaitTime, "sh", gomock.Any(), gomock.Any()).Return(cmd)
	cmd.EXPECT().WithStdout(&buf).Return(cmd)
	cmd.EXPECT().WithStderr(&buf).Return(cmd)
	cmd.EXPECT().WithEnvVarsMap(nil).Return(cmd)
	cmd.EXPECT().Run().Return(nil)
	fs.EXPECT().Open(gomock.Any()).Return(f1, nil)

	o, retries, err := e.Run(ctx)
	assert.Nil(t, err)
	assert.Equal(t, o[envVar], envVal)
	assert.Equal(t, retries, numRetries)
}

func TestExecuteErrorWithOutput(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	var buf bytes.Buffer
	numRetries := int32(1)
	filePath := "/tmp/idoutput.txt"

	fs := filesystem.NewMockFileSystem(ctrl)
	cmdFactory := mexec.NewMockCmdContextFactory(ctrl)
	cmd := mexec.NewMockCommand(ctrl)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	f, err := os.Create(filePath)
	if err != nil {
		panic(err)
	}
	f.WriteString("abc xyz\n")
	f.WriteString("abc1")
	f.Close()

	e := runTask{
		id:                "step1",
		command:           "ls",
		timeoutSecs:       5,
		numRetries:        numRetries,
		envVarOutputs:     []string{"abc", "abc1"},
		tmpFilePath:       "/tmp",
		log:               log.Sugar(),
		fs:                fs,
		cmdContextFactory: cmdFactory,
		procWriter:        &buf,
	}

	cmdFactory.EXPECT().CmdContextWithSleep(gomock.Any(), cmdExitWaitTime, "sh", gomock.Any(), gomock.Any()).Return(cmd)
	cmd.EXPECT().WithStdout(&buf).Return(cmd)
	cmd.EXPECT().WithStderr(&buf).Return(cmd)
	cmd.EXPECT().WithEnvVarsMap(nil).Return(cmd)
	cmd.EXPECT().Run().Return(nil)
	fs.EXPECT().Open(gomock.Any()).Return(nil, fmt.Errorf("Error while opening file"))

	o, retries, err := e.Run(ctx)
	assert.NotNil(t, err)
	assert.Equal(t, len(o), 0)
	assert.Equal(t, retries, numRetries)
}

func TestRunTaskCreate(t *testing.T) {
	tmpPath := "/tmp/"
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	step := &pb.UnitStep{
		Id: "test",
		Step: &pb.UnitStep_Run{
			Run: &pb.RunStep{
				Command: "cd . ; ls",
			},
		},
	}

	var buf bytes.Buffer
	executor := NewRunTask(step, tmpPath, log.Sugar(), &buf)
	assert.NotNil(t, executor)
}
