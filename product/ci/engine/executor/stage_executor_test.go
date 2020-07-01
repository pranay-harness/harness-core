package executor

import (
	"encoding/base64"
	"testing"

	"github.com/golang/protobuf/proto"
	"github.com/wings-software/portal/commons/go/lib/logs"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

func getEncodedStageProto(t *testing.T, execution *pb.Execution) string {
	data, err := proto.Marshal(execution)
	if err != nil {
		t.Fatalf("marshaling error: %v", err)
	}
	return base64.StdEncoding.EncodeToString(data)
}

func TestStageRun(t *testing.T) {
	tmpFilePath := "/tmp"
	executionProto1 := &pb.Execution{
		Steps: []*pb.Step{
			{
				Id: "test1",
				Step: &pb.Step_Run{
					Run: &pb.RunStep{},
				},
			},
		},
	}
	executionProto2 := &pb.Execution{
		Steps: []*pb.Step{
			{
				Step: &pb.Step_Run{
					Run: &pb.RunStep{},
				},
			},
		},
	}

	encodedExecutionProto1 := getEncodedStageProto(t, executionProto1)
	encodedExecutionProto2 := getEncodedStageProto(t, executionProto2)
	incorrectBase64Enc := "x"
	invalidStageEnc := "YWJjZA=="
	logPath := "/a/b"
	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	tests := []struct {
		name         string
		encodedStage string
		logPath      string
		expectedErr  bool
	}{
		{
			name:         "incorrect encoded stage",
			encodedStage: incorrectBase64Enc,
			logPath:      logPath,
			expectedErr:  true,
		},
		{
			name:         "stage encoding not in execution proto format",
			encodedStage: invalidStageEnc,
			logPath:      logPath,
			expectedErr:  true,
		},
		{
			name:         "empty stage success",
			encodedStage: "",
			logPath:      logPath,
			expectedErr:  false,
		},
		{
			name:         "stage with error in run step",
			encodedStage: encodedExecutionProto1,
			logPath:      logPath,
			expectedErr:  true,
		},
		{
			name:         "step ID is not set in stage",
			encodedStage: encodedExecutionProto2,
			logPath:      logPath,
			expectedErr:  true,
		},
	}
	for _, tc := range tests {
		e := NewStageExecutor(tc.encodedStage, tc.logPath, tmpFilePath, log.Sugar())
		got := e.Run()
		if tc.expectedErr == (got == nil) {
			t.Fatalf("%s: expected error: %v, got: %v", tc.name, tc.expectedErr, got)
		}
	}
}

func TestExecuteStage(t *testing.T) {
	logPath := "/a/b"
	tmpFilePath := "/tmp"
	emptyStage := ""
	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	ExecuteStage(emptyStage, logPath, tmpFilePath, log.Sugar())
}
