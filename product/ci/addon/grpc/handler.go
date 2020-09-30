package grpc

import (
	"fmt"
	"time"

	pb "github.com/wings-software/portal/product/ci/addon/proto"
	"github.com/wings-software/portal/product/ci/addon/tasks"
	enginepb "github.com/wings-software/portal/product/ci/engine/proto"
	logger "github.com/wings-software/portal/product/ci/logger/util"
	"go.uber.org/zap"
	"golang.org/x/net/context"
)

// handler is used to implement AddonServer
type handler struct {
	stopCh chan bool
	log    *zap.SugaredLogger
}

var (
	newRemoteLogger = logger.GetRemoteLogger
	newRunTask      = tasks.NewRunTask
	newPluginTask   = tasks.NewPluginTask
)

// NewAddonHandler returns a GRPC handler that implements pb.AddonServer
func NewAddonHandler(stopCh chan bool, log *zap.SugaredLogger) pb.AddonServer {
	return &handler{stopCh, log}
}

// SignalStop sends a signal to stop the GRPC service.
func (h *handler) SignalStop(ctx context.Context, in *pb.SignalStopRequest) (*pb.SignalStopResponse, error) {
	go func() {
		// Ensure that all the addon service tasks are complete before sending the signal.
		// Sleep will ensure that this RPC completes successfully
		time.Sleep(1 * time.Second)
		h.stopCh <- true
	}()
	return &pb.SignalStopResponse{}, nil
}

// ExecuteStep executes a unit step.
func (h *handler) ExecuteStep(ctx context.Context, in *pb.ExecuteStepRequest) (*pb.ExecuteStepResponse, error) {
	rl, err := newRemoteLogger(in.GetStep().GetId())
	if err != nil {
		return &pb.ExecuteStepResponse{}, err
	}
	defer rl.Writer.Close()

	switch x := in.GetStep().GetStep().(type) {
	case *enginepb.UnitStep_Run:
		stepOutput, numRetries, err := newRunTask(in.GetStep(), in.GetTmpFilePath(), rl.BaseLogger, rl.Writer).Run(ctx)
		response := &pb.ExecuteStepResponse{
			Output:     stepOutput,
			NumRetries: numRetries,
		}
		return response, err
	case *enginepb.UnitStep_Plugin:
		numRetries, err := newPluginTask(in.GetStep(), rl.BaseLogger, rl.Writer).Run(ctx)
		response := &pb.ExecuteStepResponse{
			NumRetries: numRetries,
		}
		return response, err
	case nil:
		return &pb.ExecuteStepResponse{}, fmt.Errorf("UnitStep is not set")
	default:
		return &pb.ExecuteStepResponse{}, fmt.Errorf("UnitStep has unexpected type %T", x)
	}
}
