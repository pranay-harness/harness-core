package grpc

import (
	"fmt"
	"net"
	"time"

	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
	"google.golang.org/grpc"
)

const (
	hardStopWaitTimeout = 10
)

//go:generate mockgen -source server.go -package=grpc -destination mocks/server_mock.go LiteEngineServer

//LiteEngineServer implements a GRPC server that listens to messages from main lite engine
type LiteEngineServer interface {
	Start()
	Stop()
}

type liteEngineServer struct {
	port        uint
	listener    net.Listener
	grpcServer  *grpc.Server
	log         *zap.SugaredLogger
	stopCh      chan bool
	stepLogPath string
	tmpFilePath string
}

//NewLiteEngineServer constructs a new LiteEngineServer
func NewLiteEngineServer(port uint, stepLogPath, tmpFilePath string, log *zap.SugaredLogger) (LiteEngineServer, error) {
	listener, err := net.Listen("tcp", fmt.Sprintf(":%d", port))
	if err != nil {
		return nil, err
	}

	stopCh := make(chan bool, 1)
	server := liteEngineServer{
		port:        port,
		log:         log,
		stopCh:      stopCh,
		stepLogPath: stepLogPath,
		tmpFilePath: tmpFilePath,
	}
	server.grpcServer = grpc.NewServer()
	server.listener = listener
	return &server, nil
}

//Start signals the GRPC server to begin serving on the configured port
func (s *liteEngineServer) Start() {
	pb.RegisterLiteEngineServer(s.grpcServer, NewLiteEngineHandler(s.stepLogPath, s.tmpFilePath, s.stopCh, s.log))
	err := s.grpcServer.Serve(s.listener)
	if err != nil {
		s.log.Fatalw("error starting gRPC server", zap.Error(err))
	}
}

//Stop method waits for signal to stop the server and stops GRPC server upon receiving it
func (s *liteEngineServer) Stop() {
	<-s.stopCh
	s.log.Infow("Initiating shutdown of CI lite engine server")
	if s.grpcServer != nil {
		// Hard stop the GRPC server if it doesn't gracefully shut down within hardStopWaitTimeout.
		go func() {
			time.Sleep(hardStopWaitTimeout * time.Second)
			s.log.Infow("Initiating hard shutdown of CI lite engine server")
			s.grpcServer.Stop()
		}()

		s.log.Infow("Gracefully shutting down CI lite engine server")
		s.grpcServer.GracefulStop()
	}
}
