package steps

import (
	"context"
	"fmt"
	"net"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/golang/protobuf/proto"
	"github.com/pkg/errors"
	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/logs"
	addon "github.com/wings-software/portal/product/ci/addon/grpc"
	caddon "github.com/wings-software/portal/product/ci/addon/grpc/client"
	mgrpc "github.com/wings-software/portal/product/ci/addon/grpc/client/mocks"
	addonpb "github.com/wings-software/portal/product/ci/addon/proto"
	"github.com/wings-software/portal/product/ci/engine/output"
	enginepb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
	"google.golang.org/grpc"
	"google.golang.org/grpc/test/bufconn"
)

var lis *bufconn.Listener

const bufSize = 1024 * 1024

func init() {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	stopCh := make(chan bool)
	server := addon.NewAddonHandler(stopCh, log.Sugar())
	lis = bufconn.Listen(bufSize)
	s := grpc.NewServer()
	addonpb.RegisterAddonServer(s, server)
	go func() {
		if err := s.Serve(lis); err != nil {
			log.Sugar().Fatalw(fmt.Sprintf("Server exited with error: %d", err))
		}
	}()
}

func bufDialer(context.Context, string) (net.Conn, error) {
	return lis.Dial()
}

func createPublishArtifactsStep() *enginepb.UnitStep {
	// Registry type not specified
	info1 := &addonpb.BuildPublishImage{
		DockerFile: "/step/harness/Dockerifle_1",
		Context:    "/step/harness",
		Destination: &addonpb.Destination{
			DestinationUrl: "us.gcr.io/ci-play/kaniko-build:v1.2",
			Connector: &addonpb.Connector{
				Id:   "G",
				Auth: addonpb.AuthType_SECRET_FILE,
			},
		},
	}
	info2 := &addonpb.BuildPublishImage{
		DockerFile: "/step/harness/Dockerifle_2",
		Context:    "/step/harness",
		Destination: &addonpb.Destination{
			DestinationUrl: "vistaarjuneja/xyz:0.1",
			LocationType:   addonpb.LocationType_DOCKERHUB,
			Connector: &addonpb.Connector{
				Id:   "D",
				Auth: addonpb.AuthType_BASIC_AUTH,
			},
		},
	}
	info3 := &addonpb.BuildPublishImage{
		DockerFile: "/step/harness/Dockerifle_1",
		Context:    "/step/harness",
		Destination: &addonpb.Destination{
			DestinationUrl: "448640225317.dkr.ecr.us-east-1.amazonaws.com/vistaarjuneja:v0.5",
			LocationType:   addonpb.LocationType_ECR,
			Connector: &addonpb.Connector{
				Id:   "E",
				Auth: addonpb.AuthType_ACCESS_KEY,
			},
		},
	}

	info4 := &addonpb.UploadFile{
		FilePattern: "/step/harness/test.txt",
		Destination: &addonpb.Destination{
			DestinationUrl: "https://harness.jfrog.io/artifactory/pcf",
			Connector: &addonpb.Connector{
				Id:   "J",
				Auth: addonpb.AuthType_BASIC_AUTH,
			},
			LocationType: addonpb.LocationType_JFROG,
		},
	}
	x := []*addonpb.BuildPublishImage{}
	x = append(x, info1)
	x = append(x, info2)
	x = append(x, info3)

	y := []*addonpb.UploadFile{}
	y = append(y, info4)

	publishArtifactsStep := &enginepb.UnitStep_PublishArtifacts{
		PublishArtifacts: &enginepb.PublishArtifactsStep{
			Images: x,
			Files:  y,
		},
	}

	return &enginepb.UnitStep{
		Id:          "10",
		DisplayName: "publishing",
		Step:        publishArtifactsStep,
	}
}

func TestCreatePublishArtifacts_Success(t *testing.T) {
	ctx := context.Background()
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	conn, err := grpc.DialContext(ctx, "bufnet", grpc.WithContextDialer(bufDialer), grpc.WithInsecure())
	if err != nil {
		t.Fatalf("Failed to dial bufnet: %v", err)
	}
	defer conn.Close()

	// Create a valid PublishArtifacts step
	x := []*addonpb.BuildPublishImage{}
	publishArtifactsStep := &enginepb.UnitStep_PublishArtifacts{
		PublishArtifacts: &enginepb.PublishArtifactsStep{
			Images: x,
		},
	}

	step := &enginepb.UnitStep{
		Id:          "xyz",
		DisplayName: "publishing",
		Step:        publishArtifactsStep,
	}

	client := addonpb.NewAddonClient(conn)

	oldClient := newAddonClient
	defer func() { newAddonClient = oldClient }()
	// Initialize a mock CI addon
	mockClient := mgrpc.NewMockAddonClient(ctrl)
	mockClient.EXPECT().Client().Return(client)
	mockClient.EXPECT().CloseConn().Return(nil)
	newAddonClient = func(port uint, log *zap.SugaredLogger) (caddon.AddonClient, error) {
		return mockClient, nil
	}

	testPublishStep := NewPublishArtifactsStep(step, nil, log.Sugar())
	err = testPublishStep.Run(ctx)

	assert.Nil(t, err)
}

