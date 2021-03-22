package grpc

import (
	"fmt"
	"time"

	addonlogs "github.com/wings-software/portal/product/ci/addon/logs"
	pb "github.com/wings-software/portal/product/ci/addon/proto"
	"github.com/wings-software/portal/product/ci/addon/tasks"
	"github.com/wings-software/portal/product/ci/engine/logutil"
	enginepb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
	"golang.org/x/net/context"
)

// handler is used to implement AddonServer
type handler struct {
	stopCh     chan bool
	logMetrics bool
	log        *zap.SugaredLogger
}

var (
	newGrpcRemoteLogger = logutil.GetGrpcRemoteLogger
	newRunTask          = tasks.NewRunTask
	newRunTestsTask     = tasks.NewRunTestsTask
	newPluginTask       = tasks.NewPluginTask
)

// NewAddonHandler returns a GRPC handler that implements pb.AddonServer
func NewAddonHandler(stopCh chan bool, logMetrics bool, log *zap.SugaredLogger) pb.AddonServer {
	return &handler{stopCh, logMetrics, log}
}

// SignalStop sends a signal to stop the GRPC service.
func (h *handler) SignalStop(ctx context.Context, in *pb.SignalStopRequest) (*pb.SignalStopResponse, error) {
	go func() {
		// Ensure that all the addon service tasks are complete before sending the signal.
		// Sleep will ensure that this RPC completes successfully
		time.Sleep(1 * time.Second)
		h.stopCh <- true
	}()
	// Explicitly close pending logs before returning back, as they depend on the lite engine
	// server being up.
	addonlogs.LogState().ClosePendingLogs()
	return &pb.SignalStopResponse{}, nil
}

// ExecuteStep executes a unit step.
func (h *handler) ExecuteStep(ctx context.Context, in *pb.ExecuteStepRequest) (*pb.ExecuteStepResponse, error) {
	rl, err := newGrpcRemoteLogger(in.GetStep().GetLogKey())
	if err != nil {
		return &pb.ExecuteStepResponse{}, err
	}
	defer rl.Writer.Close()

	switch x := in.GetStep().GetStep().(type) {
	case *enginepb.UnitStep_Run:
		stepOutput, numRetries, err := newRunTask(in.GetStep(), in.GetPrevStepOutputs(), in.GetTmpFilePath(), rl.BaseLogger,
			rl.Writer, h.logMetrics, h.log).Run(ctx)
		response := &pb.ExecuteStepResponse{
			Output:     stepOutput,
			NumRetries: numRetries,
		}
		return response, err
	case *enginepb.UnitStep_RunTests:
		numRetries, err := newRunTestsTask(in.GetStep(), in.GetTmpFilePath(), rl.BaseLogger, rl.Writer, h.logMetrics, h.log).Run(ctx)
		response := &pb.ExecuteStepResponse{
			NumRetries: numRetries,
		}
		return response, err
	case *enginepb.UnitStep_Plugin:
		numRetries, err := newPluginTask(in.GetStep(), in.GetPrevStepOutputs(), rl.BaseLogger, rl.Writer, h.logMetrics, h.log).Run(ctx)
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
