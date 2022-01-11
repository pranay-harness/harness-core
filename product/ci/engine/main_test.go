// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package main

import (
	"context"
	"encoding/base64"
	"io"
	"os"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/golang/protobuf/proto"
	"github.com/wings-software/portal/commons/go/lib/logs"
	"github.com/wings-software/portal/product/ci/engine/grpc"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

type mockServer struct {
	err error
}

func (s *mockServer) Start() error { return s.err }
func (s *mockServer) Stop()        {}

func TestMainEmptyStage(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	defer func() {
		args.Stage = nil
	}()

	oldGetLogKey := getLogKey
	defer func() { getLogKey = oldGetLogKey }()
	getLogKey = func(keyID string) (string, error) {
		return "foo:bar", nil
	}

	oldLogger := newHTTPRemoteLogger
	defer func() { newHTTPRemoteLogger = oldLogger }()
	newHTTPRemoteLogger = func(key string) (rl *logs.RemoteLogger, err error) {
		log, _ := logs.GetObservedLogger(zap.InfoLevel)
		return &logs.RemoteLogger{BaseLogger: log.Sugar(), Writer: logs.NopWriter()}, nil
	}

	execution := &pb.Execution{}
	data, err := proto.Marshal(execution)
	if err != nil {
		t.Fatalf("marshaling error: %v", err)
	}
	encoded := base64.StdEncoding.EncodeToString(data)
	tmpPath := "/tmp"

	oldArgs := os.Args
	defer func() { os.Args = oldArgs }()
	os.Args = []string{"engine", "stage", "--input", encoded, "--tmppath", tmpPath}

	oldExecuteStage := executeStage
	defer func() { executeStage = oldExecuteStage }()
	executeStage = func(input, tmpFilePath string, svcPorts []uint, debug bool, log *zap.SugaredLogger) error {
		return nil
	}

	m := &mockServer{err: nil}
	oldServer := engineServer
	defer func() { engineServer = oldServer }()
	engineServer = func(port uint, log *zap.SugaredLogger, procWriter io.Writer) (grpc.EngineServer, error) {
		return m, nil
	}

	main()
}

func TestMainEmptyStageMultiWorkers(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	defer func() {
		args.Stage = nil
	}()

	oldGetLogKey := getLogKey
	defer func() { getLogKey = oldGetLogKey }()
	getLogKey = func(keyID string) (string, error) {
		return "foo:bar", nil
	}

	oldLogger := newHTTPRemoteLogger
	defer func() { newHTTPRemoteLogger = oldLogger }()
	newHTTPRemoteLogger = func(key string) (rl *logs.RemoteLogger, err error) {
		log, _ := logs.GetObservedLogger(zap.InfoLevel)
		return &logs.RemoteLogger{BaseLogger: log.Sugar(), Writer: logs.NopWriter()}, nil
	}

	execution := &pb.Execution{}
	data, err := proto.Marshal(execution)
	if err != nil {
		t.Fatalf("marshaling error: %v", err)
	}
	encoded := base64.StdEncoding.EncodeToString(data)
	tmpPath := "/tmp"

	oldArgs := os.Args
	defer func() { os.Args = oldArgs }()
	os.Args = []string{"engine", "stage", "--input", encoded, "--tmppath", tmpPath, "--debug"}

	oldExecuteStage := executeStage
	defer func() { executeStage = oldExecuteStage }()
	executeStage = func(input, tmpFilePath string, svcPorts []uint, debug bool, log *zap.SugaredLogger) error {
		return nil
	}

	m := &mockServer{err: nil}
	oldServer := engineServer
	defer func() { engineServer = oldServer }()
	engineServer = func(port uint, log *zap.SugaredLogger, procWriter io.Writer) (grpc.EngineServer, error) {
		return m, nil
	}

	main()
}
