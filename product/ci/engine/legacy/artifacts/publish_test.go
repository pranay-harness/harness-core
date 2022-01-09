// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package artifacts

import (
	"context"
	"os"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/pkg/errors"
	"github.com/stretchr/testify/assert"
	"go.uber.org/zap"

	"github.com/wings-software/portal/commons/go/lib/filesystem"
	"github.com/wings-software/portal/commons/go/lib/kaniko"
	kaniko_mock "github.com/wings-software/portal/commons/go/lib/kaniko/mocks"
	"github.com/wings-software/portal/commons/go/lib/logs"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
)

func Test_validatePublishArtifactRequest(t *testing.T) {
	filePattern := "/a/b/c"
	destinationURL := "file://a/b/c"
	connectorID := "testConnector"
	dockerFilePath := "~/DockerFile"
	context := "~/"

	tests := []struct {
		name        string
		files       []*pb.UploadFile
		images      []*pb.BuildPublishImage
		expectedErr bool
	}{
		{
			name:        "file pattern not set",
			files:       []*pb.UploadFile{{}},
			expectedErr: true,
		},
		{
			name: "destination is not set",
			files: []*pb.UploadFile{
				{FilePattern: filePattern},
			},
			expectedErr: true,
		},
		{
			name: "Connector is not set",
			files: []*pb.UploadFile{
				{
					FilePattern: filePattern,
					Destination: &pb.Destination{
						DestinationUrl: destinationURL,
					},
				},
			},
			expectedErr: true,
		},
		{
			name: "valid request without location type",
			files: []*pb.UploadFile{
				{
					FilePattern: filePattern,
					Destination: &pb.Destination{
						DestinationUrl: destinationURL,
						Connector: &pb.Connector{
							Id: connectorID,
						},
					},
				},
			},
			expectedErr: true,
		},
		{
			name: "valid request with files & images",
			files: []*pb.UploadFile{
				{
					FilePattern: filePattern,
					Destination: &pb.Destination{
						DestinationUrl: destinationURL,
						Connector: &pb.Connector{
							Id: connectorID,
						},
						LocationType: pb.LocationType_JFROG,
					},
				},
			},
			images: []*pb.BuildPublishImage{
				{DockerFile: dockerFilePath,
					Context: context,
					Destination: &pb.Destination{
						DestinationUrl: destinationURL,
						Connector: &pb.Connector{
							Id:   connectorID,
							Auth: pb.AuthType_BASIC_AUTH,
						},
						LocationType: pb.LocationType_DOCKERHUB},
				},
			},
			expectedErr: false,
		},
		{
			name:        "empty image list",
			images:      []*pb.BuildPublishImage{{}},
			expectedErr: true,
		},
		{
			name: "context is not set",
			images: []*pb.BuildPublishImage{
				{
					DockerFile: dockerFilePath,
				},
			},
			expectedErr: true,
		},
		{
			name: "dockerfile is not set",
			images: []*pb.BuildPublishImage{
				{
					Context: dockerFilePath,
				},
			},
			expectedErr: true,
		},
		{
			name: "jfrog with invalid auth type",
			files: []*pb.UploadFile{
				{
					FilePattern: filePattern,
					Destination: &pb.Destination{
						DestinationUrl: destinationURL,
						Connector: &pb.Connector{
							Id:   connectorID,
							Auth: pb.AuthType_SECRET_FILE,
						},
						LocationType: pb.LocationType_JFROG},
				},
			},
			expectedErr: true,
		},
		{
			name: "s3 with invalid auth type",
			files: []*pb.UploadFile{
				{
					FilePattern: filePattern,
					Destination: &pb.Destination{
						DestinationUrl: destinationURL,
						Connector: &pb.Connector{
							Id:   connectorID,
							Auth: pb.AuthType_SECRET_FILE,
						},
						LocationType: pb.LocationType_S3},
				},
			},
			expectedErr: true,
		},
		{
			name: "s3 with unset region",
			files: []*pb.UploadFile{
				{
					FilePattern: filePattern,
					Destination: &pb.Destination{
						DestinationUrl: destinationURL,
						Connector: &pb.Connector{
							Id:   connectorID,
							Auth: pb.AuthType_ACCESS_KEY,
						},
						LocationType: pb.LocationType_S3},
				},
			},
			expectedErr: true,
		},
		{
			name: "gcr with invalid auth type",
			images: []*pb.BuildPublishImage{
				{DockerFile: dockerFilePath,
					Context: context,
					Destination: &pb.Destination{
						DestinationUrl: destinationURL,
						Connector: &pb.Connector{
							Id:   connectorID,
							Auth: pb.AuthType_BASIC_AUTH,
						},
						LocationType: pb.LocationType_GCR},
				},
			},
			expectedErr: true,
		},
		{
			name: "ecr with invalid auth type",
			images: []*pb.BuildPublishImage{
				{DockerFile: dockerFilePath,
					Context: context,
					Destination: &pb.Destination{
						DestinationUrl: destinationURL,
						Connector: &pb.Connector{
							Id:   connectorID,
							Auth: pb.AuthType_SECRET_FILE,
						},
						LocationType: pb.LocationType_ECR},
				},
			},
			expectedErr: true,
		},
		{
			name: "dockerhub with invalid auth type",
			images: []*pb.BuildPublishImage{
				{DockerFile: dockerFilePath,
					Context: context,
					Destination: &pb.Destination{
						DestinationUrl: destinationURL,
						Connector: &pb.Connector{
							Id:   connectorID,
							Auth: pb.AuthType_SECRET_FILE,
						},
						LocationType: pb.LocationType_DOCKERHUB},
				},
			},
			expectedErr: true,
		},
	}
	for _, tc := range tests {
		got := validatePublishRequest(tc.files, tc.images)
		if tc.expectedErr == (got == nil) {
			t.Fatalf("%s: expected error: %v, got: %v", tc.name, tc.expectedErr, got)
		}
	}
}

