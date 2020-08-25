package grpc

import (
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/logs"
	pb "github.com/wings-software/portal/product/ci/scm/proto"
	"go.uber.org/zap"
	"golang.org/x/net/context"
)

func TestParseWebhookErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	stopCh := make(chan bool)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	h := NewSCMHandler(stopCh, log.Sugar())
	_, err := h.ParseWebhook(ctx, &pb.ParseWebhookRequest{})
	assert.NotNil(t, err)
}
