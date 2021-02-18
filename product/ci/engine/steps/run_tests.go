package steps

import (
	"bufio"
	"context"
	"fmt"
	"os"
	"time"

	"github.com/pkg/errors"
	"github.com/wings-software/portal/commons/go/lib/archive"
	"github.com/wings-software/portal/commons/go/lib/filesystem"
	"github.com/wings-software/portal/commons/go/lib/utils"
	addonpb "github.com/wings-software/portal/product/ci/addon/proto"
	"github.com/wings-software/portal/product/ci/common/external"
	"github.com/wings-software/portal/product/ci/engine/http"
	"github.com/wings-software/portal/product/ci/engine/output"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

const (
	diffPath = "/step-exec/.harness/vcs/diff.txt"
	srcDir   = "/step-exec/.harness/test-intelligence/callgraph"             //directory where callgraph will be created
	archPath = "/step-exec/.harness/test-intelligence/archive/callgraph.tar" //directory where tar file of callgraph will be created
)

var (
	remoteTiClient = external.GetTiHTTPClient
	getOrgId       = external.GetOrgId
	getProjectId   = external.GetProjectId
	getPipelineId  = external.GetPipelineId
	getBuildId     = external.GetBuildId
	getStageId     = external.GetStageId
)

// RunTestsStep represents interface to execute a run step
type runTestsStep struct {
	id               string
	name             string
	tempPath         string   // File path to store generated temporary files
	lang             string   // language of codebase
	buildTool        string   // buildTool used for codebase
	goals            string   // custom flags to
	executionCommand string   // final command which will be executed by addon
	envVarOutputs    []string // Environment variables to be exported to the step
	cntrPort         uint32
	tiSrvEP          string // base url where the tiServer would be running
	acctID           string
	token            string
	archiver         archive.Archiver
	stepCtx          *pb.StepContext
	so               output.StageOutput
	log              *zap.SugaredLogger
}

// RunTestsStep represents interface to execute a run step
type RunTestsStep interface {
	Run(ctx context.Context) (*output.StepOutput, int32, error)
}

// NewRunTestsStep creates a run step executor
func NewRunTestsStep(step *pb.UnitStep, tempPath string, so output.StageOutput,
	log *zap.SugaredLogger) RunTestsStep {
	r := step.GetRunTests()
	return &runTestsStep{
		id:        step.GetId(),
		name:      step.GetDisplayName(),
		goals:     r.GetGoals(),
		buildTool: r.GetBuildTool(),
		lang:      r.GetLanguage(),
		cntrPort:  r.GetContainerPort(),
		stepCtx:   r.GetContext(),
		tempPath:  tempPath,
		tiSrvEP:   os.Getenv("TI_SERVER_URL"),
		acctID:    os.Getenv("HARNESS_ACCOUNT_ID"),
		token:     os.Getenv("AUTH_TOKEN"),
		so:        so,
		log:       log,
	}
}

// Run execute tests with provided goals with retries and timeout handling
func (e *runTestsStep) Run(ctx context.Context) (*output.StepOutput, int32, error) {
	if err := e.validate(); err != nil {
		e.log.Errorw("failed to validate runTestsStep step", "step_id", e.id, zap.Error(err))
		return nil, int32(1), err
	}

	if err := e.resolveJEXL(ctx); err != nil {
		return nil, int32(1), err
	}

	changedFiles, err := e.readVCSDiffFromFile()

	if err != nil {
		e.log.Errorw("failed to read vcs diff in runTests step", "step_id", e.id, zap.Error(err))
		return nil, int32(1), err
	}

	org, err := getOrgId()
	if err != nil {
		return nil, int32(1), err
	}
	project, err := getProjectId()
	if err != nil {
		return nil, int32(1), err
	}
	pipeline, err := getPipelineId()
	if err != nil {
		return nil, int32(1), err
	}
	build, err := getBuildId()
	if err != nil {
		return nil, int32(1), err
	}
	stage, err := getStageId()
	if err != nil {
		return nil, int32(1), err
	}

	// client := http.NewHTTPClient(e.tiSrvEP, e.acctID, e.token, e.log)
	tc, err := remoteTiClient()
	if err != nil {
		e.log.Errorw("could not create a client to the TI service", zap.Error(err))
		return nil, int32(1), err
	}
	// testsToExecute, err := client.GetTestsToRun(changedFiles)
	_, err = tc.GetTests(org, project, pipeline, build, stage, e.id, []string{changedFiles})

	// e.log.Errorw(fmt.Sprintf("%s", testsToE), zap.Error(err))

	testsToExecute := "dummy"

	runAll := false
	if err != nil {
		runAll = true
	}

	executionCommand, err := e.getRunTestsCommand(testsToExecute, runAll)
	if err != nil {
		return nil, int32(1), err
	}

	return e.execute(ctx, executionCommand)
}

func (e *runTestsStep) getRunTestsCommand(testsToExecute string, runAll bool) (string, error) {
	e.log.Infow(
		"running tests with intelligence",
		"testsToExecute", testsToExecute,
	)

	testsFlag := ""

	if runAll == false {
		testsFlag = fmt.Sprintf("-Dtest=%s", testsToExecute)
	}

	switch e.buildTool {
	case "maven":
		// Eg. of goals: "-T 2C -DskipTests"
		// command will finally be like:
		// mvn -T 2C -DskipTests -Dtest=TestSquare,TestCirle test
		return fmt.Sprintf("mvn test %s %s -am", e.goals, testsFlag), nil
	default:
		e.log.Errorw(fmt.Sprintf("only maven build tool is supported, build tool is: %s", e.buildTool), "step_id", e.id)
		return "", fmt.Errorf("build tool %s is not suported", e.buildTool)
	}
}

func (e *runTestsStep) readVCSDiffFromFile() (string, error) {
	file, err := os.Open(diffPath)

	defer file.Close()

	if err != nil {
		e.log.Errorw(fmt.Sprintf("could not open %s file", diffPath), "step_id", e.id, zap.Error(err))
		return "", err
	}

	scanner := bufio.NewScanner(file)
	scanner.Split(bufio.ScanLines)

	var txtlines string
	for scanner.Scan() {
		txtlines += scanner.Text()
	}
	return txtlines, nil
}

func (e *runTestsStep) validate() error {
	if e.cntrPort == 0 {
		return fmt.Errorf("runTestsStep container port is not set")
	}

	if e.lang != "java" {
		e.log.Errorw(fmt.Sprintf("Only java is supported as the codebase language. Received language is: %s", e.lang), "step_id", e.id)
		return fmt.Errorf("unsupported language in test intelligence step")
	}

	return nil
}

// resolveJEXL resolves JEXL expressions present in run step input
func (e *runTestsStep) resolveJEXL(ctx context.Context) error {
	// JEXL expressions are only present in goals
	s := e.goals
	resolvedExprs, err := evaluateJEXL(ctx, e.id, []string{s}, e.so, false, e.log)

	if err != nil {
		return err
	}

	e.goals = resolvedExprs[s]
	return nil
}

func (e *runTestsStep) execute(ctx context.Context, executionCommand string) (*output.StepOutput, int32, error) {
	st := time.Now()

	addonClient, err := newAddonClient(uint(e.cntrPort), e.log)

	if err != nil {
		e.log.Errorw("Unable to create CI addon client", "step_id", e.id, zap.Error(err))
		return nil, int32(1), errors.Wrap(err, "Could not create CI Addon client")
	}
	defer addonClient.CloseConn()

	// temp solution as mvn is not present by default in image
	// discuss with Shiv as well if we need this option in the runTests command.
	executionCommand = "source /etc/profile.d/maven.sh; " + executionCommand
	e.log.Infow(
		fmt.Sprintf("Final command created by test intelligence is: %s", executionCommand), "step_id", e.id)

	c := addonClient.Client()
	arg := e.getExecuteStepArg()
	ret, err := c.ExecuteStep(ctx, arg)
	if err != nil {
		e.log.Errorw("Execute run step RPC failed", "step_id", e.id, "elapsed_time_ms", utils.TimeSince(st), zap.Error(err))
		return nil, int32(1), err
	}

	stepOutput := &output.StepOutput{}
	stepOutput.Output.Variables = ret.GetOutput()

	err = e.uploadCallGraph()

	if err != nil {
		return nil, 0, errors.Wrap(err, fmt.Sprintf("Failed while uploading callgraph to the xxxxxxxx %s", diffPath))
	}

	e.log.Infow("Successfully executed ti step", "elapsed_time_ms", utils.TimeSince(st))
	return stepOutput, ret.GetNumRetries(), nil

}

func (e *runTestsStep) getExecuteStepArg() *addonpb.ExecuteStepRequest {
	return &addonpb.ExecuteStepRequest{
		Step: &pb.UnitStep{
			Id:          e.id,
			DisplayName: e.name,
			Step: &pb.UnitStep_Run{
				Run: &pb.RunStep{
					Command:       e.executionCommand,
					Context:       e.stepCtx,
					EnvVarOutputs: e.envVarOutputs,
				},
			},
		},
		TmpFilePath: e.tempPath,
	}
}

func (e *runTestsStep) uploadCallGraph() error {
	fs := filesystem.NewOSFileSystem(e.log)
	e.archiver = archive.NewArchiver(archiveFormat, fs, e.log)

	err := e.archiveFiles()

	if err != nil {
		return errors.Wrap(err, "Step failed while archiving callgraph")
	}
	client := http.NewHTTPClient(e.tiSrvEP, e.acctID, e.token, e.log)
	err = client.UploadCallGraph(archPath)
	if err != nil {
		return errors.Wrap(err, "Step failed while uploading callgraph")
	}
	return nil
}

// Archive the files
func (e *runTestsStep) archiveFiles() error {
	e.log.Infow(
		"Archiving callgraphs for uploading",
		"srcDir", srcDir,
		"archPath", archPath,
	)
	err := e.archiver.Archive([]string{srcDir}, archPath)
	if err != nil {
		return errors.Wrap(err, fmt.Sprintf("Failed to archive files: %s", srcDir))
	}
	return nil
}