func Test_publishToJfrog_WithError(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	p := &publishArtifacts{
		log: log.Sugar(),
	}
	filePattern := "/a/b/c"
	destinationURL := "https://harness.jfrog.io/artifactory/pcf"
	connectorID := "jfrogConnector"
	jfrogUsrName := "admin"
	jfrogPwd := "admin"
	jfrogPath := "/bin/jfrog"

	tests := []struct {
		name             string
		inputDst         *pb.Destination
		inputFilePattern string
		expectedErr      bool
		envVars          map[string]string
	}{
		{
			name: "username environment var is not set",
			inputDst: &pb.Destination{
				Connector: &pb.Connector{
					Id:   connectorID,
					Auth: pb.AuthType_BASIC_AUTH,
				},
			},
			expectedErr: true,
		},
		{
			name: "password environment var is not set",
			inputDst: &pb.Destination{
				Connector: &pb.Connector{
					Id:   connectorID,
					Auth: pb.AuthType_BASIC_AUTH,
				},
			},
			expectedErr: true,
			envVars: map[string]string{
				"USERNAME_" + connectorID: jfrogUsrName,
			},
		},
		{
			name: "jfrog cli environment var is not set",
			inputDst: &pb.Destination{
				Connector: &pb.Connector{
					Id:   connectorID,
					Auth: pb.AuthType_BASIC_AUTH,
				},
			},
			expectedErr: true,
			envVars: map[string]string{
				"USERNAME_" + connectorID: jfrogUsrName,
				"PASSWORD_" + connectorID: jfrogPwd,
			},
		},
		{
			name: "destination URL is not correct",
			inputDst: &pb.Destination{
				Connector: &pb.Connector{
					Id:   connectorID,
					Auth: pb.AuthType_BASIC_AUTH,
				},
			},
			inputFilePattern: filePattern,
			expectedErr:      true,
			envVars: map[string]string{
				"USERNAME_" + connectorID: jfrogUsrName,
				"PASSWORD_" + connectorID: jfrogPwd,
				"JFROG_PATH":              jfrogPath,
			},
		},
		{
			name: "failed to create client",
			inputDst: &pb.Destination{
				Connector: &pb.Connector{
					Id:   connectorID,
					Auth: pb.AuthType_BASIC_AUTH,
				},
				DestinationUrl: destinationURL,
			},
			inputFilePattern: filePattern,
			expectedErr:      true,
			envVars: map[string]string{
				"USERNAME_" + connectorID: "",
				"PASSWORD_" + connectorID: "",
				"JFROG_PATH":              jfrogPath,
			},
		},
	}
	for _, tc := range tests {
		if tc.envVars != nil {
			for k, v := range tc.envVars {
				if err := os.Setenv(k, v); err != nil {
					t.Fatalf("%s: failed to set environment variable: %s, %s", tc.name, k, v)
				}
			}
		}
		got := p.publishToJfrog(ctx, tc.inputFilePattern, tc.inputDst)
		if tc.expectedErr == (got == nil) {
			t.Fatalf("%s: expected error: %v, got: %v", tc.name, tc.expectedErr, got)
		}
		if tc.envVars != nil {
			for k := range tc.envVars {
				if err := os.Unsetenv(k); err != nil {
					t.Fatalf("%s: failed to unset environment variable: %s", tc.name, k)
				}
			}
		}
	}
}

