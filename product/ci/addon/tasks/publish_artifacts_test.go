package tasks

import (
	"context"
	"github.com/golang/mock/gomock"
	"github.com/pkg/errors"
	"github.com/stretchr/testify/assert"
	"go.uber.org/zap"
	"os"
	"testing"

	"github.com/wings-software/portal/commons/go/lib/filesystem"
	"github.com/wings-software/portal/commons/go/lib/kaniko"
	"github.com/wings-software/portal/commons/go/lib/kaniko/mocks"
	"github.com/wings-software/portal/commons/go/lib/logs"
	pb "github.com/wings-software/portal/product/ci/addon/proto"
)

func Test_validatePublishArtifactRequest(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	p := &publishArtifacts{
		log: log.Sugar(),
	}
	taskID := "test-id"
	filePattern := "/a/b/c"
	destinationURL := "file://a/b/c"
	connectorID := "testConnector"
	dockerFilePath := "~/DockerFile"
	context := "~/"

	tests := []struct {
		name        string
		input       *pb.PublishArtifactsRequest
		expectedErr bool
	}{
		{
			name:        "nil request",
			input:       nil,
			expectedErr: true,
		},
		{
			name:        "no task ID",
			input:       &pb.PublishArtifactsRequest{},
			expectedErr: true,
		},
		{
			name: "file pattern not set",
			input: &pb.PublishArtifactsRequest{
				TaskId: &pb.TaskId{Id: taskID},
				Files:  []*pb.UploadFile{{}},
			},
			expectedErr: true,
		},
		{
			name: "destination is not set",
			input: &pb.PublishArtifactsRequest{
				TaskId: &pb.TaskId{Id: taskID},
				Files: []*pb.UploadFile{
					{FilePattern: filePattern},
				},
			},
			expectedErr: true,
		},
		{
			name: "Connector is not set",
			input: &pb.PublishArtifactsRequest{
				TaskId: &pb.TaskId{Id: taskID},
				Files: []*pb.UploadFile{
					{
						FilePattern: filePattern,
						Destination: &pb.Destination{
							DestinationUrl: destinationURL,
						},
					},
				},
			},
			expectedErr: true,
		},
		{
			name: "valid request without location type",
			input: &pb.PublishArtifactsRequest{
				TaskId: &pb.TaskId{Id: taskID},
				Files: []*pb.UploadFile{
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
			},
			expectedErr: true,
		},
		{
			name: "valid request with files & images",
			input: &pb.PublishArtifactsRequest{
				TaskId: &pb.TaskId{Id: taskID},
				Files: []*pb.UploadFile{
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
				Images: []*pb.BuildPublishImage{
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
			},
			expectedErr: false,
		},
		{
			name: "empty image list",
			input: &pb.PublishArtifactsRequest{
				TaskId: &pb.TaskId{Id: taskID},
				Images: []*pb.BuildPublishImage{{}},
			},
			expectedErr: true,
		},
		{
			name: "context is not set",
			input: &pb.PublishArtifactsRequest{
				TaskId: &pb.TaskId{Id: taskID},
				Images: []*pb.BuildPublishImage{
					{
						DockerFile: dockerFilePath,
					},
				},
			},
			expectedErr: true,
		},
		{
			name: "dockerfile is not set",
			input: &pb.PublishArtifactsRequest{
				TaskId: &pb.TaskId{Id: taskID},
				Images: []*pb.BuildPublishImage{
					{
						Context: dockerFilePath,
					},
				},
			},
			expectedErr: true,
		},
		{
			name: "jfrog with different auth",
			input: &pb.PublishArtifactsRequest{
				TaskId: &pb.TaskId{Id: taskID},
				Files: []*pb.UploadFile{
					{FilePattern: filePattern,
						Destination: &pb.Destination{
							DestinationUrl: destinationURL,
							Connector: &pb.Connector{
								Id:   connectorID,
								Auth: pb.AuthType_SECRET_FILE,
							},
							LocationType: pb.LocationType_JFROG},
					},
				},
			},
			expectedErr: true,
		},
		{
			name: "gcr with different auth",
			input: &pb.PublishArtifactsRequest{
				TaskId: &pb.TaskId{Id: taskID},
				Images: []*pb.BuildPublishImage{
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
			},
			expectedErr: true,
		},
		{
			name: "ecr with different auth",
			input: &pb.PublishArtifactsRequest{
				TaskId: &pb.TaskId{Id: taskID},
				Images: []*pb.BuildPublishImage{
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
			},
			expectedErr: true,
		},
		{
			name: "dockerhub with different auth",
			input: &pb.PublishArtifactsRequest{
				TaskId: &pb.TaskId{Id: taskID},
				Images: []*pb.BuildPublishImage{
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
			},
			expectedErr: true,
		},
	}
	for _, tc := range tests {
		got := p.validate(tc.input)
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
			name: "file pattern is not correct",
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
	taskID := "test-task"
	filePattern := "/a/b/c"
	destinationURL := "https://harness.jfrog.io/artifactory/pcf"
	connectorID := "jfrogConnector"

	tests := []struct {
		name        string
		input       *pb.PublishArtifactsRequest
		expectedErr bool
	}{
		{
			name: "unsupported file upload",
			input: &pb.PublishArtifactsRequest{
				TaskId: &pb.TaskId{Id: taskID},
				Files: []*pb.UploadFile{
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
			},
			expectedErr: true,
		},
		{
			name: "unsupported image upload",
			input: &pb.PublishArtifactsRequest{
				TaskId: &pb.TaskId{Id: taskID},
				Images: []*pb.BuildPublishImage{
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
			},
			expectedErr: true,
		},
		{
			name: "failed to publish to JFROG",
			input: &pb.PublishArtifactsRequest{
				TaskId: &pb.TaskId{Id: taskID},
				Files: []*pb.UploadFile{
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
			},
			expectedErr: true,
		},
		{
			name: "success",
			input: &pb.PublishArtifactsRequest{
				TaskId: &pb.TaskId{Id: taskID},
			},
			expectedErr: false,
		},
	}
	for _, tc := range tests {
		got := p.Publish(ctx, tc.input)
		if tc.expectedErr == (got == nil) {
			t.Fatalf("%s: expected error: %v, got: %v", tc.name, tc.expectedErr, got)
		}
	}
}
