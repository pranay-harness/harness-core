// Copyright 2020 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package kaniko

import (
	"bytes"
	"context"
	"encoding/base64"
	"fmt"
	"io"
	"os/exec"
	"strings"
	"time"

	"github.com/pkg/errors"
	"github.com/wings-software/portal/commons/go/lib/filesystem"
	"github.com/wings-software/portal/commons/go/lib/utils"
	"go.uber.org/zap"
)

//go:generate mockgen -source publisher.go -package kaniko_mock -destination mocks/publisher_mock.go

// /kaniko/executor --dockerfile=/step/harness/Dockerifle_1 --context=dir:///step/harness --destination=vistaarjuneja/kaniko-test:v1.2
const (
	// Timeout constants
	defaultTimeoutSecs int64 = 1200 // 20 minutes timeout for building and pushing an image to a registry

	// Docker config file path
	dockerConfigFilePath string = "/kaniko/.docker/config.json"

	// Path to kaniko executor
	kanikoExecutorPath string = "/kaniko/executor"

	// DockerHub specific variables
	dockerDefaultEndpoint string = "https://index.docker.io/v1/"

	// GCR specific variables
	gcrEnvVariable string = "GOOGLE_APPLICATION_CREDENTIALS"

	// ECR specific variables
	ecrAccessKeyEnv string = "AWS_ACCESS_KEY_ID"
	ecrSecretKeyEnv string = "AWS_SECRET_ACCESS_KEY"

	// Supported registry types
	DOCKERHUB string = "dockerhub"
	GCR       string = "gcr"
	ECR       string = "ecr"
)

var execCommandWithContext = exec.CommandContext

type dockerHubAuth struct {
	username string
	password string
	endpoint string
}

type ecrAuth struct {
	accessKey string
	secretKey string
}

type gcrAuth struct {
	secretPath string
}

type registryAuth struct {
	dockerHub dockerHubAuth
	gcr       gcrAuth
	ecr       ecrAuth
}

type registryClient struct {
	registryType string
	auth         registryAuth
	timeoutSecs  int64
	log          *zap.SugaredLogger
	fs           filesystem.FileSystem
}

type Registry interface {
	Publish(filename, fileContext, destination string) error // Method to publish an image to a registry
}

type RegistryClientOption func(*registryClient)

// ----------------------------------------------------------------------------------------

// DockerHub requires username and password to the registry
func WithDockerHubClient(username, password, endpoint string) RegistryClientOption {
	return func(c *registryClient) {
		c.registryType = DOCKERHUB
		c.auth.dockerHub.username = username
		c.auth.dockerHub.password = password
		c.auth.dockerHub.endpoint = endpoint
		if c.auth.dockerHub.endpoint == "" {
			c.auth.dockerHub.endpoint = dockerDefaultEndpoint
		}
	}
}

// GCR requires secretPath containing service account details in JSON
func WithGcrClient(secretPath string) RegistryClientOption {
	return func(c *registryClient) {
		c.registryType = GCR
		c.auth.gcr.secretPath = secretPath
	}
}

// ECR requires access key id and secret key id
func WithEcrClient(accessKey, secretKey string) RegistryClientOption {
	return func(c *registryClient) {
		c.registryType = ECR
		c.auth.ecr.accessKey = accessKey
		c.auth.ecr.secretKey = secretKey
	}
}

// Timeout for building & pushing the image. Default value is 20 minutes.
// This might need to be larger for very large images.
func WithRegistryClientTimeoutSecs(timeoutSecs int64) RegistryClientOption {
	return func(c *registryClient) {
		c.timeoutSecs = timeoutSecs
	}
}

// ----------------------------------------------------------------------------------------

func NewRegistryClient(log *zap.SugaredLogger, fs filesystem.FileSystem, opts ...RegistryClientOption) (Registry, error) {
	c := &registryClient{
		log:         log,
		fs:          fs,
		timeoutSecs: defaultTimeoutSecs,
	}
	for _, opt := range opts {
		opt(c)
	}

	// An option needs to be provided for one registry to make the connection
	if c.registryType == "" {
		return nil, errors.New("Please provide registry information for one of DockerHub, GCR or ECR")
	}
	return c, nil
}

func createKanikoArg(filePath, fileContext, destination string) []string {
	args := []string{fmt.Sprintf("--dockerfile=%s", filePath),
		fmt.Sprintf("--context=dir://%s", fileContext),
		fmt.Sprintf("--destination=%s", destination),
		fmt.Sprintf("--cleanup")}
	return args
}

// Create the docker config file for Dockerhub
func (c *registryClient) createDockerHubConfig(dockerConfigPath string) error {
	authBytes := []byte(fmt.Sprintf("%s:%s", c.auth.dockerHub.username, c.auth.dockerHub.password))
	encodedString := base64.StdEncoding.EncodeToString(authBytes)
	jsonBytes := []byte(fmt.Sprintf(`{"auths": {"%s": {"auth": "%s"}}}`, c.auth.dockerHub.endpoint, encodedString))
	err := c.fs.WriteFile(dockerConfigPath, func(at io.WriterAt) error {
		_, err := at.WriteAt(jsonBytes, 0)
		return err
	})
	if err != nil {
		logCredentialWriteFailure(c.log, "Errored out writing creds to docker config file", dockerConfigPath, err)
		return err
	}
	return nil
}

// Create the docker config file for ECR
func (c *registryClient) createECRConfig(destinationUrl, dockerConfigPath string) error {
	jsonBytes := []byte(fmt.Sprintf(`{"credStore": "ecr-login", "credHelpers": {"%s": "ecr-login"}}`, destinationUrl))
	err := c.fs.WriteFile(dockerConfigPath, func(at io.WriterAt) error {
		_, err := at.WriteAt(jsonBytes, 0)
		return err
	})
	if err != nil {
		logCredentialWriteFailure(c.log, "Errored out writing creds to docker config file", dockerConfigPath, err)
		return err
	}
	return nil
}