func Test_publishToS3_WithError(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	p := &publishArtifacts{
		log: log.Sugar(),
	}

	filePattern := "/a/b/c"
	destinationURL := "s3://bucket/key"
	incorrectDstURL := "s3/bucket/key"
	connectorID := "s3Connector"
	accessKey := "test"
	secretKey := "test123"
	region := "us-east-1"

	tests := []struct {
		name             string
		inputDst         *pb.Destination
		inputFilePattern string
		expectedErr      bool
		envVars          map[string]string
	}{
		{
			name: "access key environment var is not set",
			inputDst: &pb.Destination{
				Connector: &pb.Connector{
					Id:   connectorID,
					Auth: pb.AuthType_ACCESS_KEY,
				},
				Region: region,
			},
			expectedErr: true,
		},
		{
			name: "secret environment var is not set",
			inputDst: &pb.Destination{
				Connector: &pb.Connector{
					Id:   connectorID,
					Auth: pb.AuthType_ACCESS_KEY,
				},
				Region: region,
			},
			expectedErr: true,
			envVars: map[string]string{
				"ACCESS_KEY_" + connectorID: accessKey,
			},
		},
		{
			name: "destination URL is not correct",
			inputDst: &pb.Destination{
				Connector: &pb.Connector{
					Id:   connectorID,
					Auth: pb.AuthType_BASIC_AUTH,
				},
				Region:         region,
				DestinationUrl: incorrectDstURL,
			},
			inputFilePattern: filePattern,
			expectedErr:      true,
			envVars: map[string]string{
				"ACCESS_KEY_" + connectorID: accessKey,
				"SECRET_KEY_" + connectorID: secretKey,
			},
		},
		{
			name: "file pattern is not correct",
			inputDst: &pb.Destination{
				Connector: &pb.Connector{
					Id:   connectorID,
					Auth: pb.AuthType_BASIC_AUTH,
				},
				Region:         region,
				DestinationUrl: destinationURL,
			},
			inputFilePattern: filePattern,
			expectedErr:      true,
			envVars: map[string]string{
				"ACCESS_KEY_" + connectorID: accessKey,
				"SECRET_KEY_" + connectorID: secretKey,
			},
		},
	}
	for _, tc := range tests {
		if tc.envVars != nil {
			for k, v := range tc.envVars {
				if err := os.Setenv(k, v); err != nil {
					t.Fatalf("%s: failed to set environment variable: %s, %s", tc.name, k, v)
				}
			}
		}
		got := p.publishToS3(ctx, tc.inputFilePattern, tc.inputDst)
		if tc.expectedErr == (got == nil) {
			t.Fatalf("%s: expected error: %v, got: %v", tc.name, tc.expectedErr, got)
		}
		if tc.envVars != nil {
			for k := range tc.envVars {
				if err := os.Unsetenv(k); err != nil {
					t.Fatalf("%s: failed to unset environment variable: %s", tc.name, k)
				}
			}
		}
	}
}

