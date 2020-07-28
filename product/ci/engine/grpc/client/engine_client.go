// Package grpcclient provides a way to interact with Lite engine GRPC server using a client
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

//go:generate mockgen -source engine_client.go -package=grpcclient -destination mocks/engine_client_mock.go LiteEngineClient

const (
	backoffTime = 100 * time.Millisecond
	maxRetries  = 9
)

//LiteEngineClient implements a GRPC client to communicate with CI lite engine
type LiteEngineClient interface {
	CloseConn() error
	Client() pb.LiteEngineClient
}

type liteEngineClient struct {
	port       uint
	conn       *grpc.ClientConn
	log        *zap.SugaredLogger
	grpcClient pb.LiteEngineClient
}

// NewLiteEngineClient creates a CI lite engine client
func NewLiteEngineClient(port uint, log *zap.SugaredLogger) (LiteEngineClient, error) {
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
		grpc.WithUnaryInterceptor(grpc_retry.UnaryClientInterceptor(opts...)))
	if err != nil {
		log.Errorw("Could not create a client to CI lite engine", "error_msg", zap.Error(err))
		return nil, err
	}

	c := pb.NewLiteEngineClient(conn)
	client := liteEngineClient{
		port:       port,
		log:        log,
		conn:       conn,
		grpcClient: c,
	}
	return &client, nil
}

func (c *liteEngineClient) CloseConn() error {
	if err := c.conn.Close(); err != nil {
		return err
	}
	return nil
}

func (c *liteEngineClient) Client() pb.LiteEngineClient {
	return c.grpcClient
}
