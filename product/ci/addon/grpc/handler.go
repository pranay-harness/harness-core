package grpc

import (
	"time"

	pb "github.com/wings-software/portal/product/ci/addon/proto"
	"github.com/wings-software/portal/product/ci/addon/tail"
	"github.com/wings-software/portal/product/ci/addon/tasks"
	"go.uber.org/zap"
	"golang.org/x/net/context"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

// handler is used to implement AddonServer
type handler struct {
	stopCh chan bool
	log    *zap.SugaredLogger
}

// NewAddonHandler returns a GRPC handler that implements pb.AddonServer
func NewAddonHandler(stopCh chan bool, log *zap.SugaredLogger) pb.AddonServer {
	return &handler{stopCh, log}
}

// PublishArtifacts implements uploading files or building docker images and publishing them
func (h *handler) PublishArtifacts(ctx context.Context, in *pb.PublishArtifactsRequest) (*pb.PublishArtifactsResponse, error) {
	taskID := in.GetTaskId().GetId()
	if taskID == "" {
		return nil, status.Error(codes.InvalidArgument, "task id is not set")
	}

	l := h.log.With(zap.String("task_id", taskID), zap.String("step_id", in.GetStepId()))

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

// StartTail starts tailing on a file name. It follows the file for any new output being added to it.
// It will start listening even if the file has not been created yet. Out of multiple concurrent requests for
// the same file, only one will succeed. The same filename can be watched again only if StopTail RPC has been
// invoked on it.
func (h *handler) StartTail(ctx context.Context, in *pb.StartTailRequest) (*pb.StartTailResponse, error) {
	ret, err := tail.Start(ctx, in, h.log)
	return ret, err
}

// StopTail is used to gracefully stop tailing on a file. `Wait` can be optionally specified to ensure that
// the logs of the file are completely dumped to stdout before returning
func (h *handler) StopTail(ctx context.Context, in *pb.StopTailRequest) (*pb.StopTailResponse, error) {
	ret, err := tail.Stop(ctx, in, h.log)
	return ret, err
}

// TaskProgress returns the current status of a task run
func (h *handler) TaskProgress(ctx context.Context, in *pb.TaskProgressRequest) (*pb.TaskProgressResponse, error) {
	return &pb.TaskProgressResponse{}, nil
}

// TaskProgressUpdates streams status of a task run
func (h *handler) TaskProgressUpdates(in *pb.TaskProgressUpdatesRequest, stream pb.Addon_TaskProgressUpdatesServer) error {
	return nil
}
