package grpc

import (
	"fmt"
	"net"
	"time"

	pb "github.com/wings-software/portal/product/ci/addon/proto"
	"go.uber.org/zap"
	"google.golang.org/grpc"
)

const (
	hardStopWaitTimeout = 10
)

//go:generate mockgen -source server.go -package=grpc -destination mocks/server_mock.go CIAddonServer

//CIAddonServer implements a GRPC server that listens to messages from lite engine
type CIAddonServer interface {
	Start()
	Stop()
}

type ciAddonServer struct {
	port       uint
	listener   net.Listener
	grpcServer *grpc.Server
	log        *zap.SugaredLogger
}

//NewCIAddonServer constructs a new CIAddonServer
func NewCIAddonServer(port uint, log *zap.SugaredLogger) (CIAddonServer, error) {
	listener, err := net.Listen("tcp", fmt.Sprintf("localhost:%d", port))
	if err != nil {
		return nil, err
	}

	server := ciAddonServer{
		port: port,
		log:  log,
	}
	server.grpcServer = grpc.NewServer()
	server.listener = listener
	return &server, nil
}

//Start signals the GRPC server to begin serving on the configured port
func (s *ciAddonServer) Start() {
	pb.RegisterCIAddonServer(s.grpcServer, NewCIAddonHandler(s.log))
	err := s.grpcServer.Serve(s.listener)
	if err != nil {
		s.log.Fatalw("error starting gRPC server", zap.Error(err))
	}
}

//Stop signals the GRPC server to stop serving
func (s *ciAddonServer) Stop() {
	if s.grpcServer != nil {
		// Hard stop the GRPC server if it doesn't gracefully shut down within hardStopWaitTimeout.
		go func() {
			time.Sleep(hardStopWaitTimeout * time.Second)
			s.grpcServer.Stop()
		}()
		s.grpcServer.GracefulStop()
	}
}
