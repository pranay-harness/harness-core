package steps

import (
	"context"
	"fmt"
	"github.com/golang/mock/gomock"
	"github.com/pkg/errors"
	"github.com/stretchr/testify/assert"
	"go.uber.org/zap"
	"google.golang.org/grpc"
	"google.golang.org/grpc/test/bufconn"
	"net"
	"testing"

	"github.com/wings-software/portal/commons/go/lib/logs"
	addon "github.com/wings-software/portal/product/ci/addon/grpc"
	caddon "github.com/wings-software/portal/product/ci/addon/grpc/client"
	mgrpc "github.com/wings-software/portal/product/ci/addon/grpc/client/mocks"
	addonpb "github.com/wings-software/portal/product/ci/addon/proto"
	enginepb "github.com/wings-software/portal/product/ci/engine/proto"
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

	testPublishStep := NewPublishArtifactsStep(step, log.Sugar())
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

	testPublishStep := NewPublishArtifactsStep(step, log.Sugar())
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

	testPublishStep := NewPublishArtifactsStep(step, log.Sugar())
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
		testPublishStep := NewPublishArtifactsStep(tc.input, log.Sugar())
		got := testPublishStep.Run(ctx)
		if tc.expectedErr == (got == nil) {
			t.Fatalf("%s: expected error: %v, got: %v", tc.name, tc.expectedErr, got)
		}
	}
}
