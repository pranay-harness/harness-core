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
	addonpb "github.com/wings-software/portal/product/ci/addon/proto"
	egrpc "github.com/wings-software/portal/product/ci/engine/grpc"
	mgrpc "github.com/wings-software/portal/product/ci/engine/grpc/mocks"
	enginepb "github.com/wings-software/portal/product/ci/engine/proto"
)

var lis *bufconn.Listener

const bufSize = 1024 * 1024

func init() {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	server := addon.NewCIAddonHandler(log.Sugar())
	lis = bufconn.Listen(bufSize)
	s := grpc.NewServer()
	addonpb.RegisterCIAddonServer(s, server)
	go func() {
		if err := s.Serve(lis); err != nil {
			log.Sugar().Fatalw(fmt.Sprintf("Server exited with error: %d", err))
		}
	}()
}

func bufDialer(context.Context, string) (net.Conn, error) {
	return lis.Dial()
}

func createPublishArtifactsStep() *enginepb.Step {

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

	publishArtifactsStep := &enginepb.Step_PublishArtifacts{
		PublishArtifacts: &enginepb.PublishArtifactsStep{
			Images: x,
			Files:  y,
		},
	}

	return &enginepb.Step{
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
	publishArtifactsStep := &enginepb.Step_PublishArtifacts{
		PublishArtifacts: &enginepb.PublishArtifactsStep{
			Images: x,
		},
	}

	step := &enginepb.Step{
		Id:          "xyz",
		DisplayName: "publishing",
		Step:        publishArtifactsStep,
	}

	client := addonpb.NewCIAddonClient(conn)

	oldClient := newCIAddonClient
	defer func() { newCIAddonClient = oldClient }()
	// Initialize a mock CI addon
	mockClient := mgrpc.NewMockCIAddonClient(ctrl)
	mockClient.EXPECT().Client().Return(client)
	mockClient.EXPECT().CloseConn().Return(nil)
	newCIAddonClient = func(port uint, log *zap.SugaredLogger) (egrpc.CIAddonClient, error) {
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
	client := addonpb.NewCIAddonClient(conn)

	oldClient := newCIAddonClient
	defer func() { newCIAddonClient = oldClient }()
	// Initialize a mock CI addon
	mockClient := mgrpc.NewMockCIAddonClient(ctrl)
	fmt.Println(client)
	mockClient.EXPECT().Client().Return(client)
	mockClient.EXPECT().CloseConn().Return(nil)
	newCIAddonClient = func(port uint, log *zap.SugaredLogger) (egrpc.CIAddonClient, error) {
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

	oldClient := newCIAddonClient
	defer func() { newCIAddonClient = oldClient }()
	// Initialize a mock CI addon
	newCIAddonClient = func(port uint, log *zap.SugaredLogger) (egrpc.CIAddonClient, error) {
		return nil, errors.New("Could not create client")
	}

	testPublishStep := NewPublishArtifactsStep(step, log.Sugar())
	err := testPublishStep.Run(ctx)

	assert.NotNil(t, err)
}
