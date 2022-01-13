// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package grpcclient

import (
	"fmt"
	"time"

	grpc_retry "github.com/grpc-ecosystem/go-grpc-middleware/retry"
	pb "github.com/wings-software/portal/product/ci/scm/proto"
	"go.uber.org/zap"
	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
)

//go:generate mockgen -source scm_client.go -package=grpcclient -destination mocks/scm_client_mock.go SCMClient

const (
	backoffTime = 100 * time.Millisecond
	maxRetries  = 9
	// SCMPort is the port on which CI scm service runs.
	SCMPort = 8091
)

// SCMClient implements a GRPC client to communicate with CI scm
type SCMClient interface {
	CloseConn() error
	Client() pb.SCMClient
}

type scmClient struct {
	port       uint
	conn       *grpc.ClientConn
	log        *zap.SugaredLogger
	grpcClient pb.SCMClient
}

// NewSCMClient creates a CI scm client
func NewSCMClient(port uint, log *zap.SugaredLogger) (SCMClient, error) {
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
		log.Errorw("Could not create a client to CI SCM", "error_msg", zap.Error(err))
		return nil, err
	}

	c := pb.NewSCMClient(conn)
	client := scmClient{
		port:       port,
		log:        log,
		conn:       conn,
		grpcClient: c,
	}
	return &client, nil
}

func (c *scmClient) CloseConn() error {
	if err := c.conn.Close(); err != nil {
		return err
	}
	return nil
}

func (c *scmClient) Client() pb.SCMClient {
	return c.grpcClient
}
