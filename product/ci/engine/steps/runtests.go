package steps

import (
	"context"
	"fmt"
	"time"

	grpc_retry "github.com/grpc-ecosystem/go-grpc-middleware/retry"
	"github.com/pkg/errors"
	fs "github.com/wings-software/portal/commons/go/lib/filesystem"
	"github.com/wings-software/portal/commons/go/lib/utils"
	addonpb "github.com/wings-software/portal/product/ci/addon/proto"
	"github.com/wings-software/portal/product/ci/addon/ti"
	"github.com/wings-software/portal/product/ci/common/avro"
	"github.com/wings-software/portal/product/ci/common/external"
	"github.com/wings-software/portal/product/ci/engine/output"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

var (
	getWrkspcPath  = external.GetWrkspcPath
	getChFiles     = external.GetChangedFiles
	remoteTiClient = external.GetTiHTTPClient
	getOrgId       = external.GetOrgId
	getProjectId   = external.GetProjectId
	getPipelineId  = external.GetPipelineId
	getBuildId     = external.GetBuildId
	getStageId     = external.GetStageId
)

const (
	cgDir        = "/Users/amansingh/Desktop/f2.txt"                                 // Discuss with Shiv - How to handle the deduping being done in job [TODO: Aman]
	cgSchemaPath = "callgraph.avsc" // the path of this needs to be decided [TODO: Aman]
)

// RunTestsStep represents interface to execute a runTests step
type runTestsStep struct {
	id            string           // Id of the step
	runTestsInfo  *pb.RunTestsStep // Run tests step information
	step          *pb.UnitStep
	containerPort uint32
	so            output.StageOutput // Output variables of the stage
	log           *zap.SugaredLogger // Logger
}

// RunTestsStep represents interface to execute a run step
type RunTestsStep interface {
	Run(ctx context.Context) (*output.StepOutput, int32, error)
}

// NewRunTestsStep creates a run step executor
func NewRunTestsStep(step *pb.UnitStep, so output.StageOutput, log *zap.SugaredLogger) RunTestsStep {
	return &runTestsStep{
		id:           step.GetId(),
		runTestsInfo: step.GetRunTests(),
		step:         step,
		so:           so,
		log:          log,
	}
}

func (e *runTestsStep) getDiffFiles(ctx context.Context) ([]string, error) {
	workspace, err := getWrkspcPath()
	if err != nil {
		return []string{}, err
	}
	chFiles, err := getChFiles(ctx, workspace, e.log)
	if err != nil {
		e.log.Errorw("failed to get changed filed in runTests step", "step_id", e.id, zap.Error(err))
		return []string{}, err
	}

	e.log.Infow(fmt.Sprintf("using changed files list %s to figure out which tests to run", chFiles), "step_id", e.id)
	return chFiles, nil
}

// Run executes tests with provided args with retries and timeout handling
func (e *runTestsStep) Run(ctx context.Context) (*output.StepOutput, int32, error) {
	if err := e.validate(); err != nil {
		e.log.Errorw("failed to validate runTestsStep step", "step_id", e.id, zap.Error(err))
		return nil, int32(1), err
	}
	out, retries, err := e.execute(ctx)
	// TODO: Add JEXL resolution to fields that need to be resolved
	// processCg should be called regardless of execute returns successfully or not
	// for instance the script could error out in post steps or somewhere halfway through running tests
	// If execute has errored out, return of this step would be err.
	// If execute completed successfully but processCg() failed still the step will be marked as error.
	// if the runTests step failed, then overall step status will be failure
	if err != nil {
		return out, retries, err
	}
	cgErr := e.processCg()
	// if the runTests step passed but the cg generation failed, then overall step status will be failure
	if cgErr != nil {
		e.log.Errorw("failed to process callgraph", zap.Error(cgErr))
		return out, retries, cgErr
	}
	return out, retries, err
}

// validate the container port and language
func (e *runTestsStep) validate() error {
	if e.runTestsInfo.GetContainerPort() == 0 {
		return fmt.Errorf("runTestsStep container port is not set")
	}

	if e.runTestsInfo.GetLanguage() != "java" {
		e.log.Errorw(fmt.Sprintf("only java is supported as the codebase language. Received language is: %s", e.runTestsInfo.GetLanguage()), "step_id", e.id)
		return fmt.Errorf("unsupported language in test intelligence step")
	}
	return nil
}

// execute step and send the rpc call to addon server for running tests
func (e *runTestsStep) execute(ctx context.Context) (*output.StepOutput, int32, error) {
	st := time.Now()

	diffFiles, err := e.getDiffFiles(ctx)
	if err != nil {
		return nil, int32(1), err
	}

	addonClient, err := newAddonClient(uint(e.runTestsInfo.GetContainerPort()), e.log)
	if err != nil {
		e.log.Errorw("unable to create CI addon client", "step_id", e.id, zap.Error(err))
		return nil, int32(1), errors.Wrap(err, "could not create CI Addon client")
	}
	defer addonClient.CloseConn()

	c := addonClient.Client()
	arg := e.getExecuteStepArg(diffFiles)
	ret, err := c.ExecuteStep(ctx, arg, grpc_retry.WithMax(maxAddonRetries))
	if err != nil {
		e.log.Errorw("execute run tests step RPC failed", "step_id", e.id, "elapsed_time_ms",
			utils.TimeSince(st), zap.Error(err))
		return nil, int32(1), err
	}
	e.log.Infow("successfully executed run tests step", "elapsed_time_ms", utils.TimeSince(st))
	stepOutput := &output.StepOutput{}
	stepOutput.Output.Variables = ret.GetOutput()
	return stepOutput, ret.GetNumRetries(), nil
}

func (e *runTestsStep) getExecuteStepArg(diffFiles []string) *addonpb.ExecuteStepRequest {
	// not the best practice, can take up proxying git calls later
	e.runTestsInfo.DiffFiles = diffFiles
	e.step.Step = &pb.UnitStep_RunTests{
		RunTests: e.runTestsInfo,
	}
	return &addonpb.ExecuteStepRequest{
		Step: e.step,
	}
}

func (e *runTestsStep) processCg() error {
	fs := fs.NewOSFileSystem(e.log)
	parser := ti.NewCallGraphParser(e.log, fs)
	cg, err := parser.Parse(cgDir)
	if err != nil {
		e.log.Errorf("failed to read callgraph file", zap.Error(err))
		return err
	}
	e.log.Infow(fmt.Sprintf("succesfully read callgraph files. size of nodes:[%d], size of relations:[%d]", len(cg.Nodes), len(cg.Relations)))
	cgMap := cg.ToStringMap()
	cgSer, err := avro.NewCgphSerialzer(cgSchemaPath)
	if err != nil {
		e.log.Errorf("failed to create callgraph serializer instance", zap.Error(err))
		return err
	}
	encBytes, err := cgSer.Serialize(cgMap)
	if err != nil {
		e.log.Errorf("failed to encode callgraph", zap.Error(err))
		return err
	}
	client, err := remoteTiClient()
	if err != nil {
		e.log.Errorf("failed to create tiClient", zap.Error(err))
		return err
	}
	org, err := getOrgId()
	if err != nil {
		return err
	}
	project, err := getProjectId()
	if err != nil {
		return err
	}
	pipeline, err := getPipelineId()
	if err != nil {
		return err
	}
	build, err := getBuildId()
	if err != nil {
		return err
	}
	stage, err := getStageId()
	if err != nil {
		return err
	}
	err = client.UploadCg(org, project, pipeline, build, stage, e.id, "repo", "sha", "branch", encBytes)
	if err != nil {
		e.log.Errorf("failed to upload cg to ti server", zap.Error(err))
		return err
	}
	return nil
}
