package grpc

import (
	"fmt"
	"net"
	"time"

	pb "github.com/wings-software/portal/product/ci/scm/proto"
	"go.uber.org/zap"
	"google.golang.org/grpc"
)

const (
	hardStopWaitTimeout = 10
)

//go:generate mockgen -source server.go -package=grpc -destination mocks/server_mock.go SCMServer

//SCMServer implements a GRPC server
type SCMServer interface {
	Start()
	Stop()
}

type scmServer struct {
	port       uint
	listener   net.Listener
	grpcServer *grpc.Server
	log        *zap.SugaredLogger
	stopCh     chan bool
}

//NewSCMServer constructs a new SCMServer
func NewSCMServer(port uint, log *zap.SugaredLogger) (SCMServer, error) {
	listener, err := net.Listen("tcp", fmt.Sprintf(":%d", port))
	if err != nil {
		return nil, err
	}

	stopCh := make(chan bool, 1)
	server := scmServer{
		port:   port,
		log:    log,
		stopCh: stopCh,
	}
	server.grpcServer = grpc.NewServer()
	server.listener = listener
	return &server, nil
}

//Start signals the GRPC server to begin serving on the configured port
func (s *scmServer) Start() {
	pb.RegisterSCMServer(s.grpcServer, NewSCMHandler(s.stopCh, s.log))
	err := s.grpcServer.Serve(s.listener)
	if err != nil {
		s.log.Fatalw("error starting gRPC server", zap.Error(err))
	}
}

//Stop method waits for signal to stop the server and stops GRPC server upon receiving it
func (s *scmServer) Stop() {
	<-s.stopCh
	s.log.Infow("Initiating shutdown of CI scm server")
	if s.grpcServer != nil {
		// Hard stop the GRPC server if it doesn't gracefully shut down within hardStopWaitTimeout.
		go func() {
			time.Sleep(hardStopWaitTimeout * time.Second)
			s.log.Infow("Initiating hard shutdown of CI scm server")
			s.grpcServer.Stop()
		}()

		s.log.Infow("Gracefully shutting down CI scm server")
		s.grpcServer.GracefulStop()
	}
}
