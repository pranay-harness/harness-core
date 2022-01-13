// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

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

//go:generate mockgen -source server.go -package=grpc -destination mocks/server_mock.go AddonServer

//AddonServer implements a GRPC server that listens to messages from lite engine
type AddonServer interface {
	Start() error
	Stop()
}

type addonServer struct {
	port       uint
	listener   net.Listener
	grpcServer *grpc.Server
	logMetrics bool
	log        *zap.SugaredLogger
	stopCh     chan bool
}

//NewAddonServer constructs a new AddonServer
func NewAddonServer(port uint, logMetrics bool, log *zap.SugaredLogger) (AddonServer, error) {
	listener, err := net.Listen("tcp", fmt.Sprintf(":%d", port))
	if err != nil {
		return nil, err
	}

	stopCh := make(chan bool, 1)
	server := addonServer{
		port:       port,
		logMetrics: logMetrics,
		log:        log,
		stopCh:     stopCh,
	}
	server.grpcServer = grpc.NewServer()
	server.listener = listener
	return &server, nil
}

//Start signals the GRPC server to begin serving on the configured port
func (s *addonServer) Start() error {
	pb.RegisterAddonServer(s.grpcServer, NewAddonHandler(s.stopCh, s.logMetrics, s.log))
	err := s.grpcServer.Serve(s.listener)
	if err != nil {
		s.log.Errorw("error starting gRPC server", "error_msg", zap.Error(err))
		return err
	}
	return nil
}

//Stop method waits for signal to stop the server and stops GRPC server upon receiving it
func (s *addonServer) Stop() {
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