func (c *registryClient) dockerHubSetup() error {
	if err := c.createDockerHubConfig(dockerConfigFilePath); err != nil {
		return errors.Wrap(err, "Could not create DockerHub config file")
	}
	return nil
}

func (c *registryClient) gcrSetup() error {
	if err := c.fs.Setenv(gcrEnvVariable, c.auth.gcr.secretPath); err != nil {
		return errors.Wrap(err, "Could not set GCR environment variable")
	}
	return nil
}

func (c *registryClient) ecrSetup(destination string) error {
	// Setup ECR keys
	if err := c.fs.Setenv(ecrAccessKeyEnv, c.auth.ecr.accessKey); err != nil {
		return errors.Wrap(err, "Could not set ECR access key")
	}
	if err := c.fs.Setenv(ecrSecretKeyEnv, c.auth.ecr.secretKey); err != nil {
		return errors.Wrap(err, "Could not set ECR secret key")
	}
	// Remove tags from the destination
	destinationSplit := strings.Split(destination, "/")
	destinationUrl := destinationSplit[0]

	// Repository names in ECR cannot contain `/`. The format is: <repository_info>/<repository_name>:tags
	// They also cannot be empty
	if len(destinationSplit) != 2 || len(destinationUrl) == 0 {
		return errors.New(fmt.Sprintf("Could not parse destination %s", destination))
	}

	if err := c.createECRConfig(destinationUrl, dockerConfigFilePath); err != nil {
		return errors.Wrap(err, "Could not create ECR config file")
	}
	return nil
}

func (c *registryClient) dockerHubCleanup() error {
	if err := c.fs.Remove(dockerConfigFilePath); err != nil {
		return errors.Wrap(err, "Unable to remove docker config file")
	}
	return nil
}

func (c *registryClient) gcrCleanup() error {
	if err := c.fs.Unsetenv(gcrEnvVariable); err != nil {
		return errors.Wrap(err, "Could not unset GCR credential file path")
	}

	if err := c.fs.Remove(dockerConfigFilePath); err != nil {
		return errors.Wrap(err, "Could not remove GCR config file")
	}
	return nil
}

func (c *registryClient) ecrCleanup() error {
	if err := c.fs.Unsetenv(ecrSecretKeyEnv); err != nil {
		return errors.Wrap(err, "Could not unset ECR secret key")
	}
	if err := c.fs.Unsetenv(ecrAccessKeyEnv); err != nil {
		return errors.Wrap(err, "Could not unset ECR access key")
	}
	if err := c.fs.Remove(dockerConfigFilePath); err != nil {
		return errors.Wrap(err, "Could not remove ECR config file")
	}
	return nil
}

func (c *registryClient) Publish(filePath, fileContext, destination string) error {
	start := time.Now()
	// TODO: Honour existing context
	ctx, cancel := context.WithTimeout(context.Background(), time.Duration(c.timeoutSecs)*time.Second)
	defer cancel()

	var err error

	// Cleanup
	defer func() {
		// Do Cleanup
		switch c.registryType {
		case DOCKERHUB:
			err = c.dockerHubCleanup()
		case GCR:
			err = c.gcrCleanup()
		case ECR:
			err = c.ecrCleanup()
		default:
			err = errors.New(fmt.Sprintf("No implementation for registry type %s", c.registryType))
		}
		if err != nil {
			c.log.Warnw("Unable to perform cleanup", zap.Error(err))
		}
	}()

	// Setup
	switch c.registryType {
	case DOCKERHUB:
		err = c.dockerHubSetup()
	case GCR:
		err = c.gcrSetup()
	case ECR:
		err = c.ecrSetup(destination) // ECR requires the destination to setup credentials
	default:
		err = errors.New(fmt.Sprintf("No implementation for registry type %s", c.registryType))
	}

	if err != nil {
		c.log.Warnw("Unable to perform setup", zap.Error(err))
		return err
	}

	// Execute command
	var out bytes.Buffer
	var stderr bytes.Buffer
	args := createKanikoArg(filePath, fileContext, destination)
	cmd := execCommandWithContext(ctx, kanikoExecutorPath, args...)
	cmd.Stdout = &out
	cmd.Stderr = &stderr

	err = cmd.Run()
	if ctxErr := ctx.Err(); ctxErr == context.DeadlineExceeded {
		logCommandExecError(c.log, "Timed out while trying to build and push image",
			out.String(), stderr.String(), args, start, err)
		return ctxErr
	}
	if err != nil {
		logCommandExecError(c.log, "Errored out while trying to build and push image",
			out.String(), stderr.String(), args, start, err)
		return err
	}

	c.log.Infow("Successfully built and published images to registry",
		"arguments", args,
		"stdout", out.String(),
		"elapsed_time_ms", utils.TimeSince(start))
	return nil
}

func logCommandExecError(log *zap.SugaredLogger, errorMsg, stdout, stderr string, args []string, startTime time.Time, err error) {
	log.Errorw(
		errorMsg,
		"stdout", stdout,
		"stderr", stderr,
		"arguments", args,
		"elapsed_time_ms", utils.TimeSince(startTime),
		zap.Error(err),
	)
}

func logCredentialWriteFailure(log *zap.SugaredLogger, errorMsg, filePath string, err error) {
	log.Errorw(
		errorMsg,
		"file_path", filePath,
		zap.Error(err),
	)
}
