// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package artifacts

import (
	"context"
	"fmt"
	"os"
	"regexp"
	"time"

	"github.com/pkg/errors"
	"github.com/wings-software/portal/commons/go/lib/awsutils"
	"github.com/wings-software/portal/commons/go/lib/filesystem"
	"github.com/wings-software/portal/commons/go/lib/jfrogutils"
	"github.com/wings-software/portal/commons/go/lib/kaniko"
	"github.com/wings-software/portal/commons/go/lib/utils"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

const (
	jfrogDstPattern         = `(.*?)\/artifactory\/(.+)`
	jfrogDstPatternMatchLen = 3
	jfrogRTFormat           = `/artifactory/`
	jfrogPathEnv            = "JFROG_PATH" // JFROG CLI path
	s3DstPattern            = `s3:\/\/(.*?)\/(.+)`
	s3DstPatternMatchLen    = 3
	defaultS3Token          = ""
	enableTLSV2             = true
	userNameEnvFormat       = "USERNAME_"
	passwordEnvFormat       = "PASSWORD_"
	accessKeyEnvFormat      = "ACCESS_KEY_"
	secretKeyEnvFormat      = "SECRET_KEY_"
	secretPathEnvFormat     = "SECRET_PATH_"
	endpointEnvFormat       = "ENDPOINT_"
)

var (
	newRegistryClient = kaniko.NewRegistryClient
)

// PublishArtifacts represents an interface to upload files or build docker images and upload them to artifactory.
type PublishArtifacts interface {
	Publish(ctx context.Context, files []*pb.UploadFile, images []*pb.BuildPublishImage) error
}

type publishArtifacts struct {
	log *zap.SugaredLogger
}

// NewPublishArtifactsTask implements task to publish artifacts
func NewPublishArtifactsTask(log *zap.SugaredLogger) PublishArtifacts {
	return &publishArtifacts{log}
}

//Publishes the artifacts to destination artifactories
func (p *publishArtifacts) Publish(ctx context.Context, files []*pb.UploadFile, images []*pb.BuildPublishImage) error {
	start := time.Now()

	// Basic preliminary validation to ensure that all fields are present. We can either process each request
	// and error out if any argument is invalid or we can do preliminary validation to ensure that
	// the request is mostly correct and then error out if an individual publish fails. We are going with
	// the second approach here.
	p.log.Infow("Starting publishing of artifacts")
	err := validatePublishRequest(files, images)
	if err != nil {
		p.log.Errorw("Invalid artifact upload arguments", "elapsed_time_ms", utils.TimeSince(start), zap.Error(err))
		return err
	}

	// Upload files
	for _, file := range files {
		switch file.GetDestination().GetLocationType() {
		case pb.LocationType_JFROG:
			if err := p.publishToJfrog(ctx, file.GetFilePattern(), file.GetDestination()); err != nil {
				logError(p.log, "failed to publish to jfrog", file.String(), start, err)
				return err
			}
		case pb.LocationType_S3:
			if err := p.publishToS3(ctx, file.GetFilePattern(), file.GetDestination()); err != nil {
				logError(p.log, "failed to publish to s3", file.String(), start, err)
				return err
			}
		default:
			logError(p.log, "unsupported artifact upload", file.String(), start, err)
			return fmt.Errorf("unsupported artifact upload")
		}
	}

	// Build and publish images
	for _, img := range images {
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
			logError(p.log, "Unable to publish image", img.String(), start, err)
			return err
		}
	}

	p.log.Infow(
		"Successfully published all artifacts")
	return nil
}

func (p *publishArtifacts) buildPublishToDockerHub(dockerFilePath string, context string,
	destination *pb.Destination, fs filesystem.FileSystem) error {
	st := time.Now()

	destinationURL := destination.GetDestinationUrl()
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

	if err = c.Publish(dockerFilePath, context, destinationURL); err != nil {
		p.log.Errorw("Failed to publish image", "docker_file_path", dockerFilePath,
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

	destinationURL := destination.GetDestinationUrl()
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

	if err = c.Publish(dockerFilePath, context, destinationURL); err != nil {
		p.log.Errorw("Failed to publish image", "docker_file_path", dockerFilePath,
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

	destinationURL := destination.GetDestinationUrl()
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

	if err = c.Publish(dockerFilePath, context, destinationURL); err != nil {
		p.log.Errorw("Failed to publish image", "docker_file_path", dockerFilePath,
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
	if matches == nil || len(matches) != jfrogDstPatternMatchLen {
		return fmt.Errorf(fmt.Sprintf("invalid destination url, %s", url))
	}

	rtURL := fmt.Sprintf("%s%s", matches[1], jfrogRTFormat)
	repoPath := matches[2]
	c, err := jfrogutils.NewArtifactoryClient(jfrogPath, p.log, jfrogutils.WithArtifactoryClientBasicAuth(userName, pwd))
	if err != nil {
		return err
	}

	return c.Upload(ctx, filePattern, repoPath, rtURL)
}

//Publishes the artifacts to S3 artifactory.
func (p *publishArtifacts) publishToS3(ctx context.Context, filePattern string, destination *pb.Destination) error {
	connector := destination.GetConnector()
	connectorID := connector.GetId()
	url := destination.GetDestinationUrl()
	region := destination.GetRegion()

	accessKeyEnv := fmt.Sprintf("%s%s", accessKeyEnvFormat, connectorID)
	accessKey, ok := os.LookupEnv(accessKeyEnv)
	if !ok {
		return errors.New(fmt.Sprintf("access key not set for environment variable %s", accessKeyEnv))
	}

	secretKeyEnv := fmt.Sprintf("%s%s", secretKeyEnvFormat, connectorID)
	secretKey, ok := os.LookupEnv(secretKeyEnv)
	if !ok {
		return errors.New(fmt.Sprintf("secret key not set for environment variable %s", secretKey))
	}

	r, err := regexp.Compile(s3DstPattern)
	if err != nil {
		return errors.New(fmt.Sprintf("invalid regex pattern %s", s3DstPattern))
	}
	matches := r.FindStringSubmatch(url)
	if matches == nil || len(matches) != s3DstPatternMatchLen {
		return errors.New(fmt.Sprintf("invalid destination url, %s", url))
	}

	bucket := matches[1]
	key := matches[2]
	session, err := awsutils.NewS3Session(accessKey, secretKey, defaultS3Token, region, enableTLSV2)
	if err != nil {
		return errors.Wrap(err, "failed to create s3 session")
	}

	fs := filesystem.NewOSFileSystem(p.log)
	client := awsutils.NewS3UploadClient(session)
	uploader := awsutils.NewS3Uploader(bucket, client, fs, p.log)
	_, _, err = uploader.UploadFileWithContext(ctx, key, filePattern)
	return err
}

func logError(log *zap.SugaredLogger, errMsg, args string, startTime time.Time, err error) {
	log.Errorw(
		errMsg,
		"arguments", args,
		"elapsed_time_ms", utils.TimeSince(startTime),
		zap.Error(err),
	)
}
