// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package steps

import (
	"context"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/golang/protobuf/proto"
	"github.com/pkg/errors"
	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/logs"
	"github.com/wings-software/portal/product/ci/engine/output"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

func createPublishArtifactsStep() *pb.UnitStep {
	// Registry type not specified
	info1 := &pb.BuildPublishImage{
		DockerFile: "/step/harness/Dockerifle_1",
		Context:    "/step/harness",
		Destination: &pb.Destination{
			DestinationUrl: "us.gcr.io/ci-play/kaniko-build:v1.2",
			Connector: &pb.Connector{
				Id:   "G",
				Auth: pb.AuthType_SECRET_FILE,
			},
		},
	}
	info2 := &pb.BuildPublishImage{
		DockerFile: "/step/harness/Dockerifle_2",
		Context:    "/step/harness",
		Destination: &pb.Destination{
			DestinationUrl: "vistaarjuneja/xyz:0.1",
			LocationType:   pb.LocationType_DOCKERHUB,
			Connector: &pb.Connector{
				Id:   "D",
				Auth: pb.AuthType_BASIC_AUTH,
			},
		},
	}
	info3 := &pb.BuildPublishImage{
		DockerFile: "/step/harness/Dockerifle_1",
		Context:    "/step/harness",
		Destination: &pb.Destination{
			DestinationUrl: "448640225317.dkr.ecr.us-east-1.amazonaws.com/vistaarjuneja:v0.5",
			LocationType:   pb.LocationType_ECR,
			Connector: &pb.Connector{
				Id:   "E",
				Auth: pb.AuthType_ACCESS_KEY,
			},
		},
	}

	info4 := &pb.UploadFile{
		FilePattern: "/step/harness/test.txt",
		Destination: &pb.Destination{
			DestinationUrl: "https://harness.jfrog.io/artifactory/pcf",
			Connector: &pb.Connector{
				Id:   "J",
				Auth: pb.AuthType_BASIC_AUTH,
			},
			LocationType: pb.LocationType_JFROG,
		},
	}
	x := []*pb.BuildPublishImage{}
	x = append(x, info1)
	x = append(x, info2)
	x = append(x, info3)

	y := []*pb.UploadFile{}
	y = append(y, info4)

	publishArtifactsStep := &pb.UnitStep_PublishArtifacts{
		PublishArtifacts: &pb.PublishArtifactsStep{
			Images: x,
			Files:  y,
		},
	}

	return &pb.UnitStep{
		Id:          "10",
		DisplayName: "publishing",
		Step:        publishArtifactsStep,
	}
}

func TestCreatePublishArtifacts_Invalid(t *testing.T) {
	ctx := context.Background()
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	// Create an invalid publish artifacts image step
	step := createPublishArtifactsStep()

	testPublishStep := NewPublishArtifactsStep(step, nil, log.Sugar())
	err := testPublishStep.Run(ctx)

	assert.NotNil(t, err)
}

func Test_GetRequestArgError(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	stepID := "test-step"
	stepName := "test step"
	dockerFile := "/a/b"
	invalidFilePattern := "~test"
	invalidDockerFile := "~test"
	invalidContext := "~test"

	tests := []struct {
		name        string
		input       *pb.UnitStep
		expectedErr bool
	}{
		{
			name: "invalid file pattern",
			input: &pb.UnitStep{
				Id:          stepID,
				DisplayName: stepName,
				Step: &pb.UnitStep_PublishArtifacts{
					PublishArtifacts: &pb.PublishArtifactsStep{
						Files: []*pb.UploadFile{
							{
								FilePattern: invalidFilePattern,
							},
						},
					},
				},
			},
			expectedErr: true,
		},
		{
			name: "invalid docker file",
			input: &pb.UnitStep{
				Id:          stepID,
				DisplayName: stepName,
				Step: &pb.UnitStep_PublishArtifacts{
					PublishArtifacts: &pb.PublishArtifactsStep{
						Images: []*pb.BuildPublishImage{
							{
								DockerFile: invalidDockerFile,
							},
						},
					},
				},
			},
			expectedErr: true,
		},
		{
			name: "invalid context",
			input: &pb.UnitStep{
				Id:          stepID,
				DisplayName: stepName,
				Step: &pb.UnitStep_PublishArtifacts{
					PublishArtifacts: &pb.PublishArtifactsStep{
						Images: []*pb.BuildPublishImage{
							{
								DockerFile: dockerFile,
								Context:    invalidContext,
							},
						},
					},
				},
			},
			expectedErr: true,
		},
	}
	for _, tc := range tests {
		testPublishStep := NewPublishArtifactsStep(tc.input, nil, log.Sugar())
		got := testPublishStep.Run(ctx)
		if tc.expectedErr == (got == nil) {
			t.Fatalf("%s: expected error: %v, got: %v", tc.name, tc.expectedErr, got)
		}
	}
}

func Test_GetRequestArgResolveJEXL(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	stepID := "step2"
	stepName := "test step"

	jFilePattern := "${step1.output.foo}"
	jDstURL := "${step1.output.hello}"
	jRegion := "${step1.output.lite}"
	jDockerFile := "${step1.output.docker}"
	jContextFile := "${step1.output.story}"
	filePatternVal := "bar"
	dstURLVal := "world"
	regionVal := "engine"
	dockerFileVal := "hub"
	contextFileVal := "teller"

	tests := []struct {
		name           string
		input          *pb.UnitStep
		resolvedFiles  []*pb.UploadFile
		resolvedImages []*pb.BuildPublishImage
		jexlEvalRet    map[string]string
		jexlEvalErr    error
		expectedErr    bool
	}{
		{
			name: "jexl evaluate error",
			input: &pb.UnitStep{
				Id:          stepID,
				DisplayName: stepName,
				Step: &pb.UnitStep_PublishArtifacts{
					PublishArtifacts: &pb.PublishArtifactsStep{
						Files: []*pb.UploadFile{
							{
								FilePattern: jFilePattern,
							},
						},
					},
				},
			},
			jexlEvalRet: nil,
			jexlEvalErr: errors.New("evaluation failed"),
			expectedErr: true,
		},
		{
			name: "jexl evaluate success with files",
			input: &pb.UnitStep{
				Id:          stepID,
				DisplayName: stepName,
				Step: &pb.UnitStep_PublishArtifacts{
					PublishArtifacts: &pb.PublishArtifactsStep{
						Files: []*pb.UploadFile{
							{
								FilePattern: jFilePattern,
								Destination: &pb.Destination{
									DestinationUrl: jDstURL,
									Connector: &pb.Connector{
										Id:   "J",
										Auth: pb.AuthType_ACCESS_KEY,
									},
									LocationType: pb.LocationType_S3,
									Region:       jRegion,
								},
							},
						},
					},
				},
			},
			resolvedFiles: []*pb.UploadFile{
				{
					FilePattern: filePatternVal,
					Destination: &pb.Destination{
						DestinationUrl: dstURLVal,
						Connector: &pb.Connector{
							Id:   "J",
							Auth: pb.AuthType_ACCESS_KEY,
						},
						LocationType: pb.LocationType_S3,
						Region:       regionVal,
					},
				},
			},
			jexlEvalRet: map[string]string{jFilePattern: filePatternVal, jDstURL: dstURLVal, jRegion: regionVal},
			jexlEvalErr: nil,
			expectedErr: false,
		},
		{
			name: "jexl evaluate success with images",
			input: &pb.UnitStep{
				Id:          stepID,
				DisplayName: stepName,
				Step: &pb.UnitStep_PublishArtifacts{
					PublishArtifacts: &pb.PublishArtifactsStep{
						Images: []*pb.BuildPublishImage{
							{
								DockerFile: jDockerFile,
								Context:    jContextFile,
								Destination: &pb.Destination{
									DestinationUrl: jDstURL,
									Connector: &pb.Connector{
										Id:   "J",
										Auth: pb.AuthType_ACCESS_KEY,
									},
									LocationType: pb.LocationType_ECR,
								},
							},
						},
					},
				},
			},
			resolvedImages: []*pb.BuildPublishImage{
				{
					DockerFile: dockerFileVal,
					Context:    contextFileVal,
					Destination: &pb.Destination{
						DestinationUrl: dstURLVal,
						Connector: &pb.Connector{
							Id:   "J",
							Auth: pb.AuthType_ACCESS_KEY,
						},
						LocationType: pb.LocationType_ECR,
					},
				},
			},
			jexlEvalRet: map[string]string{jDockerFile: dockerFileVal, jDstURL: dstURLVal, jContextFile: contextFileVal},
			jexlEvalErr: nil,
			expectedErr: false,
		},
	}
	oldJEXLEval := evaluateJEXL
	defer func() { evaluateJEXL = oldJEXLEval }()
	for _, tc := range tests {
		testPublishStep := &publishArtifactsStep{
			stageOutput: nil,
			files:       tc.input.GetPublishArtifacts().GetFiles(),
			images:      tc.input.GetPublishArtifacts().GetImages(),
			log:         log.Sugar(),
		}
		// Initialize a mock CI addon
		evaluateJEXL = func(ctx context.Context, stepID string, expressions []string, o output.StageOutput,
			isSkipCondition bool, log *zap.SugaredLogger) (map[string]string, error) {
			return tc.jexlEvalRet, tc.jexlEvalErr
		}
		files, images, got := testPublishStep.resolveExpressions(ctx)
		if tc.expectedErr == (got == nil) {
			t.Fatalf("%s: expected error: %v, got: %v", tc.name, tc.expectedErr, got)
		}

		if got == nil {
			assert.Equal(t, len(files), len(tc.resolvedFiles))
			for i := 0; i < len(files); i++ {
				assert.Equal(t, proto.Equal(files[i], tc.resolvedFiles[i]), true)
			}
			for i := 0; i < len(images); i++ {
				assert.Equal(t, proto.Equal(images[i], tc.resolvedImages[i]), true)
			}
		}

	}
}