func Test_publishToDockerHub_WithEnvError(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	p := &publishArtifacts{
		log: log.Sugar(),
	}
	dockerFilePath := "~/Dockerfile"
	context := "~/"
	destinationURL := "vj/kaniko-test:0.1"
	connectorID := "dockerhub_connector"
	dockerHubUsername := "xyz"
	// dockerHubPassword := "xyz"

	tests := []struct {
		name             string
		inputDst         *pb.Destination
		inputFilePattern string
		expectedErr      bool
		envVars          map[string]string
	}{
		{
			name: "username & password not set",
			inputDst: &pb.Destination{
				DestinationUrl: destinationURL,
				Connector: &pb.Connector{
					Id:   connectorID,
					Auth: pb.AuthType_BASIC_AUTH,
				},
				LocationType: pb.LocationType_DOCKERHUB,
			},
			expectedErr: true,
		},
		{
			name: "password not set",
			inputDst: &pb.Destination{
				DestinationUrl: destinationURL,
				Connector: &pb.Connector{
					Id:   connectorID,
					Auth: pb.AuthType_BASIC_AUTH,
				},
				LocationType: pb.LocationType_DOCKERHUB,
			},
			expectedErr: true,
			envVars: map[string]string{
				"USERNAME_" + connectorID: dockerHubUsername,
			},
		},
	}
	fs := filesystem.NewOSFileSystem(log.Sugar())
	for _, tc := range tests {
		if tc.envVars != nil {
			for k, v := range tc.envVars {
				if err := os.Setenv(k, v); err != nil {
					t.Fatalf("%s: failed to set environment variable: %s, %s", tc.name, k, v)
				}
			}
		}
		got := p.buildPublishToDockerHub(dockerFilePath, context, tc.inputDst, fs)
		if tc.expectedErr == (got == nil) {
			t.Fatalf("%s: expected error: %v, got: %v", tc.name, tc.expectedErr, got)
		}
		if tc.envVars != nil {
			for k := range tc.envVars {
				if err := os.Unsetenv(k); err != nil {
					t.Fatalf("%s: failed to unset environment variable: %s", tc.name, k)
				}
			}
		}
	}
}

func Test_publishToGCR_WithEnvError(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	p := &publishArtifacts{
		log: log.Sugar(),
	}
	dockerFilePath := "~/Dockerfile"
	context := "~/"
	destinationURL := "us.gcr.io/kaniko-test:0.1"
	connectorID := "dockerhub_connector"
	// gcrSecretPath := "/gcr_secret"

	tests := []struct {
		name             string
		inputDst         *pb.Destination
		inputFilePattern string
		expectedErr      bool
		envVars          map[string]string
	}{
		{
			name: "secret path not set",
			inputDst: &pb.Destination{
				DestinationUrl: destinationURL,
				Connector: &pb.Connector{
					Id:   connectorID,
					Auth: pb.AuthType_SECRET_FILE,
				},
				LocationType: pb.LocationType_GCR,
			},
			expectedErr: true,
		},
	}
	fs := filesystem.NewOSFileSystem(log.Sugar())
	for _, tc := range tests {
		if tc.envVars != nil {
			for k, v := range tc.envVars {
				if err := os.Setenv(k, v); err != nil {
					t.Fatalf("%s: failed to set environment variable: %s, %s", tc.name, k, v)
				}
			}
		}
		got := p.buildPublishToGCR(dockerFilePath, context, tc.inputDst, fs)

		if tc.envVars != nil {
			for k := range tc.envVars {
				if err := os.Unsetenv(k); err != nil {
					t.Fatalf("%s: failed to unset environment variable: %s", tc.name, k)
				}
			}
		}

		if tc.expectedErr == (got == nil) {
			t.Fatalf("%s: expected error: %v, got: %v", tc.name, tc.expectedErr, got)
		}
	}
}

