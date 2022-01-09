// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package logutil

import (
	"context"
	"errors"
	"sync"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	grpcclient "github.com/wings-software/portal/product/ci/engine/grpc/client"
	mclient "github.com/wings-software/portal/product/ci/engine/grpc/client/mocks"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
	"google.golang.org/grpc"
)

// struct used to test gRPC abstractions
// This type implements a lite engine gRPC client to track ops done on it
type logProxyClient struct {
	ops []string
	wg  sync.WaitGroup
	err error // if created with error, return error
}

// streaming client
type logProxyUploadUsingLinkClient struct {
	err error
	grpc.ClientStream
}

type logProxyUploadClient struct {
	err error
	grpc.ClientStream
}

func (lc *logProxyUploadUsingLinkClient) Send(in *pb.UploadUsingLinkRequest) error {
	return nil
}

func (lc *logProxyUploadUsingLinkClient) CloseAndRecv() (*pb.UploadUsingLinkResponse, error) {
	return &pb.UploadUsingLinkResponse{}, nil
}

func (lc *logProxyUploadClient) Send(in *pb.UploadRequest) error {
	return nil
}

func (lc *logProxyUploadClient) CloseAndRecv() (*pb.UploadResponse, error) {
	return &pb.UploadResponse{}, nil
}

func (lpc *logProxyClient) Open(ctx context.Context, in *pb.OpenRequest, opts ...grpc.CallOption) (*pb.OpenResponse, error) {
	lpc.ops = append(lpc.ops, "open")
	lpc.wg.Done() // Record an open call
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

func (lpc *logProxyClient) UploadUsingLink(ctx context.Context, opts ...grpc.CallOption) (pb.LogProxy_UploadUsingLinkClient, error) {
	lpc.ops = append(lpc.ops, "uploadusinglink")
	return &logProxyUploadUsingLinkClient{}, lpc.err
}

func (lpc *logProxyClient) Upload(ctx context.Context, opts ...grpc.CallOption) (pb.LogProxy_UploadClient, error) {
	lpc.ops = append(lpc.ops, "upload")
	return &logProxyUploadClient{}, lpc.err
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
	defer func() {
		newLogProxyClient = oldLogProxyClient
	}()
	mGrpcClient := NewMockGrpcLogProxyClient(nil)
	mGrpcClient.wg.Add(1)
	mEngineClient := mclient.NewMockLogProxyClient(ctrl)
	mEngineClient.EXPECT().Client().Return(mGrpcClient)

	newLogProxyClient = func(port uint, log *zap.SugaredLogger) (grpcclient.LogProxyClient, error) {
		return mEngineClient, nil
	}
	key := "foo:test"
	_, err := GetGrpcRemoteLogger(key)

	mGrpcClient.wg.Wait() // Wait for the open call

	assert.Equal(t, err, nil)
	assert.Equal(t, len(mGrpcClient.ops), 1)
	assert.Equal(t, mGrpcClient.ops[0], "open")
}

func Test_GetGrpcRemoteLogger_OpenFailure(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	oldEngineClient := newLogProxyClient
	defer func() {
		newLogProxyClient = oldEngineClient
	}()
	mGrpcClient := NewMockGrpcLogProxyClient(errors.New("failure"))
	mGrpcClient.wg.Add(1)
	mEngineClient := mclient.NewMockLogProxyClient(ctrl)
	mEngineClient.EXPECT().Client().Return(mGrpcClient)
	newLogProxyClient = func(port uint, log *zap.SugaredLogger) (grpcclient.LogProxyClient, error) {
		return mEngineClient, nil
	}
	key := "foo:test"
	_, err := GetGrpcRemoteLogger(key)

	mGrpcClient.wg.Wait() // Wait for the open call

	// Failure of opening the stream should not error out the logger
	assert.Nil(t, err)
}
