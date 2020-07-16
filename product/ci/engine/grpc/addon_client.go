package grpc

import (
	"fmt"
	"time"

	grpc_retry "github.com/grpc-ecosystem/go-grpc-middleware/retry"
	pb "github.com/wings-software/portal/product/ci/addon/proto"
	"go.uber.org/zap"
	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
)

const (
	backoffTime = 100 * time.Millisecond
	maxRetries  = 9
	// CIAddonPort is the port on which CI addon service runs.
	CIAddonPort = 8001
)

//CIAddonClient implements a GRPC client to communicate with CI addon
type CIAddonClient interface {
	CloseConn() error
	Client() pb.CIAddonClient
}

type ciAddonClient struct {
	port       uint
	conn       *grpc.ClientConn
	log        *zap.SugaredLogger
	grpcClient pb.CIAddonClient
}

// NewCIAddonClient creates a CI addon client
func NewCIAddonClient(port uint, log *zap.SugaredLogger) (CIAddonClient, error) {
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
		log.Errorw("Could not create a client to CI Addon", "error_msg", zap.Error(err))
		return nil, err
	}

	c := pb.NewCIAddonClient(conn)
	client := ciAddonClient{
		port:       port,
		log:        log,
		conn:       conn,
		grpcClient: c,
	}
	return &client, nil
}

func (c *ciAddonClient) CloseConn() error {
	if err := c.conn.Close(); err != nil {
		return err
	}
	return nil
}

func (c *ciAddonClient) Client() pb.CIAddonClient {
	return c.grpcClient
}