func Test_publishToECR_WithEnvError(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	p := &publishArtifacts{
		log: log.Sugar(),
	}
	dockerFilePath := "~/Dockerfile"
	context := "~/"
	destinationURL := "us.gcr.io/kaniko-test:0.1"
	connectorID := "dockerhub_connector"
	// secretKey := "secret-key"
	accessKey := "access-key"

	tests := []struct {
		name             string
		inputDst         *pb.Destination
		inputFilePattern string
		expectedErr      bool
		envVars          map[string]string
	}{
		{
			name: "access key not set",
			inputDst: &pb.Destination{
				DestinationUrl: destinationURL,
				Connector: &pb.Connector{
					Id:   connectorID,
					Auth: pb.AuthType_ACCESS_KEY,
				},
				LocationType: pb.LocationType_ECR,
			},
			expectedErr: true,
		},
		{
			name: "secret key not set",
			inputDst: &pb.Destination{
				DestinationUrl: destinationURL,
				Connector: &pb.Connector{
					Id:   connectorID,
					Auth: pb.AuthType_ACCESS_KEY,
				},
				LocationType: pb.LocationType_ECR,
			},
			expectedErr: true,
			envVars: map[string]string{
				"ACCESS_KEY_" + connectorID: accessKey,
			},
		},
	}
	fs := filesystem.NewOSFileSystem(log.Sugar())
	for _, tc := range tests {
		if tc.envVars != nil {
			for k, v := range tc.envVars {
				if err := os.Setenv(k, v); err != nil {
					t.Fatalf("%s: failed to set environment variable: %s, %s", tc.name, k, v)
				}
			}
		}
		got := p.buildPublishToECR(dockerFilePath, context, tc.inputDst, fs)

		if tc.envVars != nil {
			for k := range tc.envVars {
				if err := os.Unsetenv(k); err != nil {
					t.Fatalf("%s: failed to unset environment variable: %s", tc.name, k)
				}
			}
		}

		if tc.expectedErr == (got == nil) {
			t.Fatalf("%s: expected error: %v, got: %v", tc.name, tc.expectedErr, got)
		}
	}
}

func Test_publishToDockerHub_WithKanikoSuccess(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	fs := filesystem.NewOSFileSystem(log.Sugar())

	p := &publishArtifacts{
		log: log.Sugar(),
	}
	dockerFilePath := "~/Dockerfile"
	context := "~/"
	destinationURL := "vj/kaniko-test:0.1"
	connectorID := "dockerhub_connector"
	dockerHubUsername := "xyz"
	dockerHubPassword := "xyz"

	mockedKaniko := kaniko_mock.NewMockRegistry(ctrl)
	mockedKaniko.EXPECT().Publish(dockerFilePath, context, destinationURL).Return(nil)

	oldRegistryClient := newRegistryClient
	defer func() { newRegistryClient = oldRegistryClient }()
	newRegistryClient = func(log *zap.SugaredLogger, fs filesystem.FileSystem,
		opts ...kaniko.RegistryClientOption) (kaniko.Registry, error) {
		return mockedKaniko, nil
	}

	tc := struct {
		name     string
		inputDst *pb.Destination
		envVars  map[string]string
	}{
		name: "username and password are set",
		inputDst: &pb.Destination{
			DestinationUrl: destinationURL,
			Connector: &pb.Connector{
				Id:   connectorID,
				Auth: pb.AuthType_BASIC_AUTH,
			},
			LocationType: pb.LocationType_DOCKERHUB,
		},
		envVars: map[string]string{
			"USERNAME_" + connectorID: dockerHubUsername,
			"PASSWORD_" + connectorID: dockerHubPassword,
		},
	}
	for k, v := range tc.envVars {
		if err := os.Setenv(k, v); err != nil {
			t.Fatalf("%s: failed to set environment variable: %s", tc.name, k)
		}
	}

	err := p.buildPublishToDockerHub(dockerFilePath, context, tc.inputDst, fs)

	if tc.envVars != nil {
		for k := range tc.envVars {
			if err := os.Unsetenv(k); err != nil {
				t.Fatalf("%s: failed to unset environment variable: %s", tc.name, k)
			}
		}
	}
	assert.Nil(t, err)
}

