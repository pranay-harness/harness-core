package grpc

import (
	"github.com/wings-software/portal/product/ci/scm/file"
	"github.com/wings-software/portal/product/ci/scm/parser"
	pb "github.com/wings-software/portal/product/ci/scm/proto"
	"go.uber.org/zap"
	"golang.org/x/net/context"
)

// handler is used to implement SCMServer
type handler struct {
	stopCh chan bool
	log    *zap.SugaredLogger
}

// NewSCMHandler returns a GRPC handler that implements pb.SCMServer
func NewSCMHandler(stopCh chan bool, log *zap.SugaredLogger) pb.SCMServer {
	return &handler{stopCh, log}
}

// ParseWebhook is used to parse the webhook
func (h *handler) ParseWebhook(ctx context.Context, in *pb.ParseWebhookRequest) (*pb.ParseWebhookResponse, error) {
	return parser.ParseWebhook(ctx, in, h.log)
}

// Createfile is used to create a file
func (h *handler) CreateFile(ctx context.Context, in *pb.FileModifyRequest) (*pb.ContentResponse, error) {
	return file.CreateFile(ctx, in, h.log)
}

// DeleteFile is used to delete a file
func (h *handler) DeleteFile(ctx context.Context, in *pb.FileDeleteRequest) (*pb.ContentResponse, error) {
	return file.DeleteFile(ctx, in, h.log)
}

// FindFile is used to return a file
func (h *handler) FindFile(ctx context.Context, in *pb.FileFindRequest) (*pb.ContentResponse, error) {
	return file.FindFile(ctx, in, h.log)
}

// UpdateFile is used to update a file
func (h *handler) UpdateFile(ctx context.Context, in *pb.FileModifyRequest) (*pb.ContentResponse, error) {
	return file.UpdateFile(ctx, in, h.log)
}

// UpsertFile is used to create a file if it doesnt exist, or update the file if it does.
func (h *handler) UpsertFile(ctx context.Context, in *pb.FileModifyRequest) (*pb.ContentResponse, error) {
	return file.UpsertFile(ctx, in, h.log)
}
