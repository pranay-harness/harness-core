package imageutil

import (
	"context"
	"errors"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/tj/assert"
	cengine "github.com/wings-software/portal/product/ci/engine/grpc/client"
	"google.golang.org/grpc"

	"github.com/wings-software/portal/commons/go/lib/logs"
	mgrpc "github.com/wings-software/portal/product/ci/engine/grpc/client/mocks"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

type mockClient struct {
	response *pb.GetImageEntrypointResponse
	err      error
}

func (c *mockClient) GetImageEntrypoint(ctx context.Context, in *pb.GetImageEntrypointRequest, opts ...grpc.CallOption) (*pb.GetImageEntrypointResponse, error) {
	return c.response, c.err
}
func (c *mockClient) UpdateState(ctx context.Context, in *pb.UpdateStateRequest, opts ...grpc.CallOption) (*pb.UpdateStateResponse, error) {
	return nil, nil
}

func TestGetEntrypoint(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	image := "redis"
	id := "test"
	secret := "foo"
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	c := &mockClient{
		response: nil,
		err:      errors.New("server not running"),
	}
	mClient := mgrpc.NewMockEngineClient(ctrl)
	mClient.EXPECT().CloseConn().Return(nil)
	mClient.EXPECT().Client().Return(c)

	oldClient := newEngineClient
	defer func() { newEngineClient = oldClient }()
	newEngineClient = func(port uint, log *zap.SugaredLogger) (cengine.EngineClient, error) {
		return mClient, nil
	}

	_, _, err := GetEntrypoint(ctx, id, image, secret, log.Sugar())
	assert.NotNil(t, err)
}