func Test_publishToDockerHub_WithKanikoFailure(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	fs := filesystem.NewOSFileSystem(log.Sugar())

	p := &publishArtifacts{
		log: log.Sugar(),
	}
	dockerFilePath := "~/Dockerfile"
	context := "~/"
	destinationURL := "vj/kaniko-test:0.1"
	connectorID := "dockerhub_connector"
	dockerHubUsername := "xyz"
	dockerHubPassword := "xyz"

	mockedKaniko := kaniko_mock.NewMockRegistry(ctrl)
	mockedKaniko.EXPECT().Publish(dockerFilePath, context, destinationURL).Return(errors.New("Could not publish"))

	oldRegistryClient := newRegistryClient
	defer func() { newRegistryClient = oldRegistryClient }()
	newRegistryClient = func(log *zap.SugaredLogger, fs filesystem.FileSystem,
		opts ...kaniko.RegistryClientOption) (kaniko.Registry, error) {
		return mockedKaniko, nil
	}

	tc := struct {
		name     string
		inputDst *pb.Destination
		envVars  map[string]string
	}{
		name: "username and password are set",
		inputDst: &pb.Destination{
			DestinationUrl: destinationURL,
			Connector: &pb.Connector{
				Id:   connectorID,
				Auth: pb.AuthType_BASIC_AUTH,
			},
			LocationType: pb.LocationType_DOCKERHUB,
		},
		envVars: map[string]string{
			"USERNAME_" + connectorID: dockerHubUsername,
			"PASSWORD_" + connectorID: dockerHubPassword,
		},
	}
	for k, v := range tc.envVars {
		if err := os.Setenv(k, v); err != nil {
			t.Fatalf("%s: failed to set environment variable: %s", tc.name, k)
		}
	}

	err := p.buildPublishToDockerHub(dockerFilePath, context, tc.inputDst, fs)
	assert.NotNil(t, err)

	if tc.envVars != nil {
		for k := range tc.envVars {
			if err := os.Unsetenv(k); err != nil {
				t.Fatalf("%s: failed to unset environment variable: %s", tc.name, k)
			}
		}
	}
}

func Test_publishToECR_WithKanikoSuccess(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	fs := filesystem.NewOSFileSystem(log.Sugar())

	p := &publishArtifacts{
		log: log.Sugar(),
	}
	dockerFilePath := "~/Dockerfile"
	context := "~/"
	destinationURL := "vj/kaniko-test:0.1"
	connectorID := "dockerhub_connector"
	secretKey := "secret-key"
	accessKey := "access-key"

	mockedKaniko := kaniko_mock.NewMockRegistry(ctrl)
	mockedKaniko.EXPECT().Publish(dockerFilePath, context, destinationURL).Return(nil)

	oldRegistryClient := newRegistryClient
	defer func() { newRegistryClient = oldRegistryClient }()
	newRegistryClient = func(log *zap.SugaredLogger, fs filesystem.FileSystem,
		opts ...kaniko.RegistryClientOption) (kaniko.Registry, error) {
		return mockedKaniko, nil
	}

	tc := struct {
		name     string
		inputDst *pb.Destination
		envVars  map[string]string
	}{
		name: "all variables set",
		inputDst: &pb.Destination{
			DestinationUrl: destinationURL,
			Connector: &pb.Connector{
				Id:   connectorID,
				Auth: pb.AuthType_ACCESS_KEY,
			},
			LocationType: pb.LocationType_ECR,
		},
		envVars: map[string]string{
			"SECRET_KEY_" + connectorID: secretKey,
			"ACCESS_KEY_" + connectorID: accessKey,
		},
	}
	for k, v := range tc.envVars {
		if err := os.Setenv(k, v); err != nil {
			t.Fatalf("%s: failed to set environment variable: %s", tc.name, k)
		}
	}

	err := p.buildPublishToECR(dockerFilePath, context, tc.inputDst, fs)

	if tc.envVars != nil {
		for k := range tc.envVars {
			if err := os.Unsetenv(k); err != nil {
				t.Fatalf("%s: failed to unset environment variable: %s", tc.name, k)
			}
		}
	}

	assert.Nil(t, err)
}

