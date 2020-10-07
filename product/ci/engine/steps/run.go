package steps

import (
	"context"
	"fmt"
	"time"

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
	commands      []string
	envVarOutputs []string
	containerPort uint32
	stepContext   *pb.StepContext
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
		commands:      r.GetCommands(),
		containerPort: r.GetContainerPort(),
		stepContext:   r.GetContext(),
		envVarOutputs: r.GetEnvVarOutputs(),
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
	if len(e.commands) == 0 {
		err := fmt.Errorf("commands in run step should have atleast one item")
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
	// JEXL expressions are only present in run step commands
	s := e.commands
	resolvedExprs, err := evaluateJEXL(ctx, s, e.stageOutput, e.log)
	if err != nil {
		return err
	}

	// Updating step commands with the resolved value of JEXL expressions
	var resolvedCmds []string
	for _, cmd := range e.commands {
		if val, ok := resolvedExprs[cmd]; ok {
			resolvedCmds = append(resolvedCmds, val)
		} else {
			resolvedCmds = append(resolvedCmds, cmd)
		}
	}
	e.commands = resolvedCmds
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
	ret, err := c.ExecuteStep(ctx, arg)
	if err != nil {
		e.log.Errorw("Execute run step RPC failed", "step_id", e.id, "elapsed_time_ms", utils.TimeSince(st), zap.Error(err))
		return nil, int32(1), err
	}
	e.log.Infow("Successfully executed step", "elapsed_time_ms", utils.TimeSince(st))
	return &output.StepOutput{Output: ret.GetOutput()}, ret.GetNumRetries(), nil
}

func (e *runStep) getExecuteStepArg() *addonpb.ExecuteStepRequest {
	return &addonpb.ExecuteStepRequest{
		Step: &pb.UnitStep{
			Id:          e.id,
			DisplayName: e.displayName,
			Step: &pb.UnitStep_Run{
				Run: &pb.RunStep{
					Commands:      e.commands,
					Context:       e.stepContext,
					EnvVarOutputs: e.envVarOutputs,
				},
			},
		},
		TmpFilePath: e.tmpFilePath,
	}
}
