package main

import (
	"encoding/base64"
	"fmt"
	"os"
	"testing"

	"github.com/golang/protobuf/proto"
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
	executeStage = func(input, logpath, tmpFilePath string, log *zap.SugaredLogger) {
		fmt.Println("Hello world")
	}

	main()
}

func TestMainEmptyStep(t *testing.T) {
	defer func() {
		args.Stage = nil
		args.Step = nil
	}()

	step := &pb.Step{
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