func Test_publishToECR_WithKanikoFailure(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	fs := filesystem.NewOSFileSystem(log.Sugar())

	p := &publishArtifacts{
		log: log.Sugar(),
	}
	dockerFilePath := "~/Dockerfile"
	context := "~/"
	destinationURL := "vj/kaniko-test:0.1"
	connectorID := "dockerhub_connector"
	secretKey := "secret-key"
	accessKey := "access-key"

	mockedKaniko := kaniko_mock.NewMockRegistry(ctrl)
	mockedKaniko.EXPECT().Publish(dockerFilePath, context, destinationURL).Return(errors.New("publish error"))

	oldRegistryClient := newRegistryClient
	defer func() { newRegistryClient = oldRegistryClient }()
	newRegistryClient = func(log *zap.SugaredLogger, fs filesystem.FileSystem,
		opts ...kaniko.RegistryClientOption) (kaniko.Registry, error) {
		return mockedKaniko, nil
	}

	tc := struct {
		name     string
		inputDst *pb.Destination
		envVars  map[string]string
	}{
		name: "all variables set",
		inputDst: &pb.Destination{
			DestinationUrl: destinationURL,
			Connector: &pb.Connector{
				Id:   connectorID,
				Auth: pb.AuthType_ACCESS_KEY,
			},
			LocationType: pb.LocationType_ECR,
		},
		envVars: map[string]string{
			"SECRET_KEY_" + connectorID: secretKey,
			"ACCESS_KEY_" + connectorID: accessKey,
		},
	}
	for k, v := range tc.envVars {
		if err := os.Setenv(k, v); err != nil {
			t.Fatalf("%s: failed to set environment variable: %s", tc.name, k)
		}
	}

	err := p.buildPublishToECR(dockerFilePath, context, tc.inputDst, fs)

	if tc.envVars != nil {
		for k := range tc.envVars {
			if err := os.Unsetenv(k); err != nil {
				t.Fatalf("%s: failed to unset environment variable: %s", tc.name, k)
			}
		}
	}

	assert.NotNil(t, err)
}

func Test_publishToGCR_WithKanikoSuccess(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	fs := filesystem.NewOSFileSystem(log.Sugar())

	p := &publishArtifacts{
		log: log.Sugar(),
	}
	dockerFilePath := "~/Dockerfile"
	context := "~/"
	destinationURL := "vj/kaniko-test:0.1"
	connectorID := "dockerhub_connector"
	secretPath := "secret-path"

	mockedKaniko := kaniko_mock.NewMockRegistry(ctrl)
	mockedKaniko.EXPECT().Publish(dockerFilePath, context, destinationURL).Return(nil)

	oldRegistryClient := newRegistryClient
	defer func() { newRegistryClient = oldRegistryClient }()
	newRegistryClient = func(log *zap.SugaredLogger, fs filesystem.FileSystem,
		opts ...kaniko.RegistryClientOption) (kaniko.Registry, error) {
		return mockedKaniko, nil
	}

	tc := struct {
		name     string
		inputDst *pb.Destination
		envVars  map[string]string
	}{
		name: "all variables set",
		inputDst: &pb.Destination{
			DestinationUrl: destinationURL,
			Connector: &pb.Connector{
				Id:   connectorID,
				Auth: pb.AuthType_SECRET_FILE,
			},
			LocationType: pb.LocationType_GCR,
		},
		envVars: map[string]string{
			"SECRET_PATH_" + connectorID: secretPath,
		},
	}
	for k, v := range tc.envVars {
		if err := os.Setenv(k, v); err != nil {
			t.Fatalf("%s: failed to set environment variable: %s", tc.name, k)
		}
	}

	err := p.buildPublishToGCR(dockerFilePath, context, tc.inputDst, fs)

	if tc.envVars != nil {
		for k := range tc.envVars {
			if err := os.Unsetenv(k); err != nil {
				t.Fatalf("%s: failed to unset environment variable: %s", tc.name, k)
			}
		}
	}

	assert.Nil(t, err)
}