func TestCreatePublishArtifacts_Invalid(t *testing.T) {
	ctx := context.Background()
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	conn, err := grpc.DialContext(ctx, "bufnet", grpc.WithContextDialer(bufDialer), grpc.WithInsecure())
	if err != nil {
		t.Fatalf("Failed to dial bufnet: %v", err)
	}
	defer conn.Close()

	// Create an invalid publish artifacts image step
	step := createPublishArtifactsStep()
	client := addonpb.NewAddonClient(conn)

	oldClient := newAddonClient
	defer func() { newAddonClient = oldClient }()
	// Initialize a mock CI addon
	mockClient := mgrpc.NewMockAddonClient(ctrl)
	mockClient.EXPECT().Client().Return(client)
	mockClient.EXPECT().CloseConn().Return(nil)
	newAddonClient = func(port uint, log *zap.SugaredLogger) (caddon.AddonClient, error) {
		return mockClient, nil
	}

	testPublishStep := NewPublishArtifactsStep(step, nil, log.Sugar())
	err = testPublishStep.Run(ctx)

	assert.NotNil(t, err)
}

func TestCreatePublishArtifacts_ClientCreationErr(t *testing.T) {
	ctx := context.Background()
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	// Create an invalid publish artifacts image step
	step := createPublishArtifactsStep()

	oldClient := newAddonClient
	defer func() { newAddonClient = oldClient }()
	// Initialize a mock CI addon
	newAddonClient = func(port uint, log *zap.SugaredLogger) (caddon.AddonClient, error) {
		return nil, errors.New("Could not create client")
	}

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
		input       *enginepb.UnitStep
		expectedErr bool
	}{
		{
			name: "invalid file pattern",
			input: &enginepb.UnitStep{
				Id:          stepID,
				DisplayName: stepName,
				Step: &enginepb.UnitStep_PublishArtifacts{
					PublishArtifacts: &enginepb.PublishArtifactsStep{
						Files: []*addonpb.UploadFile{
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
			input: &enginepb.UnitStep{
				Id:          stepID,
				DisplayName: stepName,
				Step: &enginepb.UnitStep_PublishArtifacts{
					PublishArtifacts: &enginepb.PublishArtifactsStep{
						Images: []*addonpb.BuildPublishImage{
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
			input: &enginepb.UnitStep{
				Id:          stepID,
				DisplayName: stepName,
				Step: &enginepb.UnitStep_PublishArtifacts{
					PublishArtifacts: &enginepb.PublishArtifactsStep{
						Images: []*addonpb.BuildPublishImage{
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
		input          *enginepb.UnitStep
		resolvedFiles  []*addonpb.UploadFile
		resolvedImages []*addonpb.BuildPublishImage
		jexlEvalRet    map[string]string
		jexlEvalErr    error
		expectedErr    bool
	}{
		{
			name: "jexl evaluate error",
			input: &enginepb.UnitStep{
				Id:          stepID,
				DisplayName: stepName,
				Step: &enginepb.UnitStep_PublishArtifacts{
					PublishArtifacts: &enginepb.PublishArtifactsStep{
						Files: []*addonpb.UploadFile{
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
			input: &enginepb.UnitStep{
				Id:          stepID,
				DisplayName: stepName,
				Step: &enginepb.UnitStep_PublishArtifacts{
					PublishArtifacts: &enginepb.PublishArtifactsStep{
						Files: []*addonpb.UploadFile{
							{
								FilePattern: jFilePattern,
								Destination: &addonpb.Destination{
									DestinationUrl: jDstURL,
									Connector: &addonpb.Connector{
										Id:   "J",
										Auth: addonpb.AuthType_ACCESS_KEY,
									},
									LocationType: addonpb.LocationType_S3,
									Region:       jRegion,
								},
							},
						},
					},
				},
			},
			resolvedFiles: []*addonpb.UploadFile{
				{
					FilePattern: filePatternVal,
					Destination: &addonpb.Destination{
						DestinationUrl: dstURLVal,
						Connector: &addonpb.Connector{
							Id:   "J",
							Auth: addonpb.AuthType_ACCESS_KEY,
						},
						LocationType: addonpb.LocationType_S3,
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
			input: &enginepb.UnitStep{
				Id:          stepID,
				DisplayName: stepName,
				Step: &enginepb.UnitStep_PublishArtifacts{
					PublishArtifacts: &enginepb.PublishArtifactsStep{
						Images: []*addonpb.BuildPublishImage{
							{
								DockerFile: jDockerFile,
								Context:    jContextFile,
								Destination: &addonpb.Destination{
									DestinationUrl: jDstURL,
									Connector: &addonpb.Connector{
										Id:   "J",
										Auth: addonpb.AuthType_ACCESS_KEY,
									},
									LocationType: addonpb.LocationType_ECR,
								},
							},
						},
					},
				},
			},
			resolvedImages: []*addonpb.BuildPublishImage{
				{
					DockerFile: dockerFileVal,
					Context:    contextFileVal,
					Destination: &addonpb.Destination{
						DestinationUrl: dstURLVal,
						Connector: &addonpb.Connector{
							Id:   "J",
							Auth: addonpb.AuthType_ACCESS_KEY,
						},
						LocationType: addonpb.LocationType_ECR,
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
		evaluateJEXL = func(ctx context.Context, expressions []string, o output.StageOutput,
			log *zap.SugaredLogger) (map[string]string, error) {
			return tc.jexlEvalRet, tc.jexlEvalErr
		}
		ret, got := testPublishStep.createPublishArtifactArg(ctx)
		if tc.expectedErr == (got == nil) {
			t.Fatalf("%s: expected error: %v, got: %v", tc.name, tc.expectedErr, got)
		}

		if got == nil {
			assert.Equal(t, len(ret.GetFiles()), len(tc.resolvedFiles))
			for i := 0; i < len(ret.GetFiles()); i++ {
				assert.Equal(t, proto.Equal(ret.GetFiles()[i], tc.resolvedFiles[i]), true)
			}
			for i := 0; i < len(ret.GetImages()); i++ {
				assert.Equal(t, proto.Equal(ret.GetImages()[i], tc.resolvedImages[i]), true)
			}
		}

	}
}
