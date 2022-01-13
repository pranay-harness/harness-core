// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package steps

// Proxy the request to CI addon

import (
	"context"
	"fmt"
	"time"

	"github.com/pkg/errors"

	"github.com/wings-software/portal/commons/go/lib/filesystem"
	"github.com/wings-software/portal/commons/go/lib/utils"
	"github.com/wings-software/portal/product/ci/engine/legacy/artifacts"
	"github.com/wings-software/portal/product/ci/engine/output"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

var (
	artifactPublishTask = artifacts.NewPublishArtifactsTask
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
	files       []*pb.UploadFile
	images      []*pb.BuildPublishImage
	log         *zap.SugaredLogger
}

// NewPublishArtifactsStep implements execution of publish artifacts step
func NewPublishArtifactsStep(step *pb.UnitStep, so output.StageOutput, log *zap.SugaredLogger) PublishArtifactsStep {
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
	resolvedFiles, resolvedImages, err := s.resolveExpressions(ctx)
	if err != nil {
		return err
	}

	err = artifactPublishTask(s.log).Publish(ctx, resolvedFiles, resolvedImages)
	if err != nil {
		s.log.Errorw("Failed to publish artifacts", "elapsed_time_ms", utils.TimeSince(st), zap.Error(err))
		return err
	}
	s.log.Infow("Successfully published artifacts", "elapsed_time_ms", utils.TimeSince(st))
	return nil
}

// resolveExpressions resolves JEXL expressions & expand home directory present in
// publish artifact step input
func (s *publishArtifactsStep) resolveExpressions(ctx context.Context) (
	[]*pb.UploadFile, []*pb.BuildPublishImage, error) {
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

	resolvedExprs, err := evaluateJEXL(ctx, s.id, exprsToResolve, s.stageOutput, false, s.log)
	if err != nil {
		return nil, nil, err
	}

	// Resolving home directory & JEXL expression for files and images
	var resolvedFiles []*pb.UploadFile
	for _, file := range s.files {
		f, err := s.resolveFile(file, resolvedExprs)
		if err != nil {
			return nil, nil, err
		}
		resolvedFiles = append(resolvedFiles, f)
	}

	var resolvedImages []*pb.BuildPublishImage
	for _, image := range s.images {
		i, err := s.resolveImage(image, resolvedExprs)
		if err != nil {
			return nil, nil, err
		}
		resolvedImages = append(resolvedImages, i)
	}
	return resolvedFiles, resolvedImages, nil
}

func (s *publishArtifactsStep) resolveFile(file *pb.UploadFile,
	resolvedExprs map[string]string) (*pb.UploadFile, error) {
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

func (s *publishArtifactsStep) resolveImage(image *pb.BuildPublishImage,
	resolvedExprs map[string]string) (*pb.BuildPublishImage, error) {
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
