package tasks

import (
	"context"
	"fmt"
	"os"
	"regexp"
	"time"

	"github.com/wings-software/portal/commons/go/lib/jfrogutils"
	"github.com/wings-software/portal/commons/go/lib/utils"
	pb "github.com/wings-software/portal/product/ci/addon/proto"
	"go.uber.org/zap"
)

const (
	jfrogDstPattern   = `(.*?)\/artifactory\/(.+)`
	jfrogRTFormat     = `/artifactory/`
	userNameEnvFormat = "USERNAME_"
	passwordEnvFormat = "PASSWORD_"
)

// PublishArtifact represents an interface to upload files or build docker images and upload them to artifactory.
type PublishArtifact interface {
	Publish(ctx context.Context, in *pb.PublishArtifactsRequest) error
}

type publishArtifact struct {
	log *zap.SugaredLogger
}

// NewPublishArtifactTask implements task to publish artifacts
func NewPublishArtifactTask(log *zap.SugaredLogger) PublishArtifact {
	return &publishArtifact{log}
}

//Publishes the artifacts to destination artifactories
func (p *publishArtifact) Publish(ctx context.Context, in *pb.PublishArtifactsRequest) error {
	start := time.Now()
	err := p.validate(in)
	if err != nil {
		logWarning(p.log, "invalid artifact upload arguments", in.String(), start, err)
		return err
	}

	for _, file := range in.GetFiles() {
		if file.GetDestination().GetArtifactType() != pb.ArtifactType_JFROG {
			logWarning(p.log, "unsupported artifact upload", file.String(), start, err)
			return fmt.Errorf("unsupported artifact upload")
		}

		if err := p.publishToJfrog(ctx, file.GetFilePattern(), file.GetDestination()); err != nil {
			logWarning(p.log, "failed to publish to jfrog", file.String(), start, err)
			return err
		}
	}

	p.log.Infow(
		"Successfully published artifacts",
		"elapsed_time_ms", utils.TimeSince(start),
	)
	return nil
}

// Validates destination artifact request
func (p *publishArtifact) validateDstArtifact(in *pb.DestinationArtifact) error {
	if in.GetDestination() == "" {
		return fmt.Errorf("artifact destination url is not set")
	}
	if in.GetConnector().GetId() == "" {
		return fmt.Errorf("Connector ID is not set")
	}
	return nil
}

//Validates the publish artifact request
func (p *publishArtifact) validate(in *pb.PublishArtifactsRequest) error {
	if in.GetTaskId().GetId() == "" {
		return fmt.Errorf("task id is not set")
	}

	for _, file := range in.GetFiles() {
		if file.GetFilePattern() == "" {
			return fmt.Errorf("file pattern is not set")
		}
		if err := p.validateDstArtifact(file.GetDestination()); err != nil {
			return err
		}
	}

	for _, image := range in.GetImages() {
		i := image.GetImage()
		if i.GetDockerFile() == "" {
			return fmt.Errorf("Docker file is not set")
		}
		if i.GetImage() == "" {
			return fmt.Errorf("Image name for docker image is not set")
		}
		if i.GetTag() == "" {
			return fmt.Errorf("Image tag for docker image is not set")
		}
		if err := p.validateDstArtifact(image.GetDestination()); err != nil {
			return err
		}
	}
	return nil
}

//Publishes the artifacts to JFROG artifactory.
func (p *publishArtifact) publishToJfrog(ctx context.Context, filePattern string, destination *pb.DestinationArtifact) error {
	connector := destination.GetConnector()
	if connector.GetAuth() != pb.AuthType_BASIC_AUTH {
		return fmt.Errorf("unsupported auth type")
	}

	connectorID := connector.GetId()
	userNameEnv := fmt.Sprintf("%s%s", userNameEnvFormat, connectorID)
	userName, ok := os.LookupEnv(userNameEnv)
	if !ok {
		return fmt.Errorf(fmt.Sprintf("username not set for environment variable %s", userNameEnv))
	}

	pwdEnv := fmt.Sprintf("%s%s", passwordEnvFormat, connectorID)
	pwd, ok := os.LookupEnv(pwdEnv)
	if !ok {
		return fmt.Errorf(fmt.Sprintf("password not set for environment variable %s", pwdEnv))
	}

	r, err := regexp.Compile(jfrogDstPattern)
	if err != nil {
		return fmt.Errorf(fmt.Sprintf("invalid regex pattern %s", jfrogDstPattern))
	}

	matches := r.FindStringSubmatch(destination.GetDestination())
	if matches == nil || len(matches) != 3 {
		return fmt.Errorf(fmt.Sprintf("invalid destination url, %s", destination.Destination))
	}

	rtURL := fmt.Sprintf("%s%s", matches[1], jfrogRTFormat)
	repoPath := matches[2]
	c, err := jfrogutils.NewArtifactoryClient(p.log, jfrogutils.WithArtifactoryClientBasicAuth(userName, pwd))
	if err != nil {
		return err
	}

	return c.Upload(ctx, filePattern, repoPath, rtURL)
}

func logWarning(log *zap.SugaredLogger, warnMsg, args string, startTime time.Time, err error) {
	log.Warnw(
		warnMsg,
		"arguments", args,
		"elapsed_time_ms", utils.TimeSince(startTime),
		zap.Error(err),
	)
}
