package logutil

import (
	"bufio"
	"context"
	"encoding/json"
	"errors"
	"io"

	"github.com/wings-software/portal/product/ci/engine/consts"
	grpcclient "github.com/wings-software/portal/product/ci/engine/grpc/client"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"github.com/wings-software/portal/product/log-service/client"
	"github.com/wings-software/portal/product/log-service/stream"
	"go.uber.org/zap"
)

var (
	newLogProxyClient = grpcclient.NewLogProxyClient
)

// GrpcRemoteClient implements the Log service client interface
// It accepts a lite engine grpc client to send RPCs to it.
type GrpcRemoteClient struct {
	grpcClient grpcclient.LogProxyClient
}

// NewGrpcRemoteClient returns a client that can interact with the log service gRPC server.
func NewGrpcRemoteClient() (*GrpcRemoteClient, error) {
	log := zap.NewExample().Sugar()
	client, err := newLogProxyClient(consts.LiteEnginePort, log)
	if err != nil {
		return nil, err
	}
	return &GrpcRemoteClient{
		grpcClient: client,
	}, nil
}

// UploadLink returns an upload link for the logs.
func (gw *GrpcRemoteClient) UploadLink(ctx context.Context, key string) (*client.Link, error) {
	in := &pb.UploadLinkRequest{Key: key}
	resp, err := gw.grpcClient.Client().UploadLink(ctx, in)
	if err != nil {
		return nil, err
	}
	return &client.Link{Value: resp.GetLink()}, nil
}

// Open opens up a log stream.
func (gw *GrpcRemoteClient) Open(ctx context.Context, key string) error {
	in := &pb.OpenRequest{Key: key}
	_, err := gw.grpcClient.Client().Open(ctx, in)
	if err != nil {
		return err
	}
	return nil
}

// Close deletes the log stream.
func (gw *GrpcRemoteClient) Close(ctx context.Context, key string) error {
	in := &pb.CloseRequest{Key: key}
	_, err := gw.grpcClient.Client().Close(ctx, in)
	if err != nil {
		return err
	}
	return nil
}

// UploadUsingLink uses a link to upload logs to the data store.
func (gw *GrpcRemoteClient) UploadUsingLink(ctx context.Context, link string, r io.Reader) error {
	var lines []string
	scanner := bufio.NewScanner(r)
	for scanner.Scan() {
		lines = append(lines, scanner.Text())
	}
	in := &pb.UploadUsingLinkRequest{Link: link, Lines: lines}
	_, err := gw.grpcClient.Client().UploadUsingLink(ctx, in)
	if err != nil {
		return err
	}
	return nil
}

// Write writes logs to the data stream.
func (gw *GrpcRemoteClient) Write(ctx context.Context, key string, lines []*stream.Line) error {
	var streamLines []string
	for _, line := range lines {
		jsonLine, err := json.Marshal(line)
		if err != nil {
			return err
		}
		streamLines = append(streamLines, string(jsonLine))
	}
	in := &pb.WriteRequest{Key: key, Lines: streamLines}
	_, err := gw.grpcClient.Client().Write(ctx, in)
	if err != nil {
		return err
	}
	return nil
}

// Upload is not implemented.
func (gw *GrpcRemoteClient) Upload(ctx context.Context, key string, r io.Reader) error {
	return errors.New("not implemented")
}

// Download is not implemented.
func (gw *GrpcRemoteClient) Download(ctx context.Context, key string) (io.ReadCloser, error) {
	return nil, errors.New("not implemented")
}

// DownloadLink is not implemented.
func (gw *GrpcRemoteClient) DownloadLink(ctx context.Context, key string) (*client.Link, error) {
	return nil, errors.New("not implemented")
}

// Tail is not implemented.
func (gw *GrpcRemoteClient) Tail(ctx context.Context, key string) (<-chan string, <-chan error) {
	return nil, nil
}

// Info is not implemented.
func (gw *GrpcRemoteClient) Info(ctx context.Context) (*stream.Info, error) {
	return nil, errors.New("not implemented")
}