func Test_publishToGCR_WithKanikoFailure(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	fs := filesystem.NewOSFileSystem(log.Sugar())

	p := &publishArtifacts{
		log: log.Sugar(),
	}
	dockerFilePath := "~/Dockerfile"
	context := "~/"
	destinationURL := "vj/kaniko-test:0.1"
	connectorID := "dockerhub_connector"
	secretPath := "secret-path"

	mockedKaniko := kaniko_mock.NewMockRegistry(ctrl)
	mockedKaniko.EXPECT().Publish(dockerFilePath, context, destinationURL).Return(errors.New("publish error"))

	oldRegistryClient := newRegistryClient
	defer func() { newRegistryClient = oldRegistryClient }()
	newRegistryClient = func(log *zap.SugaredLogger, fs filesystem.FileSystem,
		opts ...kaniko.RegistryClientOption) (kaniko.Registry, error) {
		return mockedKaniko, nil
	}

	tc := struct {
		name     string
		inputDst *pb.Destination
		envVars  map[string]string
	}{
		name: "all variables set",
		inputDst: &pb.Destination{
			DestinationUrl: destinationURL,
			Connector: &pb.Connector{
				Id:   connectorID,
				Auth: pb.AuthType_SECRET_FILE,
			},
			LocationType: pb.LocationType_GCR,
		},
		envVars: map[string]string{
			"SECRET_PATH_" + connectorID: secretPath,
		},
	}
	for k, v := range tc.envVars {
		if err := os.Setenv(k, v); err != nil {
			t.Fatalf("%s: failed to set environment variable: %s", tc.name, k)
		}
	}

	err := p.buildPublishToGCR(dockerFilePath, context, tc.inputDst, fs)

	if tc.envVars != nil {
		for k := range tc.envVars {
			if err := os.Unsetenv(k); err != nil {
				t.Fatalf("%s: failed to unset environment variable: %s", tc.name, k)
			}
		}
	}

	assert.NotNil(t, err)
}

func Test_Publish(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	p := NewPublishArtifactsTask(log.Sugar())
	dockerFilePath := "~/dockerfile"
	context := "~/"
	filePattern := "/a/b/c"
	destinationURL := "https://harness.jfrog.io/artifactory/pcf"
	connectorID := "jfrogConnector"
	s3Region := "us-east-1"

	tests := []struct {
		name        string
		files       []*pb.UploadFile
		images      []*pb.BuildPublishImage
		expectedErr bool
	}{
		{
			name: "unsupported file upload",
			files: []*pb.UploadFile{
				{
					FilePattern: filePattern,
					Destination: &pb.Destination{
						DestinationUrl: destinationURL,
						Connector: &pb.Connector{
							Id: connectorID,
						},
						LocationType: pb.LocationType_GCR,
					},
				},
			},
			expectedErr: true,
		},
		{
			name: "unsupported image upload",
			images: []*pb.BuildPublishImage{
				{
					DockerFile: dockerFilePath,
					Context:    context,
					Destination: &pb.Destination{
						DestinationUrl: destinationURL,
						Connector: &pb.Connector{
							Id: connectorID,
						},
						LocationType: pb.LocationType_JFROG,
					},
				},
			},
			expectedErr: true,
		},
		{
			name: "failed to publish to JFROG",
			files: []*pb.UploadFile{
				{
					FilePattern: filePattern,
					Destination: &pb.Destination{
						DestinationUrl: destinationURL,
						Connector: &pb.Connector{
							Id: connectorID,
						},
						LocationType: pb.LocationType_JFROG,
					},
				},
			},
			expectedErr: true,
		},
		{
			name: "failed to publish to S3",
			files: []*pb.UploadFile{
				{
					FilePattern: filePattern,
					Destination: &pb.Destination{
						DestinationUrl: destinationURL,
						Connector: &pb.Connector{
							Id:   connectorID,
							Auth: pb.AuthType_ACCESS_KEY,
						},
						LocationType: pb.LocationType_S3,
						Region:       s3Region,
					},
				},
			},
			expectedErr: true,
		},
		{
			name:        "success",
			expectedErr: false,
		},
	}
	for _, tc := range tests {
		got := p.Publish(ctx, tc.files, tc.images)
		if tc.expectedErr == (got == nil) {
			t.Fatalf("%s: expected error: %v, got: %v", tc.name, tc.expectedErr, got)
		}
	}
}
