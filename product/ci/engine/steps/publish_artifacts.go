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
	"github.com/wings-software/portal/product/ci/engine/output"
	enginepb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

var (
	newAddonClient = caddon.NewAddonClient
)

//go:generate mockgen -source publish_artifacts.go -package steps -destination mocks/publish_artifacts_mock.go PublishArtifactsStep

// PublishArtifactsStep to publish artifacts to an artifactory
type PublishArtifactsStep interface {
	Run(ctx context.Context) error
}

type publishArtifactsStep struct {
	id          string
	displayName string
	stageOutput output.StageOutput
	files       []*addonpb.UploadFile
	images      []*addonpb.BuildPublishImage
	log         *zap.SugaredLogger
}

func NewPublishArtifactsStep(step *enginepb.UnitStep, so output.StageOutput, log *zap.SugaredLogger) PublishArtifactsStep {
	s := step.GetPublishArtifacts()
	return &publishArtifactsStep{
		id:          step.GetId(),
		displayName: step.GetDisplayName(),
		stageOutput: so,
		files:       s.GetFiles(),
		images:      s.GetImages(),
		log:         log,
	}
}

func (s *publishArtifactsStep) Run(ctx context.Context) error {
	st := time.Now()
	arg, err := s.createPublishArtifactArg(ctx)
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
func (s *publishArtifactsStep) createPublishArtifactArg(ctx context.Context) (
	*addonpb.PublishArtifactsRequest, error) {
	newUUID, err := uuid.NewV4()
	if err != nil {
		return nil, errors.Wrap(err, "Failed to generate UUID")
	}
	taskID := &addonpb.TaskId{Id: newUUID.String()}

	resolvedFiles, resolvedImages, err := s.resolveExpressions(ctx)
	if err != nil {
		return nil, err
	}
	return &addonpb.PublishArtifactsRequest{
		TaskId: taskID,
		Files:  resolvedFiles,
		Images: resolvedImages,
		StepId: s.id,
	}, nil
}

// resolveExpressions resolves JEXL expressions & expand home directory present in
// publish artifact step input
func (s *publishArtifactsStep) resolveExpressions(ctx context.Context) (
	[]*addonpb.UploadFile, []*addonpb.BuildPublishImage, error) {
	// JEXL expressions are only present in:
	// 1. filePattern, destination URl & region for upload files
	// 2. dockerfile, context, destination URL
	var exprsToResolve []string
	for _, file := range s.files {
		exprsToResolve = append(exprsToResolve, file.GetFilePattern())
		exprsToResolve = append(exprsToResolve, file.GetDestination().GetDestinationUrl())
		exprsToResolve = append(exprsToResolve, file.GetDestination().GetRegion())
	}
	for _, image := range s.images {
		exprsToResolve = append(exprsToResolve, image.GetDockerFile())
		exprsToResolve = append(exprsToResolve, image.GetContext())
		exprsToResolve = append(exprsToResolve, image.GetDestination().GetDestinationUrl())
	}

	resolvedExprs, err := evaluateJEXL(ctx, exprsToResolve, s.stageOutput, s.log)
	if err != nil {
		return nil, nil, err
	}

	// Resolving home directory & JEXL expression for files and images
	var resolvedFiles []*addonpb.UploadFile
	for _, file := range s.files {
		f, err := s.resolveFile(file, resolvedExprs)
		if err != nil {
			return nil, nil, err
		}
		resolvedFiles = append(resolvedFiles, f)
	}

	var resolvedImages []*addonpb.BuildPublishImage
	for _, image := range s.images {
		i, err := s.resolveImage(image, resolvedExprs)
		if err != nil {
			return nil, nil, err
		}
		resolvedImages = append(resolvedImages, i)
	}
	return resolvedFiles, resolvedImages, nil
}

func (s *publishArtifactsStep) resolveFile(file *addonpb.UploadFile,
	resolvedExprs map[string]string) (*addonpb.UploadFile, error) {
	// Resolve JEXL & home directory for file pattern
	var err error
	filePattern := file.GetFilePattern()
	if val, ok := resolvedExprs[file.GetFilePattern()]; ok {
		filePattern = val
	}
	if file.FilePattern, err = expand(filePattern); err != nil {
		return nil, err
	}

	dst := file.GetDestination()
	if val, ok := resolvedExprs[dst.GetDestinationUrl()]; ok {
		dst.DestinationUrl = val
	}
	if val, ok := resolvedExprs[dst.GetRegion()]; ok {
		dst.Region = val
	}
	file.Destination = dst
	return file, nil
}

func (s *publishArtifactsStep) resolveImage(image *addonpb.BuildPublishImage,
	resolvedExprs map[string]string) (*addonpb.BuildPublishImage, error) {
	// Resolve JEXL & home directory for image docker file
	var err error
	dockerfile := image.GetDockerFile()
	if val, ok := resolvedExprs[dockerfile]; ok {
		dockerfile = val
	}
	if image.DockerFile, err = expand(dockerfile); err != nil {
		return nil, err
	}

	// Resolve JEXL & home directory for image context file
	contextFile := image.GetContext()
	if val, ok := resolvedExprs[contextFile]; ok {
		contextFile = val
	}
	if image.Context, err = expand(contextFile); err != nil {
		return nil, err
	}

	dst := image.GetDestination()
	if val, ok := resolvedExprs[dst.GetDestinationUrl()]; ok {
		dst.DestinationUrl = val
	}
	image.Destination = dst
	return image, nil
}

// expand method expands the filepath to resolve tilde and environment variables
// Resolution of environment variables will be handled by CI manager.
func expand(filepath string) (string, error) {
	path, err := filesystem.ExpandTilde(filepath)
	if err != nil {
		return "", errors.Wrap(err, fmt.Sprintf("failed to expand %s", filepath))
	}
	return path, nil
}
