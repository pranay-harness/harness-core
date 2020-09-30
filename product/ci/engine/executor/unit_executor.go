package executor

import (
	"context"
	"fmt"
	"time"

	"github.com/pkg/errors"
	"github.com/wings-software/portal/commons/go/lib/filesystem"
	"github.com/wings-software/portal/commons/go/lib/logs"
	caddon "github.com/wings-software/portal/product/ci/addon/grpc/client"
	addonpb "github.com/wings-software/portal/product/ci/addon/proto"
	"github.com/wings-software/portal/product/ci/engine/output"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"github.com/wings-software/portal/product/ci/engine/status"
	"github.com/wings-software/portal/product/ci/engine/steps"
	logger "github.com/wings-software/portal/product/ci/logger/util"
	"go.uber.org/zap"
)

var (
	saveCacheStep        = steps.NewSaveCacheStep
	restoreCacheStep     = steps.NewRestoreCacheStep
	publishArtifactsStep = steps.NewPublishArtifactsStep
	runStep              = steps.NewRunStep
	pluginStep           = steps.NewPluginStep
	sendStepStatus       = status.SendStepStatus
	newRemoteLogger      = logger.GetRemoteLogger
	newAddonClient       = caddon.NewAddonClient
)

//go:generate mockgen -source unit_executor.go -package=executor -destination mocks/unit_executor_mock.go UnitExecutor

// UnitExecutor represents an interface to execute a unit step
type UnitExecutor interface {
	Run(ctx context.Context, step *pb.UnitStep, so output.StageOutput, accountID string) (*output.StepOutput, error)
	Cleanup(ctx context.Context, step *pb.UnitStep) error
}

type unitExecutor struct {
	tmpFilePath string // File path to store generated temporary files
	log         *zap.SugaredLogger
}

// NewUnitExecutor creates a unit step executor
func NewUnitExecutor(tmpFilePath string, log *zap.SugaredLogger) UnitExecutor {
	return &unitExecutor{
		tmpFilePath: tmpFilePath,
		log:         log,
	}
}

func (e *unitExecutor) validate(step *pb.UnitStep) error {
	if step.GetId() == "" {
		err := fmt.Errorf("Step ID should be non-empty")
		e.log.Errorw("Step ID is not set", zap.Error(err))
		return err
	}
	if step.GetCallbackToken() == "" {
		err := fmt.Errorf("Callback token should be non-empty")
		e.log.Errorw("Callback token is not set", zap.Error(err))
		return err
	}
	if step.GetTaskId() == "" {
		err := fmt.Errorf("Task ID should be non-empty")
		e.log.Errorw("Task ID is not set", zap.Error(err))
		return err
	}
	return nil
}

// Executes a unit step
func (e *unitExecutor) Run(ctx context.Context, step *pb.UnitStep, so output.StageOutput,
	accountID string) (*output.StepOutput, error) {
	// Ensure step_id is present as a parameter for all logs
	e.log = e.log.With(zap.String("step_id", step.GetId()))

	start := time.Now()
	stepOutput, numRetries, err := e.execute(ctx, step, so)
	timeTaken := time.Since(start)

	callbackToken := step.GetCallbackToken()
	taskID := step.GetTaskId()
	statusErr := sendStepStatus(ctx, accountID, callbackToken, taskID, numRetries, timeTaken,
		stepOutput, err, e.log)
	if statusErr != nil {
		e.log.Errorw("Failed to send step status. Bailing out stage execution", zap.Error(err))
		return nil, statusErr
	}
	return stepOutput, err
}

func (e *unitExecutor) execute(ctx context.Context, step *pb.UnitStep,
	so output.StageOutput) (*output.StepOutput, int32, error) {
	numRetries := int32(1)
	var rl *logs.RemoteLogger
	var err error
	var stepOutput *output.StepOutput

	fs := filesystem.NewOSFileSystem(e.log)
	if err = e.validate(step); err != nil {
		return nil, numRetries, err
	}

	switch x := step.GetStep().(type) {
	case *pb.UnitStep_Run:
		e.log.Infow("Run step info", "step", x.Run.String())
		stepOutput, numRetries, err = runStep(step, e.tmpFilePath, so, e.log).Run(ctx)
		if err != nil {
			return nil, numRetries, err
		}
	case *pb.UnitStep_Plugin:
		e.log.Infow("Plugin step info", "step", x.Plugin.String())
		numRetries, err = pluginStep(step, e.log).Run(ctx)
		if err != nil {
			return nil, numRetries, err
		}
	case *pb.UnitStep_SaveCache:
		rl, err = newRemoteLogger(step.GetId())
		if err != nil {
			return nil, numRetries, err
		}
		defer rl.Writer.Close()
		e.log.Infow("Save cache step info", "step", x.SaveCache.String())
		stepOutput, err = saveCacheStep(step, e.tmpFilePath, so, fs, rl.BaseLogger).Run(ctx)
		if err != nil {
			return nil, numRetries, err
		}
	case *pb.UnitStep_RestoreCache:
		rl, err = newRemoteLogger(step.GetId())
		if err != nil {
			return nil, numRetries, err
		}
		defer rl.Writer.Close()
		e.log.Infow("Restore cache step info", "step", x.RestoreCache.String())
		if err = restoreCacheStep(step, e.tmpFilePath, so, fs, rl.BaseLogger).Run(ctx); err != nil {
			return nil, numRetries, err
		}
	case *pb.UnitStep_PublishArtifacts:
		rl, err = newRemoteLogger(step.GetId())
		if err != nil {
			return nil, numRetries, err
		}
		defer rl.Writer.Close()
		e.log.Infow("Publishing artifact info", "step", x.PublishArtifacts.String())
		if err = publishArtifactsStep(step, so, rl.BaseLogger).Run(ctx); err != nil {
			return nil, numRetries, err
		}
	case nil:
		e.log.Infow("Field is not set", "step", x)
	default:
		return nil, numRetries, fmt.Errorf("UnitStep has unexpected type %T", x)
	}

	return stepOutput, numRetries, nil
}

func (e *unitExecutor) Cleanup(ctx context.Context, step *pb.UnitStep) error {
	switch x := step.GetStep().(type) {
	case *pb.UnitStep_Run:
		port := x.Run.GetContainerPort()
		return e.stopAddonServer(ctx, uint(port))
	case *pb.UnitStep_Plugin:
		port := x.Plugin.GetContainerPort()
		return e.stopAddonServer(ctx, uint(port))
	}
	return nil
}

// Stops CI-Addon GRPC server
func (e *unitExecutor) stopAddonServer(ctx context.Context, port uint) error {
	addonClient, err := newAddonClient(port, e.log)
	if err != nil {
		e.log.Errorw("Could not create CI Addon client", zap.Error(err))
		return errors.Wrap(err, "Could not create CI Addon client")
	}
	defer addonClient.CloseConn()

	_, err = addonClient.Client().SignalStop(ctx, &addonpb.SignalStopRequest{})
	if err != nil {
		e.log.Errorw("Unable to send Stop server request", zap.Error(err))
		return errors.Wrap(err, "Could not send stop server request")
	}
	return nil
}
