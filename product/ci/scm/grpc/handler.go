package grpc

import (
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
