package grpc

import (
	"fmt"
	pb "github.com/wings-software/portal/product/ci/addon/proto"
	"go.uber.org/zap"
	"google.golang.org/grpc"
)

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

func NewCIAddonClient(port uint, log *zap.SugaredLogger) (CIAddonClient, error) {
	// Create a connection on the port and disable transport security (TLS/SSL)
	conn, err := grpc.Dial(fmt.Sprintf(":%d", port), grpc.WithInsecure())
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
