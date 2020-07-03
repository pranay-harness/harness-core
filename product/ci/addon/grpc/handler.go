package grpc

import (
	pb "github.com/wings-software/portal/product/ci/addon/proto"
	"github.com/wings-software/portal/product/ci/addon/tasks"
	"go.uber.org/zap"
	"golang.org/x/net/context"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

// handler is used to implement CIAddonServer
type handler struct {
	log *zap.SugaredLogger
}

// NewCIAddonHandler returns a GRPC handler that implements pb.CIAddonServer
func NewCIAddonHandler(log *zap.SugaredLogger) pb.CIAddonServer {
	return &handler{log}
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

// TaskProgress returns the current status of a task run
func (h *handler) TaskProgress(ctx context.Context, in *pb.TaskProgressRequest) (*pb.TaskProgressResponse, error) {
	return &pb.TaskProgressResponse{}, nil
}

// TaskProgressUpdates streams status of a task run
func (h *handler) TaskProgressUpdates(in *pb.TaskProgressUpdatesRequest, stream pb.CIAddon_TaskProgressUpdatesServer) error {
	return nil
}
