package main

import (
	"encoding/base64"
	"os"
	"testing"

	"github.com/golang/protobuf/proto"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
)

func Test_Main_EmptyStage(t *testing.T) {
	execution := &pb.Execution{}
	data, err := proto.Marshal(execution)
	if err != nil {
		t.Fatalf("marshaling error: %v", err)
	}
	encoded := base64.StdEncoding.EncodeToString(data)
	logPath := "/a/b"

	oldArgs := os.Args
	defer func() { os.Args = oldArgs }()
	os.Args = []string{"engine", "--stage", encoded, "--logpath", logPath}

	main()
}
