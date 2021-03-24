package executor

import (
	"context"
	"time"

	grpc_retry "github.com/grpc-ecosystem/go-grpc-middleware/retry"
	"github.com/pkg/errors"
	"github.com/wings-software/portal/commons/go/lib/utils"
	caddon "github.com/wings-software/portal/product/ci/addon/grpc/client"
	addonpb "github.com/wings-software/portal/product/ci/addon/proto"
	"github.com/wings-software/portal/product/ci/engine/output"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

const (
	maxAddonRetries = 100 // max retry time of 10 seconds
)

var (
	newAddonClient = caddon.NewAddonClient
)

// ExecuteStepOnAddon executes customer provided step on addon
func ExecuteStepOnAddon(ctx context.Context, step *pb.UnitStep, tmpFilePath string,
	log *zap.SugaredLogger) (*output.StepOutput, error) {
	st := time.Now()
	containerPort := step.GetContainerPort()
	stepID := step.GetId()

	addonClient, err := newAddonClient(uint(containerPort), log)
	if err != nil {
		log.Errorw("Unable to create CI addon client", "step_id", stepID,
			"elapsed_time_ms", utils.TimeSince(st), zap.Error(err))
		return nil, errors.Wrap(err, "Could not create CI Addon client")
	}
	defer addonClient.CloseConn()

	c := addonClient.Client()
	arg := &addonpb.ExecuteStepRequest{
		Step:        step,
		TmpFilePath: tmpFilePath,
	}
	ret, err := c.ExecuteStep(ctx, arg, grpc_retry.WithMax(maxAddonRetries))
	if err != nil {
		log.Errorw("Execute step RPC failed", "step_id", stepID,
			"elapsed_time_ms", utils.TimeSince(st), zap.Error(err))
		return nil, err
	}

	log.Infow("Successfully executed step", "step_id", stepID,
		"elapsed_time_ms", utils.TimeSince(st))
	stepOutput := &output.StepOutput{}
	stepOutput.Output.Variables = ret.GetOutput()
	return stepOutput, nil
}

// StopAddon stops addon grpc service running on specified port
func StopAddon(ctx context.Context, stepID string, port uint32, log *zap.SugaredLogger) error {
	addonClient, err := newAddonClient(uint(port), log)
	if err != nil {
		log.Errorw("Could not create CI Addon client", "step_id", stepID, zap.Error(err))
		return errors.Wrap(err, "Could not create CI Addon client")
	}
	defer addonClient.CloseConn()

	_, err = addonClient.Client().SignalStop(ctx, &addonpb.SignalStopRequest{})
	if err != nil {
		log.Errorw("Unable to send Stop server request", "step_id", stepID, zap.Error(err))
		return errors.Wrap(err, "Could not send stop server request")
	}

	log.Infow("Successfully stopped step container", "step_id", stepID, "port", port)
	return nil
}
