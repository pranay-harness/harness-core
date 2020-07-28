package grpc

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/logs"
	"go.uber.org/zap"
	"google.golang.org/grpc"
)

func TestServerFailToListen(t *testing.T) {
	tmpPath := "/tmp"
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	_, err := NewLiteEngineServer(65536, tmpPath, tmpPath, log.Sugar())
	assert.Error(t, err)
}

func TestStopNilServer(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	stopCh := make(chan bool, 1)
	s := &liteEngineServer{
		port:   65534,
		log:    log.Sugar(),
		stopCh: stopCh,
	}
	stopCh <- true
	s.Stop()
}

func TestStopRunningServer(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	stopCh := make(chan bool, 1)
	s := &liteEngineServer{
		port:       65533,
		grpcServer: grpc.NewServer(),
		log:        log.Sugar(),
		stopCh:     stopCh,
	}
	stopCh <- true
	s.Stop()
}

func TestNewLiteEngineServer(t *testing.T) {
	tmpPath := "/tmp"
	port := uint(5001)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	_, err := NewLiteEngineServer(port, tmpPath, tmpPath, log.Sugar())
	assert.Equal(t, err, nil)
}
