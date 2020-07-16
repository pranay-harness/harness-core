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
	stopCh     chan bool
}

//NewCIAddonServer constructs a new CIAddonServer
func NewCIAddonServer(port uint, log *zap.SugaredLogger) (CIAddonServer, error) {
	listener, err := net.Listen("tcp", fmt.Sprintf(":%d", port))
	if err != nil {
		return nil, err
	}

	stopCh := make(chan bool, 1)
	server := ciAddonServer{
		port:   port,
		log:    log,
		stopCh: stopCh,
	}
	server.grpcServer = grpc.NewServer()
	server.listener = listener
	return &server, nil
}

//Start signals the GRPC server to begin serving on the configured port
func (s *ciAddonServer) Start() {
	pb.RegisterCIAddonServer(s.grpcServer, NewCIAddonHandler(s.stopCh, s.log))
	err := s.grpcServer.Serve(s.listener)
	if err != nil {
		s.log.Fatalw("error starting gRPC server", zap.Error(err))
	}
}

//Stop method waits for signal to stop the server and stops GRPC server upon receiving it
func (s *ciAddonServer) Stop() {
	<-s.stopCh
	s.log.Infow("Initiating shutdown of CI addon server")
	if s.grpcServer != nil {
		// Hard stop the GRPC server if it doesn't gracefully shut down within hardStopWaitTimeout.
		go func() {
			time.Sleep(hardStopWaitTimeout * time.Second)
			s.log.Infow("Initiating hard shutdown of CI addon server")
			s.grpcServer.Stop()
		}()

		s.log.Infow("Gracefully shutting down CI addon server")
		s.grpcServer.GracefulStop()
	}
}
