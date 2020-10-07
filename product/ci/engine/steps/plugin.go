package steps

import (
	"context"
	"time"

	"github.com/pkg/errors"
	"github.com/wings-software/portal/commons/go/lib/utils"
	addonpb "github.com/wings-software/portal/product/ci/addon/proto"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

//go:generate mockgen -source plugin.go -package=steps -destination mocks/plugin_mock.go PluginStep

// PluginStep represents interface to execute a plugin step
type PluginStep interface {
	Run(ctx context.Context) (int32, error)
}

type pluginStep struct {
	id             string
	displayName    string
	image          string
	imageSecretEnv string // Name of environment variable that stores the docker image secret. If not set, plugin is public.
	containerPort  uint32
	stepContext    *pb.StepContext
	log            *zap.SugaredLogger
}

// NewPluginStep creates a plugin step executor
func NewPluginStep(step *pb.UnitStep, log *zap.SugaredLogger) PluginStep {
	r := step.GetPlugin()
	return &pluginStep{
		id:             step.GetId(),
		displayName:    step.GetDisplayName(),
		image:          r.GetImage(),
		imageSecretEnv: r.GetImageSecretEnv(),
		containerPort:  r.GetContainerPort(),
		stepContext:    r.GetContext(),
		log:            log,
	}
}

// Executes customer provided plugin step
func (e *pluginStep) Run(ctx context.Context) (int32, error) {
	if err := e.validate(); err != nil {
		e.log.Errorw("failed to validate plugin step", "step_id", e.id, zap.Error(err))
		return int32(1), err
	}
	return e.execute(ctx)
}

func (e *pluginStep) validate() error {
	if e.image == "" {
		err := errors.New("plugin image is not set")
		return err
	}
	if e.containerPort == 0 {
		err := errors.New("plugin step container port is not set")
		return err
	}
	return nil
}

func (e *pluginStep) execute(ctx context.Context) (int32, error) {
	st := time.Now()

	addonClient, err := newAddonClient(uint(e.containerPort), e.log)
	if err != nil {
		e.log.Errorw("Unable to create CI addon client", "step_id", e.id, "elapsed_time_ms", utils.TimeSince(st), zap.Error(err))
		return int32(1), errors.Wrap(err, "Could not create CI Addon client")
	}
	defer addonClient.CloseConn()

	c := addonClient.Client()
	arg := e.getExecuteStepArg()
	ret, err := c.ExecuteStep(ctx, arg)
	if err != nil {
		e.log.Errorw("Plugin step RPC failed", "step_id", e.id, "elapsed_time_ms", utils.TimeSince(st), zap.Error(err))
		return int32(1), err
	}
	e.log.Infow("Successfully executed step", "step_id", e.id, "elapsed_time_ms", utils.TimeSince(st))
	return ret.GetNumRetries(), nil
}

func (e *pluginStep) getExecuteStepArg() *addonpb.ExecuteStepRequest {
	return &addonpb.ExecuteStepRequest{
		Step: &pb.UnitStep{
			Id:          e.id,
			DisplayName: e.displayName,
			Step: &pb.UnitStep_Plugin{
				Plugin: &pb.PluginStep{
					Image:          e.image,
					Context:        e.stepContext,
					ImageSecretEnv: e.imageSecretEnv,
				},
			},
		},
	}
}
