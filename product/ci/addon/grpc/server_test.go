package grpc

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/logs"
	"go.uber.org/zap"
	"google.golang.org/grpc"
)

func TestServerFailToListen(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	_, err := NewCIAddonServer(65536, log.Sugar())
	assert.Error(t, err)
}

func TestStopNilServer(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	s, err := NewCIAddonServer(65534, log.Sugar())
	s.Stop()
	assert.NoError(t, err)
}

func TestStopRunningServer(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	s := &ciAddonServer{
		port:       65533,
		grpcServer: grpc.NewServer(),
		log:        log.Sugar(),
	}
	s.Stop()
}
