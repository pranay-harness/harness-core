package steps

// Proxy the request to CI addon

import (
	"context"
	"time"
	"github.com/gofrs/uuid"
	"github.com/pkg/errors"

	"github.com/wings-software/portal/commons/go/lib/utils"
	addonpb "github.com/wings-software/portal/product/ci/addon/proto"
	egrpc "github.com/wings-software/portal/product/ci/engine/grpc"
	enginepb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

var (
	newCIAddonClient = egrpc.NewCIAddonClient
)

const (
	ciAddonPort = 8001
)

// mockgen -source publish_artifacts.go -package steps -destination mocks/publish_artifacts_mock.go

// Step to publish artifacts to an artifactory
type PublishArtifactsStep interface {
	Run(ctx context.Context) error
}

type publishArtifactsStep struct {
	id          string
	displayName string
	files       []*addonpb.UploadFile
	images      []*addonpb.BuildPublishImage
	log         *zap.SugaredLogger
}

func NewPublishArtifactsStep(step *enginepb.Step, log *zap.SugaredLogger) PublishArtifactsStep {
	s := step.GetPublishArtifacts()
	return &publishArtifactsStep{
		id:          step.GetId(),
		displayName: step.GetDisplayName(),
		files:       s.GetFiles(),
		images:      s.GetImages(),
		log:         log,
	}
}

func (s *publishArtifactsStep) Run(ctx context.Context) error {
	st := time.Now()
	ciAddonClient, err := newCIAddonClient(ciAddonPort, s.log)
	if err != nil {
		s.log.Warnw("Unable to create CI addon client", "error_msg", zap.Error(err))
		return errors.Wrap(err, "Could not create CI Addon client")
	}
	defer ciAddonClient.CloseConn()

	newUUID, err := uuid.NewV4()
	if err != nil {
		s.log.Warnw("Unable to generate UUID", "error_msg", zap.Error(err))
		return errors.Wrap(err, "Failed to generate UUID")
	}
	taskIdString := newUUID.String()
	taskId := &addonpb.TaskId{Id: taskIdString}
	req := &addonpb.PublishArtifactsRequest{TaskId: taskId, Files: s.files, Images: s.images}
	c := ciAddonClient.Client()
	_, err = c.PublishArtifacts(ctx, req)
	if err != nil {
		s.log.Warnw("Publish artifact RPC failed", "error_msg", zap.Error(err), "elapsed_time_ms", utils.TimeSince(st))
		return err
	}
	s.log.Infow("Successfully published artifacts", "elapsed_time_ms", utils.TimeSince(st))
	return nil
}
