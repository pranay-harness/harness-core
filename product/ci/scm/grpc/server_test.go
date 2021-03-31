package grpc

import (
	"io/ioutil"
	"log"
	"os"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/logs"
	"go.uber.org/zap"
	"google.golang.org/grpc"
)

func TestServerFailToListen(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	_, err := NewSCMServer(65536, "", log.Sugar())
	assert.Error(t, err)
}

func TestStopNilServer(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	stopCh := make(chan bool, 1)
	s := &scmServer{
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
	s := &scmServer{
		port:       65533,
		grpcServer: grpc.NewServer(),
		log:        log.Sugar(),
		stopCh:     stopCh,
	}
	stopCh <- true
	s.Stop()
}

func TestNewSCMServer(t *testing.T) {
	port := uint(5000)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	_, err := NewSCMServer(port, "", log.Sugar())
	assert.Nil(t, err)
}

func TestNewSCMServerFailSocket(t *testing.T) {
	// create a temp file then remove it.
	file, err := ioutil.TempFile("/tmp", "prefix")
	if err != nil {
		log.Fatal(err)
	}
	os.Remove(file.Name())
	// use that tempfile name for the socket then remove it.
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	_, err = NewSCMServer(8080, file.Name(), log.Sugar())
	assert.Nil(t, err)

	os.Remove(file.Name())
}
