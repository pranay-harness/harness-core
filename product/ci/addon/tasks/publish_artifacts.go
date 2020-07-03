package tasks

import (
	"context"
	"fmt"
	"os"
	"regexp"
	"time"

	"github.com/pkg/errors"
	"github.com/wings-software/portal/commons/go/lib/filesystem"
	"github.com/wings-software/portal/commons/go/lib/jfrogutils"
	"github.com/wings-software/portal/commons/go/lib/kaniko"
	"github.com/wings-software/portal/commons/go/lib/utils"
	pb "github.com/wings-software/portal/product/ci/addon/proto"
	"go.uber.org/zap"
)

const (
	jfrogDstPattern     = `(.*?)\/artifactory\/(.+)`
	jfrogRTFormat       = `/artifactory/`
	jfrogPathEnv        = "JFROG_PATH" // JFROG CLI path
	userNameEnvFormat   = "USERNAME_"
	passwordEnvFormat   = "PASSWORD_"
	accessKeyEnvFormat  = "ACCESS_KEY_"
	secretKeyEnvFormat  = "SECRET_KEY_"
	secretPathEnvFormat = "SECRET_PATH_"
	endpointEnvFormat   = "ENDPOINT_"
)

var (
	newRegistryClient = kaniko.NewRegistryClient
)

// PublishArtifacts represents an interface to upload files or build docker images and upload them to artifactory.
type PublishArtifacts interface {
	Publish(ctx context.Context, in *pb.PublishArtifactsRequest) error
}

type publishArtifacts struct {
	log *zap.SugaredLogger
}

// NewPublishArtifactsTask implements task to publish artifacts
func NewPublishArtifactsTask(log *zap.SugaredLogger) PublishArtifacts {
	return &publishArtifacts{log}
}

//Publishes the artifacts to destination artifactories
func (p *publishArtifacts) Publish(ctx context.Context, in *pb.PublishArtifactsRequest) error {
	start := time.Now()

	// Basic preliminary validation to ensure that all fields are present. We can either process each request
	// and error out if any argument is invalid or we can do preliminary validation to ensure that
	// the request is mostly correct and then error out if an individual publish fails. We are going with
	// the second approach here.

	err := p.validate(in)
	if err != nil {
		logWarning(p.log, "invalid artifact upload arguments", in.String(), start, err)
		return err
	}

	// Upload files
	for _, file := range in.GetFiles() {
		if file.GetDestination().GetLocationType() != pb.LocationType_JFROG {
			logWarning(p.log, "unsupported artifact upload", file.String(), start, err)
			return fmt.Errorf("unsupported artifact upload")
		}

		if err := p.publishToJfrog(ctx, file.GetFilePattern(), file.GetDestination()); err != nil {
			logWarning(p.log, "failed to publish to jfrog", file.String(), start, err)
			return err
		}
	}

	// Build and publish images
	for _, img := range in.GetImages() {
		fs := filesystem.NewOSFileSystem(p.log)
		destination := img.GetDestination()
		locationType := destination.GetLocationType()
		dockerFilePath := img.GetDockerFile()
		context := img.GetContext()

		var err error
		// Check the registry type and perform publishing
		if locationType == pb.LocationType_GCR {
			err = p.buildPublishToGCR(dockerFilePath, context, destination, fs)
		} else if locationType == pb.LocationType_ECR {
			err = p.buildPublishToECR(dockerFilePath, context, destination, fs)
		} else if locationType == pb.LocationType_DOCKERHUB {
			err = p.buildPublishToDockerHub(dockerFilePath, context, destination, fs)
		} else {
			err = errors.New(fmt.Sprintf("Unsupported location type for image %s", locationType))
		}
		if err != nil {
			logWarning(p.log, "Unable to publish image", img.String(), start, err)
			return err
		}
	}
	p.log.Infow(
		"Successfully published all artifacts",
		"elapsed_time_ms", utils.TimeSince(start),
	)
	return nil
}

//Validates the publish artifact request
func (p *publishArtifacts) validate(in *pb.PublishArtifactsRequest) error {
	if in.GetTaskId().GetId() == "" {
		return errors.New("task id is not set")
	}

	for _, file := range in.GetFiles() {
		err := p.validateFile(file)
		if err != nil {
			return err
		}
	}

	for _, image := range in.GetImages() {
		err := p.validateImage(image)
		if err != nil {
			return err
		}
	}
	return nil
}

