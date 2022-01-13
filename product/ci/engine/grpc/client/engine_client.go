// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package grpcclient

import (
	"fmt"
	"time"

	grpc_retry "github.com/grpc-ecosystem/go-grpc-middleware/retry"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
)

//go:generate mockgen -source engine_client.go -package=grpcclient -destination mocks/engine_client_mock.go EngineClient

const (
	backoffTime = 100 * time.Millisecond
	maxRetries  = 9
)

//EngineClient implements a GRPC client to communicate with CI engine
type EngineClient interface {
	CloseConn() error
	Client() pb.LiteEngineClient
}

type engineClient struct {
	port       uint
	conn       *grpc.ClientConn
	log        *zap.SugaredLogger
	grpcClient pb.LiteEngineClient
}

// NewEngineClient creates a CI engine client
func NewEngineClient(port uint, log *zap.SugaredLogger) (EngineClient, error) {
	// Default gRPC Call options - can be made configurable if the need arises
	// Retries are ENABLED by default for all RPCs on the below codes. To disable retries, pass in a zero value
	// Example: client.PublishArtifacts(ctx, req, grpc_retry.WithMax(0))
	// With this configuration, total retry time is approximately 25 seconds
	opts := []grpc_retry.CallOption{
		grpc_retry.WithBackoff(grpc_retry.BackoffExponential(backoffTime)),
		grpc_retry.WithCodes(codes.ResourceExhausted, codes.Unavailable),
		grpc_retry.WithMax(maxRetries),
	}

	// Create a connection on the port and disable transport security (TLS/SSL)
	// Configure interceptors for stream/unary RPCs to use default gRPC opts
	// TODO: Add TLS (CI-119)
	conn, err := grpc.Dial(
		fmt.Sprintf(":%d", port),
		grpc.WithInsecure(),
		grpc.WithStreamInterceptor(grpc_retry.StreamClientInterceptor(opts...)),
		grpc.WithUnaryInterceptor(grpc_retry.UnaryClientInterceptor(opts...)),
		grpc.WithNoProxy())
	if err != nil {
		log.Errorw("Could not create a client to CI Engine", "error_msg", zap.Error(err))
		return nil, err
	}

	c := pb.NewLiteEngineClient(conn)
	client := engineClient{
		port:       port,
		log:        log,
		conn:       conn,
		grpcClient: c,
	}
	return &client, nil
}

func (c *engineClient) CloseConn() error {
	if err := c.conn.Close(); err != nil {
		return err
	}
	return nil
}

func (c *engineClient) Client() pb.LiteEngineClient {
	return c.grpcClient
}
