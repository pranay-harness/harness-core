package steps

import (
	"context"
	"fmt"
	"time"

	grpc_retry "github.com/grpc-ecosystem/go-grpc-middleware/retry"
	"github.com/pkg/errors"
	"github.com/wings-software/portal/commons/go/lib/utils"
	caddon "github.com/wings-software/portal/product/ci/addon/grpc/client"
	addonpb "github.com/wings-software/portal/product/ci/addon/proto"
	"github.com/wings-software/portal/product/ci/engine/jexl"
	"github.com/wings-software/portal/product/ci/engine/output"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

//go:generate mockgen -source run.go -package=steps -destination mocks/run_mock.go RunStep

const (
	outputEnvSuffix string = "output"
	maxAddonRetries        = 2000 // max retry time of 200 seconds
)

var (
	evaluateJEXL   = jexl.EvaluateJEXL
	newAddonClient = caddon.NewAddonClient
)

// RunStep represents interface to execute a run step
type RunStep interface {
	Run(ctx context.Context) (*output.StepOutput, int32, error)
}

type runStep struct {
	id            string
	displayName   string
	tmpFilePath   string
	command       string
	envVarOutputs []string
	containerPort uint32
	stepContext   *pb.StepContext
	reports       []*pb.Report
	stageOutput   output.StageOutput
	log           *zap.SugaredLogger
}

// NewRunStep creates a run step executor
func NewRunStep(step *pb.UnitStep, tmpFilePath string, so output.StageOutput,
	log *zap.SugaredLogger) RunStep {
	r := step.GetRun()
	return &runStep{
		id:            step.GetId(),
		displayName:   step.GetDisplayName(),
		command:       r.GetCommand(),
		containerPort: r.GetContainerPort(),
		stepContext:   r.GetContext(),
		envVarOutputs: r.GetEnvVarOutputs(),
		reports:       r.GetReports(),
		tmpFilePath:   tmpFilePath,
		stageOutput:   so,
		log:           log,
	}
}

// Executes customer provided run step commands with retries and timeout handling
func (e *runStep) Run(ctx context.Context) (*output.StepOutput, int32, error) {
	if err := e.validate(); err != nil {
		e.log.Errorw("failed to validate run step", "step_id", e.id, zap.Error(err))
		return nil, int32(1), err
	}
	if err := e.resolveJEXL(ctx); err != nil {
		return nil, int32(1), err
	}
	return e.execute(ctx)
}

func (e *runStep) validate() error {
	if len(e.command) == 0 {
		err := fmt.Errorf("command in run step should be non-empty string")
		return err
	}
	if e.containerPort == 0 {
		err := fmt.Errorf("run step container port is not set")
		return err
	}
	return nil
}

// resolveJEXL resolves JEXL expressions present in run step input
func (e *runStep) resolveJEXL(ctx context.Context) error {
	// JEXL expressions are only present in run step command
	cmd := e.command
	resolvedExprs, err := evaluateJEXL(ctx, e.id, []string{cmd}, e.stageOutput, e.log)
	if err != nil {
		return err
	}

	// Updating step command with the resolved value of JEXL expressions
	resolvedCmd := cmd
	if val, ok := resolvedExprs[cmd]; ok {
		resolvedCmd = val
	}
	e.command = resolvedCmd
	return nil
}

func (e *runStep) execute(ctx context.Context) (*output.StepOutput, int32, error) {
	st := time.Now()

	addonClient, err := newAddonClient(uint(e.containerPort), e.log)
	if err != nil {
		e.log.Errorw("Unable to create CI addon client", "step_id", e.id, zap.Error(err))
		return nil, int32(1), errors.Wrap(err, "Could not create CI Addon client")
	}
	defer addonClient.CloseConn()

	c := addonClient.Client()
	arg := e.getExecuteStepArg()
	ret, err := c.ExecuteStep(ctx, arg, grpc_retry.WithMax(maxAddonRetries))
	if err != nil {
		e.log.Errorw("Execute run step RPC failed", "step_id", e.id, "elapsed_time_ms", utils.TimeSince(st), zap.Error(err))
		return nil, int32(1), err
	}
	e.log.Infow("Successfully executed step", "elapsed_time_ms", utils.TimeSince(st))
	stepOutput := &output.StepOutput{}
	stepOutput.Output.Variables = ret.GetOutput()
	return stepOutput, ret.GetNumRetries(), nil
}

func (e *runStep) getExecuteStepArg() *addonpb.ExecuteStepRequest {
	return &addonpb.ExecuteStepRequest{
		Step: &pb.UnitStep{
			Id:          e.id,
			DisplayName: e.displayName,
			Step: &pb.UnitStep_Run{
				Run: &pb.RunStep{
					Command:       e.command,
					Context:       e.stepContext,
					Reports:       e.reports,
					EnvVarOutputs: e.envVarOutputs,
				},
			},
		},
		TmpFilePath: e.tmpFilePath,
	}
}
