// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

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

func (c *mockJEXLClient) Ping(ctx context.Context, in *pb.PingRequest, opts ...grpc.CallOption) (*pb.PingResponse, error) {
	return nil, nil
}

func (c *mockJEXLClient) ExecuteStep(ctx context.Context, in *pb.ExecuteStepRequest, opts ...grpc.CallOption) (*pb.ExecuteStepResponse, error) {
	return nil, nil
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