func (p *publishArtifacts) buildPublishToDockerHub(dockerFilePath string, context string,
	destination *pb.Destination, fs filesystem.FileSystem) error {
	st := time.Now()

	destinationUrl := destination.GetDestinationUrl()
	connectorID := destination.GetConnector().GetId()

	usernameEnv := fmt.Sprintf("%s%s", userNameEnvFormat, connectorID)
	passwordEnv := fmt.Sprintf("%s%s", passwordEnvFormat, connectorID)
	dockerHubEnv := fmt.Sprintf("%s%s", endpointEnvFormat, connectorID)
	username, ok1 := os.LookupEnv(usernameEnv)
	if !ok1 {
		return errors.New(fmt.Sprintf("No environment variable was set for %s", usernameEnv))
	}
	password, ok2 := os.LookupEnv(passwordEnv)
	if !ok2 {
		return errors.New(fmt.Sprintf("No environment variable was set for %s", passwordEnv))
	}
	endpoint, ok3 := os.LookupEnv(dockerHubEnv)
	if !ok3 {
		p.log.Warnw(fmt.Sprintf("Could not find environment variable %s.", dockerHubEnv))
		endpoint = ""
	}
	c, err := newRegistryClient(p.log, fs, kaniko.WithDockerHubClient(username, password, endpoint))
	if err != nil {
		return err
	}

	if err = c.Publish(dockerFilePath, context, destinationUrl); err != nil {
		p.log.Warnw("Failed to publish image", "docker_file_path", dockerFilePath,
			"context", context, "destination", destination.String(),
			"time_elapsed_ms", utils.TimeSince(st), zap.Error(err))
		return err
	}

	p.log.Infow("Successfully published image", "docker_file_path", dockerFilePath, "context", context,
		"destination", destination.String(), "elapsed_time_ms", utils.TimeSince(st))
	return nil
}

func (p *publishArtifacts) buildPublishToGCR(dockerFilePath, context string,
	destination *pb.Destination, fs filesystem.FileSystem) error {
	st := time.Now()

	destinationUrl := destination.GetDestinationUrl()
	connectorID := destination.GetConnector().GetId()

	gcrSecretPathEnv := fmt.Sprintf("%s%s", secretPathEnvFormat, connectorID)
	secretPath, ok := os.LookupEnv(gcrSecretPathEnv)
	if !ok {
		return errors.New(fmt.Sprintf("No environment variable was set for %s", gcrSecretPathEnv))
	}
	c, err := newRegistryClient(p.log, fs, kaniko.WithGcrClient(secretPath))
	if err != nil {
		return err
	}

	if err = c.Publish(dockerFilePath, context, destinationUrl); err != nil {
		p.log.Warnw("Failed to publish image", "docker_file_path", dockerFilePath,
			"context", context, "destination", destination.String(),
			"time_elapsed_ms", utils.TimeSince(st), zap.Error(err))
		return err
	}

	p.log.Infow("Successfully published image", "docker_file_path", dockerFilePath, "context", context,
		"destination", destination.String(), "elapsed_time_ms", utils.TimeSince(st))
	return nil
}

func (p *publishArtifacts) buildPublishToECR(dockerFilePath, context string,
	destination *pb.Destination, fs filesystem.FileSystem) error {
	st := time.Now()

	destinationUrl := destination.GetDestinationUrl()
	connectorID := destination.GetConnector().GetId()

	accessKeyEnv := fmt.Sprintf("%s%s", accessKeyEnvFormat, connectorID)
	secretKeyEnv := fmt.Sprintf("%s%s", secretKeyEnvFormat, connectorID)
	accessKey, ok1 := os.LookupEnv(accessKeyEnv)
	if !ok1 {
		return errors.New(fmt.Sprintf("No environment variable was set for %s", accessKeyEnv))
	}
	secretKey, ok2 := os.LookupEnv(secretKeyEnv)
	if !ok2 {
		return errors.New(fmt.Sprintf("No environment variable was set for %s", secretKeyEnv))
	}
	c, err := newRegistryClient(p.log, fs, kaniko.WithEcrClient(accessKey, secretKey))

	if err != nil {
		return err
	}

	if err = c.Publish(dockerFilePath, context, destinationUrl); err != nil {
		p.log.Warnw("Failed to publish image", "docker_file_path", dockerFilePath,
			"context", context, "destination", destination.String(),
			"time_elapsed_ms", utils.TimeSince(st), zap.Error(err))
		return err
	}

	p.log.Infow("Successfully published image", "docker_file_path", dockerFilePath, "context", context,
		"destination", destination.String(), "elapsed_time_ms", utils.TimeSince(st))
	return nil
}

