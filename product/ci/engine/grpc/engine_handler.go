package grpc

import (
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"github.com/wings-software/portal/product/ci/engine/state"
	"go.uber.org/zap"
	"golang.org/x/net/context"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

// handler is used to implement EngineServer
type engineHandler struct {
	log *zap.SugaredLogger
}

// NewEngineHandler returns a GRPC handler that implements pb.EngineServer
func NewEngineHandler(log *zap.SugaredLogger) pb.LiteEngineServer {
	return &engineHandler{log}
}

// UpdateState updates the execution state.
// If required state is resume, it sets the execution state to running and sends a signal via resume channel to resume the paused execution.
func (h *engineHandler) UpdateState(ctx context.Context, in *pb.UpdateStateRequest) (*pb.UpdateStateResponse, error) {
	if in.GetAction() == pb.UpdateStateRequest_UNKNOWN {
		h.log.Errorw("Unknown action in incoming request")
		return &pb.UpdateStateResponse{}, status.Error(codes.InvalidArgument, "Unknown action")
	}

	s := state.ExecutionState()
	if in.GetAction() == pb.UpdateStateRequest_PAUSE {
		h.log.Infow("Pausing pipeline execution")
		s.SetState(state.PAUSED)
	} else {
		h.log.Infow("Resuming pipeline execution")
		s.SetState(state.RUNNING)
		ch := s.ResumeSignal()
		ch <- true
	}
	return &pb.UpdateStateResponse{}, nil
}
