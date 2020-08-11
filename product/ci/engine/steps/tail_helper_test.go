package steps

import (
	"context"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/logs"
	caddon "github.com/wings-software/portal/product/ci/addon/grpc/client"
	mgrpc "github.com/wings-software/portal/product/ci/addon/grpc/client/mocks"
	addonpb "github.com/wings-software/portal/product/ci/addon/proto"
	"go.uber.org/zap"
	"google.golang.org/grpc"
)

func TestStartTailSuccess(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	conn, err := grpc.DialContext(ctx, "bufnet", grpc.WithContextDialer(bufDialer), grpc.WithInsecure())
	if err != nil {
		t.Fatalf("Failed to dial bufnet: %v", err)
	}
	defer conn.Close()

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

	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	m := make(map[string]string)

	err = StartTail(ctx, log.Sugar(), "/some/random/path", m)
	assert.Nil(t, err)
}

func TestStartTailDeadlineExceeded(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	conn, err := grpc.DialContext(ctx, "bufnet", grpc.WithContextDialer(bufDialer), grpc.WithInsecure())
	if err != nil {
		t.Fatalf("Failed to dial bufnet: %v", err)
	}
	defer conn.Close()

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

	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	ctx, cancel := context.WithTimeout(context.Background(), 0)
	defer cancel()
	ctrl, ctx = gomock.WithContext(ctx, t)
	defer ctrl.Finish()

	m := make(map[string]string)

	err = StartTail(ctx, log.Sugar(), "/some/random/path", m)
	assert.NotNil(t, err)
}

func TestStopTailSuccess(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	conn, err := grpc.DialContext(ctx, "bufnet", grpc.WithContextDialer(bufDialer), grpc.WithInsecure())
	if err != nil {
		t.Fatalf("Failed to dial bufnet: %v", err)
	}
	defer conn.Close()

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

	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	err = StopTail(ctx, log.Sugar(), "/some/random/path", true)
	assert.Nil(t, err)
}

func TestStopTailDeadlineExceeded(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	conn, err := grpc.DialContext(ctx, "bufnet", grpc.WithContextDialer(bufDialer), grpc.WithInsecure())
	if err != nil {
		t.Fatalf("Failed to dial bufnet: %v", err)
	}
	defer conn.Close()

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

	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	ctx, cancel := context.WithTimeout(context.Background(), 0)
	defer cancel()
	ctrl, ctx = gomock.WithContext(ctx, t)
	defer ctrl.Finish()

	err = StopTail(ctx, log.Sugar(), "/some/random/path", true)
	assert.NotNil(t, err)
}
