// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

// Package grpc provides a way to interact with expression evaluator GRPC server using a client
package grpc

import (
	"time"

	grpc_retry "github.com/grpc-ecosystem/go-grpc-middleware/retry"
	pb "github.com/wings-software/portal/960-expression-service/src/main/proto/io/harness/expression/service"
	"go.uber.org/zap"
	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
)

//go:generate mockgen -source client.go -package=grpc -destination mocks/client_mock.go ExpressionEvalClient

const (
	backoffTime = 100 * time.Millisecond
	maxRetries  = 9
	// Port is the port on which expression evaluator service runs.
	Port = 8080
)

//ExpressionEvalClient implements a GRPC client to communicate with Expression evaluator service
type ExpressionEvalClient interface {
	CloseConn() error
	Client() pb.ExpressionEvaulatorServiceClient
}

type expressionEvalClient struct {
	conn       *grpc.ClientConn
	log        *zap.SugaredLogger
	grpcClient pb.ExpressionEvaulatorServiceClient
}

// NewExpressionEvalClient creates a expression evaluator client
func NewExpressionEvalClient(ip string, log *zap.SugaredLogger) (ExpressionEvalClient, error) {
	// Default gRPC Call options - can be made configurable if the need arises
	// Retries are ENABLED by default for all RPCs on the below codes. To disable retries, pass in a zero value
	// Example: client.EvaluateExpression(ctx, req, grpc_retry.WithMax(0))
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
		ip,
		grpc.WithInsecure(),
		grpc.WithStreamInterceptor(grpc_retry.StreamClientInterceptor(opts...)),
		grpc.WithUnaryInterceptor(grpc_retry.UnaryClientInterceptor(opts...)),
		grpc.WithNoProxy())
	if err != nil {
		log.Errorw("Could not create a client to expression evaluator service", "error_msg", zap.Error(err))
		return nil, err
	}

	c := pb.NewExpressionEvaulatorServiceClient(conn)
	client := expressionEvalClient{
		log:        log,
		conn:       conn,
		grpcClient: c,
	}
	return &client, nil
}

func (c *expressionEvalClient) CloseConn() error {
	if err := c.conn.Close(); err != nil {
		return err
	}
	return nil
}

func (c *expressionEvalClient) Client() pb.ExpressionEvaulatorServiceClient {
	return c.grpcClient
}
