package main

import (
	"context"
	"encoding/base64"
	"fmt"
	"os"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/golang/protobuf/proto"
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
	executeStage = func(input, logpath, tmpFilePath string, workerPorts []uint, debug bool, log *zap.SugaredLogger) {
		fmt.Println("Hello world")
	}

	main()
}

func TestMainEmptyStageMultiWorkers(t *testing.T) {
	defer func() {
		args.Stage = nil
		args.Step = nil
	}()
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
	executeStage = func(input, logpath, tmpFilePath string, workerPorts []uint, debug bool, log *zap.SugaredLogger) {
		fmt.Println("Hello world")
	}

	main()
}

func TestMainEmptyStep(t *testing.T) {
	defer func() {
		args.Stage = nil
		args.Step = nil
	}()

	step := &pb.UnitStep{
		Id: "test",
	}
	data, err := proto.Marshal(step)
	if err != nil {
		t.Fatalf("marshaling error: %v", err)
	}
	encoded := base64.StdEncoding.EncodeToString(data)
	logPath := "/a/b"
	tmpPath := "/tmp"

	os.Args = []string{"engine", "step", "--input", encoded, "--logpath", logPath, "--tmppath", tmpPath}
	main()
}

func Test_MainWithGrpc(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

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
