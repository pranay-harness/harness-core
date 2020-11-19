package logutil

import (
	"context"
	"errors"
	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	grpcclient "github.com/wings-software/portal/product/ci/engine/grpc/client"
	mclient "github.com/wings-software/portal/product/ci/engine/grpc/client/mocks"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
	"google.golang.org/grpc"
	"testing"
)

// struct used to test gRPC abstractions
// This type implements a lite engine gRPC client to track ops done on it
type logProxyClient struct {
	ops []string
	err error // if created with error, return error
}

func (lpc *logProxyClient) Open(ctx context.Context, in *pb.OpenRequest, opts ...grpc.CallOption) (*pb.OpenResponse, error) {
	lpc.ops = append(lpc.ops, "open")
	return &pb.OpenResponse{}, lpc.err
}

func (lpc *logProxyClient) Write(ctx context.Context, in *pb.WriteRequest, opts ...grpc.CallOption) (*pb.WriteResponse, error) {
	lpc.ops = append(lpc.ops, "write")
	return &pb.WriteResponse{}, lpc.err
}

func (lpc *logProxyClient) UploadLink(ctx context.Context, in *pb.UploadLinkRequest, opts ...grpc.CallOption) (*pb.UploadLinkResponse, error) {
	lpc.ops = append(lpc.ops, "uploadlink")
	return &pb.UploadLinkResponse{}, lpc.err
}

func (lpc *logProxyClient) Close(ctx context.Context, in *pb.CloseRequest, opts ...grpc.CallOption) (*pb.CloseResponse, error) {
	lpc.ops = append(lpc.ops, "close")
	return &pb.CloseResponse{}, lpc.err
}

func (lpc *logProxyClient) UploadUsingLink(ctx context.Context, in *pb.UploadUsingLinkRequest, opts ...grpc.CallOption) (*pb.UploadUsingLinkResponse, error) {
	lpc.ops = append(lpc.ops, "uploadusinglink")
	return &pb.UploadUsingLinkResponse{}, lpc.err
}

func NewMockGrpcLogProxyClient(err error) *logProxyClient {
	return &logProxyClient{
		ops: []string{},
		err: err,
	}
}

func Test_GetGrpcRemoteLogger(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	oldLogProxyClient := newLogProxyClient
	oldGetLogKey := getLogKey
	defer func() {
		newLogProxyClient = oldLogProxyClient
		getLogKey = oldGetLogKey
	}()
	mGrpcClient := NewMockGrpcLogProxyClient(nil)
	mEngineClient := mclient.NewMockLogProxyClient(ctrl)
	mEngineClient.EXPECT().Client().Return(mGrpcClient)
	getLogKey = func(stepID string) (string, error) {
		return stepID, nil
	}
	newLogProxyClient = func(port uint, log *zap.SugaredLogger) (grpcclient.LogProxyClient, error) {
		return mEngineClient, nil
	}
	key := "test"
	_, err := GetGrpcRemoteLogger(key)

	assert.Equal(t, err, nil)
	assert.Equal(t, len(mGrpcClient.ops), 1)
	assert.Equal(t, mGrpcClient.ops[0], "open")
}

func Test_GetGrpcRemoteLogger_OpenFailure(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	oldEngineClient := newLogProxyClient
	oldGetLogKey := getLogKey
	defer func() {
		newLogProxyClient = oldEngineClient
		getLogKey = oldGetLogKey
	}()
	mGrpcClient := NewMockGrpcLogProxyClient(errors.New("failure"))
	mEngineClient := mclient.NewMockLogProxyClient(ctrl)
	mEngineClient.EXPECT().Client().Return(mGrpcClient)
	getLogKey = func(stepID string) (string, error) {
		return stepID, nil
	}
	newLogProxyClient = func(port uint, log *zap.SugaredLogger) (grpcclient.LogProxyClient, error) {
		return mEngineClient, nil
	}
	key := "test"
	_, err := GetGrpcRemoteLogger(key)

	// Failure of opening the stream should not error out the logger
	assert.Nil(t, err)
}

func Test_GetGrpcRemoteLogger_KeyFailure(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	oldGetLogKey := getLogKey
	defer func() {
		getLogKey = oldGetLogKey
	}()
	getLogKey = func(stepID string) (string, error) {
		return "", errors.New("failure")
	}

	key := "test"
	_, err := GetGrpcRemoteLogger(key)

	assert.NotEqual(t, err, nil)
}
