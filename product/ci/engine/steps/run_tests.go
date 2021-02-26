package steps

import (
	"context"
	"fmt"
	"time"

	"github.com/pkg/errors"
	"github.com/wings-software/portal/commons/go/lib/utils"
	addonpb "github.com/wings-software/portal/product/ci/addon/proto"
	"github.com/wings-software/portal/product/ci/common/external"
	"github.com/wings-software/portal/product/ci/engine/output"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

var (
	remoteTiClient  = external.GetTiHTTPClient
	getOrgId        = external.GetOrgId
	getProjectId    = external.GetProjectId
	getPipelineId   = external.GetPipelineId
	getBuildId      = external.GetBuildId
	getStageId      = external.GetStageId
	getRepo         = external.GetRepo
	getSha          = external.GetSha
	getSourceBranch = external.GetSourceBranch
	getGitBinPath   = external.GetGitBinPath
	getWrkspcPath   = external.GetWrkspcPath
	getChFiles      = external.GetChangedFiles
)

// RunTestsStep represents interface to execute a run step
type runTestsStep struct {
	id                   string             // Id of the step
	name                 string             // Name of the step
	tempPath             string             // File path to store generated temporary files
	lang                 string             // language of codebase
	buildTool            string             // buildTool used for codebase
	args                 string             // custom flags to
	execCommand          string             // final command which will be executed by addon
	envVarOutputs        []string           // Environment variables to be exported to the step
	runOnlySelectedTests bool               // Flag to be used for disabling testIntelligence and running all tests
	packages             string             // Packages ti will generate callgraph for
	annotations          string             // Annotations to identify tests for instrumentation
	cntrPort             uint32             // Container for running ti port
	stepCtx              *pb.StepContext    // Step context
	so                   output.StageOutput // Output variables of the stage
	log                  *zap.SugaredLogger // Logger
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
		id:                   step.GetId(),
		name:                 step.GetDisplayName(),
		args:                 r.GetArgs(),
		buildTool:            r.GetBuildTool(),
		lang:                 r.GetLanguage(),
		cntrPort:             r.GetContainerPort(),
		stepCtx:              r.GetContext(),
		runOnlySelectedTests: r.GetRunOnlySelectedTests(),
		packages:             r.GetPackages(),
		annotations:          r.GetTestAnnotations(),
		tempPath:             tempPath,
		so:                   so,
		log:                  log,
	}
}

// Run execute tests with provided args with retries and timeout handling
func (e *runTestsStep) Run(ctx context.Context) (*output.StepOutput, int32, error) {
	if err := e.validate(); err != nil {
		e.log.Errorw("failed to validate runTestsStep step", "step_id", e.id, zap.Error(err))
		return nil, int32(1), err
	}
	var err error
	if e.args, err = e.resolveJEXL(ctx); err != nil {
		return nil, int32(1), err
	}

	gitPath, err := getGitBinPath()
	if err != nil {
		return nil, int32(1), err
	}
	workspace, err := getWrkspcPath()
	if err != nil {
		return nil, int32(1), err
	}
	chFiles, err := getChFiles(ctx, gitPath, workspace, e.log)
	if err != nil {
		e.log.Errorw("failed to read changed filed in runTests step", "step_id", e.id, zap.Error(err))
		return nil, int32(1), err
	}
	e.log.Infow(fmt.Sprintf("changed files list is: %s", chFiles), "step_id", e.id)
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

	repo, err := getRepo()
	if err != nil {
		return nil, int32(1), err
	}

	sha, err := getSha()
	if err != nil {
		return nil, int32(1), err
	}

	branch, err := getSourceBranch()
	if err != nil {
		return nil, int32(1), err
	}

	tc, err := remoteTiClient()
	if err != nil {
		e.log.Errorw("failed to create ti service client", zap.Error(err))
		return nil, int32(1), err
	}

	tests, err := tc.GetTests(org, project, pipeline, build, stage, e.id, repo, sha, branch, chFiles)

	runSelectedTests := e.runOnlySelectedTests
	if err != nil {
		e.log.Errorw("failed to fetch tests from ti server. Running all tests", zap.Error(err))
		runSelectedTests = false
	}

	var testExecList string
	e.log.Infow(fmt.Sprintf("tests received from ti server are: %s", tests))
	for index, test := range tests {
		// In case we don't get malformed information, we should run all tests.
		if test.Class == "" {
			runSelectedTests = false
			break
		}
		testExecList += test.Class
		if index < (len(tests) - 1) {
			testExecList += ","
		}
	}

	e.execCommand, err = e.getRunTestsCommand(testExecList, runSelectedTests)
	e.log.Infow(fmt.Sprintf("final execCommand is: %s", e.execCommand))
	if err != nil {
		return nil, int32(1), err
	}

	return e.execute(ctx)
}

