package tasks

import (
	"context"
	"os"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/wings-software/portal/commons/go/lib/logs"
	pb "github.com/wings-software/portal/product/ci/addon/proto"
	"go.uber.org/zap"
)

func Test_validatePublishArtifact(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	p := &publishArtifact{
		log: log.Sugar(),
	}
	taskID := "test-id"
	filePattern := "/a/b/c"
	destinationURL := "file://a/b/c"
	connectorID := "testConnector"
	dockerFile := "~/Dockerfile"
	dockerImage := "helloworld"
	dockerImageTag := "v1"

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
				Files:  []*pb.UploadFiles{{}},
			},
			expectedErr: true,
		},
		{
			name: "destination artifact is not set",
			input: &pb.PublishArtifactsRequest{
				TaskId: &pb.TaskId{Id: taskID},
				Files: []*pb.UploadFiles{
					{FilePattern: filePattern},
				},
			},
			expectedErr: true,
		},
		{
			name: "destination connector is not set",
			input: &pb.PublishArtifactsRequest{
				TaskId: &pb.TaskId{Id: taskID},
				Files: []*pb.UploadFiles{
					{
						FilePattern: filePattern,
						Destination: &pb.DestinationArtifact{
							Destination: destinationURL,
						},
					},
				},
			},
			expectedErr: true,
		},
		{
			name: "valid request",
			input: &pb.PublishArtifactsRequest{
				TaskId: &pb.TaskId{Id: taskID},
				Files: []*pb.UploadFiles{
					{
						FilePattern: filePattern,
						Destination: &pb.DestinationArtifact{
							Destination: destinationURL,
							Connector: &pb.Connector{
								Id: connectorID,
							},
						},
					},
				},
			},
			expectedErr: false,
		},
		{
			name: "docker file is not set",
			input: &pb.PublishArtifactsRequest{
				TaskId: &pb.TaskId{Id: taskID},
				Images: []*pb.BuildUploadImage{{}},
			},
			expectedErr: true,
		},
		{
			name: "image name is not set",
			input: &pb.PublishArtifactsRequest{
				TaskId: &pb.TaskId{Id: taskID},
				Images: []*pb.BuildUploadImage{
					{
						Image: &pb.BuildImage{
							DockerFile: dockerFile,
						},
					},
				},
			},
			expectedErr: true,
		},
		{
			name: "image tag is not set",
			input: &pb.PublishArtifactsRequest{
				TaskId: &pb.TaskId{Id: taskID},
				Images: []*pb.BuildUploadImage{
					{
						Image: &pb.BuildImage{
							DockerFile: dockerFile,
							Image:      dockerImage,
						},
					},
				},
			},
			expectedErr: true,
		},
		{
			name: "destination artifact is not set",
			input: &pb.PublishArtifactsRequest{
				TaskId: &pb.TaskId{Id: taskID},
				Images: []*pb.BuildUploadImage{
					{
						Image: &pb.BuildImage{
							DockerFile: dockerFile,
							Image:      dockerImage,
							Tag:        dockerImageTag,
						},
					},
				},
			},
			expectedErr: true,
		},
		{
			name: "valid request",
			input: &pb.PublishArtifactsRequest{
				TaskId: &pb.TaskId{Id: taskID},
				Images: []*pb.BuildUploadImage{
					{
						Image: &pb.BuildImage{
							DockerFile: dockerFile,
							Image:      dockerImage,
							Tag:        dockerImageTag,
						},
						Destination: &pb.DestinationArtifact{
							Destination: destinationURL,
							Connector: &pb.Connector{
								Id: connectorID,
							},
						},
					},
				},
			},
			expectedErr: false,
		},
	}
	for _, tc := range tests {
		got := p.validate(tc.input)
		if tc.expectedErr == (got == nil) {
			t.Fatalf("%s: expected error: %v, got: %v", tc.name, tc.expectedErr, got)
		}
	}
}

func Test_publishToJfrogWithError(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	p := &publishArtifact{
		log: log.Sugar(),
	}
	filePattern := "/a/b/c"
	destinationURL := "https://harness.jfrog.io/artifactory/pcf"
	connectorID := "jfrogConnector"
	jfrogUsrName := "admin"
	jfrogPwd := "admin"

	tests := []struct {
		name             string
		inputDst         *pb.DestinationArtifact
		inputFilePattern string
		expectedErr      bool
		envVars          map[string]string
	}{
		{
			name: "invalid auth",
			inputDst: &pb.DestinationArtifact{
				Connector: &pb.Connector{
					Id:   connectorID,
					Auth: pb.AuthType_SECRET_FILE,
				},
			},
			expectedErr: true,
		},
		{
			name: "username environment var is not set",
			inputDst: &pb.DestinationArtifact{
				Connector: &pb.Connector{
					Id:   connectorID,
					Auth: pb.AuthType_BASIC_AUTH,
				},
			},
			expectedErr: true,
		},
		{
			name: "password environment var is not set",
			inputDst: &pb.DestinationArtifact{
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
			inputDst: &pb.DestinationArtifact{
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
			inputDst: &pb.DestinationArtifact{
				Connector: &pb.Connector{
					Id:   connectorID,
					Auth: pb.AuthType_BASIC_AUTH,
				},
				Destination: destinationURL,
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

func Test_Publish(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	p := NewPublishArtifactTask(log.Sugar())
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
			name:        "validation failed",
			input:       nil,
			expectedErr: true,
		},
		{
			name: "unsupported artifact upload",
			input: &pb.PublishArtifactsRequest{
				TaskId: &pb.TaskId{Id: taskID},
				Files: []*pb.UploadFiles{
					{
						FilePattern: filePattern,
						Destination: &pb.DestinationArtifact{
							Destination: destinationURL,
							Connector: &pb.Connector{
								Id: connectorID,
							},
							ArtifactType: pb.ArtifactType_GCR,
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
				Files: []*pb.UploadFiles{
					{
						FilePattern: filePattern,
						Destination: &pb.DestinationArtifact{
							Destination: destinationURL,
							Connector: &pb.Connector{
								Id: connectorID,
							},
							ArtifactType: pb.ArtifactType_JFROG,
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