//Publishes the artifacts to JFROG artifactory.
func (p *publishArtifacts) publishToJfrog(ctx context.Context, filePattern string, destination *pb.Destination) error {
	connector := destination.GetConnector()
	connectorID := connector.GetId()
	url := destination.GetDestinationUrl()
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

	jfrogPath, ok := os.LookupEnv(jfrogPathEnv)
	if !ok {
		return fmt.Errorf(fmt.Sprintf("jfrog cli path not set for environment variable %s", jfrogPathEnv))
	}

	r, err := regexp.Compile(jfrogDstPattern)
	if err != nil {
		return fmt.Errorf(fmt.Sprintf("invalid regex pattern %s", jfrogDstPattern))
	}

	matches := r.FindStringSubmatch(url)
	if matches == nil || len(matches) != 3 {
		return fmt.Errorf(fmt.Sprintf("invalid destination url, %s", destination.GetDestinationUrl()))
	}

	rtURL := fmt.Sprintf("%s%s", matches[1], jfrogRTFormat)
	repoPath := matches[2]
	c, err := jfrogutils.NewArtifactoryClient(jfrogPath, p.log, jfrogutils.WithArtifactoryClientBasicAuth(userName, pwd))
	if err != nil {
		return err
	}

	return c.Upload(ctx, filePattern, repoPath, rtURL)
}

// Validate a single file.
func (p *publishArtifacts) validateFile(file *pb.UploadFile) error {
	if file.GetFilePattern() == "" {
		return errors.New("file pattern is not set")
	}
	destination := file.GetDestination()
	// Validate auth type for the specified file
	if destination.GetLocationType() == pb.LocationType_JFROG {
		if destination.GetConnector().GetAuth() != pb.AuthType_BASIC_AUTH {
			return errors.New(fmt.Sprintf("Unsupported authorization method for JFROG: %s", file.String()))
		}
	}
	if err := p.validateDestination(destination); err != nil {
		return err
	}
	return nil
}

// Validate a single image. Path to the dockerfile and the context should be set.
func (p *publishArtifacts) validateImage(image *pb.BuildPublishImage) error {
	if image.GetDockerFile() == "" {
		return errors.New("Docker file path is not set")
	}
	if image.GetContext() == "" {
		return errors.New("Docker file context is not set")
	}
	destination := image.GetDestination()
	// Validate auth type for the specified registry
	if destination.GetLocationType() == pb.LocationType_GCR {
		if destination.GetConnector().GetAuth() != pb.AuthType_SECRET_FILE {
			return errors.New(fmt.Sprintf("Unsupported authorization method for GCR: %s", image.String()))
		}
	} else if destination.GetLocationType() == pb.LocationType_ECR {
		if destination.GetConnector().GetAuth() != pb.AuthType_ACCESS_KEY {
			return errors.New(fmt.Sprintf("Unsupported authorization method for ECR: %s", image.String()))
		}
	} else if destination.GetLocationType() == pb.LocationType_DOCKERHUB {
		if destination.GetConnector().GetAuth() != pb.AuthType_BASIC_AUTH {
			return errors.New(fmt.Sprintf("Unsupported authorization method for DockerHub: %s", image.String()))
		}
	}
	if err := p.validateDestination(destination); err != nil {
		return err
	}
	return nil
}

// Validate the destination. The destination URL should be set and the connector
// should be valid.
func (p *publishArtifacts) validateDestination(in *pb.Destination) error {
	if in.GetDestinationUrl() == "" {
		return fmt.Errorf("artifact destination url is not set")
	}
	if in.GetConnector().GetId() == "" {
		return fmt.Errorf("connector ID is not set")
	}
	if in.GetLocationType() == pb.LocationType_UNKNOWN {
		return errors.New(fmt.Sprintf("Unsupported location type"))
	}
	return nil
}

func logWarning(log *zap.SugaredLogger, warnMsg, args string, startTime time.Time, err error) {
	log.Warnw(
		warnMsg,
		"arguments", args,
		"elapsed_time_ms", utils.TimeSince(startTime),
		zap.Error(err),
	)
}
