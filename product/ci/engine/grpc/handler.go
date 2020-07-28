package grpc

import (
	"time"

	"github.com/wings-software/portal/product/ci/engine/executor"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
	"golang.org/x/net/context"
)

// handler is used to implement LiteEngineServer
type handler struct {
	stepLogPath  string
	tmpFilePath  string
	stopCh       chan bool
	unitExecutor executor.UnitExecutor
	log          *zap.SugaredLogger
}

// NewLiteEngineHandler returns a GRPC handler that implements pb.LiteEngineServer
func NewLiteEngineHandler(stepLogPath, tmpFilePath string, stopCh chan bool, log *zap.SugaredLogger) pb.LiteEngineServer {
	ue := executor.NewUnitExecutor(stepLogPath, tmpFilePath, log)
	return &handler{stepLogPath, tmpFilePath, stopCh, ue, log}
}

// ExecuteStep executes a unit step
func (h *handler) ExecuteStep(ctx context.Context, in *pb.ExecuteStepRequest) (*pb.ExecuteStepResponse, error) {
	err := h.unitExecutor.Run(ctx, in.GetStep())
	return &pb.ExecuteStepResponse{}, err
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
