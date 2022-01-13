// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

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
	lg, _ := logs.GetObservedLogger(zap.InfoLevel)
	_, err := NewSCMServer(65536, "", lg.Sugar())
	assert.Error(t, err)
}

func TestStopNilServer(t *testing.T) {
	lg, _ := logs.GetObservedLogger(zap.InfoLevel)
	stopCh := make(chan bool, 1)
	s := &scmServer{
		port:   65534,
		log:    lg.Sugar(),
		stopCh: stopCh,
	}
	stopCh <- true
	s.Stop()
}

func TestStopRunningServer(t *testing.T) {
	lg, _ := logs.GetObservedLogger(zap.InfoLevel)
	stopCh := make(chan bool, 1)
	s := &scmServer{
		port:       65533,
		grpcServer: grpc.NewServer(),
		log:        lg.Sugar(),
		stopCh:     stopCh,
	}
	stopCh <- true
	s.Stop()
}

func TestNewSCMServer(t *testing.T) {
	port := uint(5000)
	lg, _ := logs.GetObservedLogger(zap.InfoLevel)
	_, err := NewSCMServer(port, "", lg.Sugar())
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
	lg, _ := logs.GetObservedLogger(zap.InfoLevel)
	_, err = NewSCMServer(8080, file.Name(), lg.Sugar())
	assert.Nil(t, err)

	os.Remove(file.Name())
}