// getRunTestsCommand makes call to ti client to fetch the tests to be run
func (e *runTestsStep) getRunTestsCommand(testsToExecute string, runSelectedTests bool) (string, error) {

	e.log.Infow(
		"running tests with intelligence",
		"testsToExecute", testsToExecute,
	)

	testsFlag := ""

	if runSelectedTests == true {
		testsFlag = fmt.Sprintf("-Dtest=%s", testsToExecute)
	}

	if len(testsToExecute) == 0 {
		return fmt.Sprintf("echo \"Skipping test run as there are no changes to tests\""), nil
	}

	switch e.buildTool {
	case "maven":
		// Eg. of goals: "-T 2C -DskipTests"
		// command will finally be like:
		// mvn -T 2C -DskipTests -Dtest=TestSquare,TestCirle test
		return fmt.Sprintf("mvn test %s %s -am", e.args, testsFlag), nil
	default:
		e.log.Errorw(fmt.Sprintf("only maven build tool is supported, build tool is: %s", e.buildTool), "step_id", e.id)
		return "", fmt.Errorf("build tool %s is not suported", e.buildTool)
	}
}

// validate the container port and language
func (e *runTestsStep) validate() error {
	if e.cntrPort == 0 {
		return fmt.Errorf("runTestsStep container port is not set")
	}

	if e.lang != "java" {
		e.log.Errorw(fmt.Sprintf("only java is supported as the codebase language. Received language is: %s", e.lang), "step_id", e.id)
		return fmt.Errorf("unsupported language in test intelligence step")
	}

	return nil
}

// resolveJEXL resolves JEXL expressions present in run step input
func (e *runTestsStep) resolveJEXL(ctx context.Context) (string, error) {
	// JEXL expressions are only present in goals
	args := e.args
	resolvedExprs, err := evaluateJEXL(ctx, e.id, []string{args}, e.so, false, e.log)
	if err != nil {
		return "", err
	}

	if val, ok := resolvedExprs[args]; ok {
		return val, nil
	}
	return args, nil
}

// execute step and sent the rpc call to addOn server for running the commands
func (e *runTestsStep) execute(ctx context.Context) (*output.StepOutput, int32, error) {
	st := time.Now()

	addonClient, err := newAddonClient(uint(e.cntrPort), e.log)
	if err != nil {
		e.log.Errorw("Unable to create CI addon client", "step_id", e.id, zap.Error(err))
		return nil, int32(1), errors.Wrap(err, "Could not create CI Addon client")
	}
	defer addonClient.CloseConn()

	c := addonClient.Client()
	arg := e.getExecuteStepArg()
	ret, err := c.ExecuteStep(ctx, arg)
	if err != nil {
		e.log.Errorw("Execute run step RPC failed", "step_id", e.id, "elapsed_time_ms", utils.TimeSince(st), zap.Error(err))
		return nil, int32(1), err
	}

	stepOutput := &output.StepOutput{}
	stepOutput.Output.Variables = ret.GetOutput()

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
					Command:       e.execCommand,
					Context:       e.stepCtx,
					EnvVarOutputs: e.envVarOutputs,
				},
			},
		},
		TmpFilePath: e.tempPath,
	}
}
