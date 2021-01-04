package remote

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

type mockJEXLClient struct {
	response *pb.EvaluateJEXLResponse
	err      error
}

func (c *mockJEXLClient) GetImageEntrypoint(ctx context.Context, in *pb.GetImageEntrypointRequest, opts ...grpc.CallOption) (*pb.GetImageEntrypointResponse, error) {
	return nil, nil
}
func (c *mockJEXLClient) UpdateState(ctx context.Context, in *pb.UpdateStateRequest, opts ...grpc.CallOption) (*pb.UpdateStateResponse, error) {
	return nil, nil
}

func (c *mockJEXLClient) EvaluateJEXL(ctx context.Context, in *pb.EvaluateJEXLRequest, opts ...grpc.CallOption) (*pb.EvaluateJEXLResponse, error) {
	return c.response, c.err
}

func TestEvaluateJEXLServerErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	id := "test"
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	c := &mockJEXLClient{
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

	_, err := EvaluateJEXL(ctx, id, nil, nil, log.Sugar())
	assert.NotNil(t, err)
}

func TestEvaluateJEXLServer(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	id := "test"
	exprs := []string{"${foo.bar}"}
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	c := &mockJEXLClient{
		response: &pb.EvaluateJEXLResponse{},
		err:      nil,
	}
	mClient := mgrpc.NewMockEngineClient(ctrl)
	mClient.EXPECT().CloseConn().Return(nil)
	mClient.EXPECT().Client().Return(c)

	oldClient := newEngineClient
	defer func() { newEngineClient = oldClient }()
	newEngineClient = func(port uint, log *zap.SugaredLogger) (cengine.EngineClient, error) {
		return mClient, nil
	}

	_, err := EvaluateJEXL(ctx, id, exprs, nil, log.Sugar())
	assert.Nil(t, err)
}
