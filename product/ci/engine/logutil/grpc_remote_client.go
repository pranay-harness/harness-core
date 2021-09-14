// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package logutil

import (
	"bufio"
	"context"
	"encoding/json"
	"errors"
	"io"

	grpc_retry "github.com/grpc-ecosystem/go-grpc-middleware/retry"
	"go.uber.org/zap"

	"github.com/wings-software/portal/product/ci/engine/consts"
	grpcclient "github.com/wings-software/portal/product/ci/engine/grpc/client"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"github.com/wings-software/portal/product/log-service/client"
	"github.com/wings-software/portal/product/log-service/stream"
)

const (
	uploadBatch = 1000 // upload final logs to RPC server in chunks
)

var (
	newLogProxyClient = grpcclient.NewLogProxyClient
)

// GrpcRemoteClient implements the Log service client interface
// It accepts a lite engine grpc client to send RPCs to it.
type GrpcRemoteClient struct {
	grpcClient grpcclient.LogProxyClient
	log        *zap.SugaredLogger
}

// NewGrpcRemoteClient returns a client that can interact with the log service gRPC server.
func NewGrpcRemoteClient() (*GrpcRemoteClient, error) {
	l, err := zap.NewProduction()
	if err != nil {
		return &GrpcRemoteClient{}, err
	}
	client, err := newLogProxyClient(consts.LiteEnginePort, l.Sugar())
	if err != nil {
		return nil, err
	}
	return &GrpcRemoteClient{
		grpcClient: client,
		log:        l.Sugar(),
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
	reader := bufio.NewReader(r)
	stream, err := gw.grpcClient.Client().UploadUsingLink(ctx, grpc_retry.Disable())
	if err != nil {
		return err
	}
	for {
		line, err := reader.ReadString('\n')
		if err != nil && err != io.EOF {
			gw.log.Errorw("could not parse line while trying to upload logs", zap.Error(err))
			break
		}

		lines = append(lines, line)
		if err != nil {
			break
		}
		if len(lines)%uploadBatch == 0 {
			in := &pb.UploadUsingLinkRequest{Link: link, Lines: lines}
			if serr := stream.Send(in); serr != nil {
				gw.log.Errorw("upload using link RPC failed", zap.Error(serr))
			}
			lines = []string{}
		}
	}
	if len(lines) > 0 {
		in := &pb.UploadUsingLinkRequest{Link: link, Lines: lines}
		if serr := stream.Send(in); serr != nil {
			gw.log.Errorw("unable to send some lines via RPC: ", zap.Error(serr))
		}
		lines = []string{}
	}

	// Close the stream and receive result
	_, err = stream.CloseAndRecv()
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
			gw.log.Errorw("error while marshalling", "key", key, zap.Error(err))
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
