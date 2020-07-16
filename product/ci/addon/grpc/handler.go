package grpc

import (
	"time"

	pb "github.com/wings-software/portal/product/ci/addon/proto"
	"github.com/wings-software/portal/product/ci/addon/tasks"
	"go.uber.org/zap"
	"golang.org/x/net/context"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

// handler is used to implement CIAddonServer
type handler struct {
	stopCh chan bool
	log    *zap.SugaredLogger
}

// NewCIAddonHandler returns a GRPC handler that implements pb.CIAddonServer
func NewCIAddonHandler(stopCh chan bool, log *zap.SugaredLogger) pb.CIAddonServer {
	return &handler{stopCh, log}
}

// PublishArtifacts implements uploading files or building docker images and publishing them
func (h *handler) PublishArtifacts(ctx context.Context, in *pb.PublishArtifactsRequest) (*pb.PublishArtifactsResponse, error) {
	taskID := in.GetTaskId().GetId()
	if taskID == "" {
		return nil, status.Error(codes.InvalidArgument, "task id is not set")
	}

	l := h.log.With(zap.String("task_id", taskID))
	t := tasks.NewPublishArtifactsTask(l)
	err := t.Publish(ctx, in)
	return &pb.PublishArtifactsResponse{}, err
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

// TaskProgress returns the current status of a task run
func (h *handler) TaskProgress(ctx context.Context, in *pb.TaskProgressRequest) (*pb.TaskProgressResponse, error) {
	return &pb.TaskProgressResponse{}, nil
}

// TaskProgressUpdates streams status of a task run
func (h *handler) TaskProgressUpdates(in *pb.TaskProgressUpdatesRequest, stream pb.CIAddon_TaskProgressUpdatesServer) error {
	return nil
}
