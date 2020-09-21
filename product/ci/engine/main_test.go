package main

import (
	"context"
	"encoding/base64"
	"fmt"
	"os"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/golang/protobuf/proto"
	"github.com/wings-software/portal/commons/go/lib/logs"
	"github.com/wings-software/portal/product/ci/engine/grpc"
	mgrpcserver "github.com/wings-software/portal/product/ci/engine/grpc/mocks"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

func TestMainEmptyStage(t *testing.T) {
	defer func() {
		args.Stage = nil
		args.Step = nil
	}()

	oldLogger := newRemoteLogger
	defer func() { newRemoteLogger = oldLogger }()
	newRemoteLogger = func(key string) (rl *logs.RemoteLogger, err error) {
		log, _ := logs.GetObservedLogger(zap.InfoLevel)
		return &logs.RemoteLogger{BaseLogger: log.Sugar(), Writer: logs.NopWriter()}, nil
	}

	execution := &pb.Execution{}
	data, err := proto.Marshal(execution)
	if err != nil {
		t.Fatalf("marshaling error: %v", err)
	}
	encoded := base64.StdEncoding.EncodeToString(data)
	logPath := "/a/b"
	tmpPath := "/tmp"

	oldArgs := os.Args
	defer func() { os.Args = oldArgs }()
	os.Args = []string{"engine", "stage", "--input", encoded, "--logpath", logPath, "--tmppath", tmpPath}

	oldExecuteStage := executeStage
	defer func() { executeStage = oldExecuteStage }()
	executeStage = func(input, logpath, tmpFilePath string, workerPorts []uint, debug bool, log *zap.SugaredLogger) error {
		fmt.Println("Hello world")
		return nil
	}

	main()
}

func TestMainEmptyStageMultiWorkers(t *testing.T) {
	defer func() {
		args.Stage = nil
		args.Step = nil
	}()

	oldLogger := newRemoteLogger
	defer func() { newRemoteLogger = oldLogger }()
	newRemoteLogger = func(key string) (rl *logs.RemoteLogger, err error) {
		log, _ := logs.GetObservedLogger(zap.InfoLevel)
		return &logs.RemoteLogger{BaseLogger: log.Sugar(), Writer: logs.NopWriter()}, nil
	}

	execution := &pb.Execution{}
	data, err := proto.Marshal(execution)
	if err != nil {
		t.Fatalf("marshaling error: %v", err)
	}
	encoded := base64.StdEncoding.EncodeToString(data)
	logPath := "/a/b"
	tmpPath := "/tmp"

	oldArgs := os.Args
	defer func() { os.Args = oldArgs }()
	os.Args = []string{"engine", "stage", "--input", encoded, "--logpath", logPath, "--tmppath", tmpPath, "--ports", "9001", "9002", "--debug"}

	oldExecuteStage := executeStage
	defer func() { executeStage = oldExecuteStage }()
	executeStage = func(input, logpath, tmpFilePath string, workerPorts []uint, debug bool, log *zap.SugaredLogger) error {
		fmt.Println("Hello world")
		return nil
	}

	main()
}

func TestMainEmptyStep(t *testing.T) {
	defer func() {
		args.Stage = nil
		args.Step = nil
	}()

	r := &pb.ExecuteStepRequest{
		Step: &pb.UnitStep{
			Id: "test",
		},
		AccountId: "test",
	}
	oldLogger := newRemoteLogger
	defer func() { newRemoteLogger = oldLogger }()
	newRemoteLogger = func(key string) (rl *logs.RemoteLogger, err error) {
		log, _ := logs.GetObservedLogger(zap.InfoLevel)
		return &logs.RemoteLogger{BaseLogger: log.Sugar(), Writer: logs.NopWriter()}, nil
	}
	data, err := proto.Marshal(r)
	if err != nil {
		t.Fatalf("marshaling error: %v", err)
	}
	encoded := base64.StdEncoding.EncodeToString(data)
	logPath := "/a/b"
	tmpPath := "/tmp"

	oldExecuteStep := executeStep
	defer func() { executeStep = oldExecuteStep }()
	executeStep = func(input, logpath, tmpFilePath string, log *zap.SugaredLogger) error {
		return nil
	}

	os.Args = []string{"engine", "step", "--input", encoded, "--logpath", logPath, "--tmppath", tmpPath}
	main()
}

func Test_MainWithGrpc(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	oldLogger := newRemoteLogger
	defer func() { newRemoteLogger = oldLogger }()
	newRemoteLogger = func(key string) (rl *logs.RemoteLogger, err error) {
		log, _ := logs.GetObservedLogger(zap.InfoLevel)
		return &logs.RemoteLogger{BaseLogger: log.Sugar(), Writer: logs.NopWriter()}, nil
	}

	logPath := "/a/b"
	tmpPath := "/tmp"
	mockServer := mgrpcserver.NewMockLiteEngineServer(ctrl)
	s := func(uint, string, string, *zap.SugaredLogger) (grpc.LiteEngineServer, error) {
		return mockServer, nil
	}

	oldArgs := os.Args
	defer func() { os.Args = oldArgs }()
	os.Args = []string{"engine", "server", "--port", "20001", "--logpath", logPath, "--tmppath", tmpPath}

	oldEngineServer := liteEngineServer
	defer func() { liteEngineServer = oldEngineServer }()
	liteEngineServer = s

	mockServer.EXPECT().Start()
	mockServer.EXPECT().Stop().AnyTimes()
	main()
}
