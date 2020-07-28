package steps

// Proxy the request to CI addon

import (
	"context"
	"fmt"
	"github.com/gofrs/uuid"
	"github.com/pkg/errors"
	"time"

	"github.com/wings-software/portal/commons/go/lib/filesystem"
	"github.com/wings-software/portal/commons/go/lib/utils"
	caddon "github.com/wings-software/portal/product/ci/addon/grpc/client"
	addonpb "github.com/wings-software/portal/product/ci/addon/proto"
	enginepb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

var (
	newAddonClient = caddon.NewAddonClient
)

// mockgen -source publish_artifacts.go -package steps -destination mocks/publish_artifacts_mock.go

// PublishArtifactsStep to publish artifacts to an artifactory
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

func NewPublishArtifactsStep(step *enginepb.UnitStep, log *zap.SugaredLogger) PublishArtifactsStep {
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
	arg, err := s.createPublishArtifactArg()
	if err != nil {
		s.log.Warnw("Failed to generate request argument", "error_msg", zap.Error(err))
		return err
	}

	addonClient, err := newAddonClient(caddon.AddonPort, s.log)
	if err != nil {
		s.log.Warnw("Unable to create CI addon client", "error_msg", zap.Error(err))
		return errors.Wrap(err, "Could not create CI Addon client")
	}
	defer addonClient.CloseConn()

	c := addonClient.Client()
	_, err = c.PublishArtifacts(ctx, arg)
	if err != nil {
		s.log.Warnw("Publish artifact RPC failed", "error_msg", zap.Error(err), "elapsed_time_ms", utils.TimeSince(st))
		return err
	}
	s.log.Infow("Successfully published artifacts", "elapsed_time_ms", utils.TimeSince(st))
	return nil
}

// createPublishArtifactArg method creates arguments for addon PublishArtifacts GRPC
func (s *publishArtifactsStep) createPublishArtifactArg() (*addonpb.PublishArtifactsRequest, error) {
	newUUID, err := uuid.NewV4()
	if err != nil {
		return nil, errors.Wrap(err, "Failed to generate UUID")
	}
	taskID := &addonpb.TaskId{Id: newUUID.String()}

	// Resolving home directory for files and images
	var resolvedFiles []*addonpb.UploadFile
	for _, file := range s.files {
		if file.FilePattern, err = expand(file.GetFilePattern()); err != nil {
			return nil, err
		}
		resolvedFiles = append(resolvedFiles, file)
	}

	var resolvedImages []*addonpb.BuildPublishImage
	for _, image := range s.images {
		if image.DockerFile, err = expand(image.GetDockerFile()); err != nil {
			return nil, err
		}
		if image.Context, err = expand(image.GetContext()); err != nil {
			return nil, err
		}
		resolvedImages = append(resolvedImages, image)
	}

	return &addonpb.PublishArtifactsRequest{
		TaskId: taskID,
		Files:  resolvedFiles,
		Images: resolvedImages,
	}, nil
}

// expand method expands the filepath to resolve tilde and environment variables
// TODO: Add resolution of environment variables
func expand(filepath string) (string, error) {
	path, err := filesystem.ExpandTilde(filepath)
	if err != nil {
		return "", errors.Wrap(err, fmt.Sprintf("failed to expand %s", filepath))
	}
	return path, nil
}
