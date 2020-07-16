package executor

import (
	"context"
	"encoding/base64"
	"fmt"
	"net"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/golang/protobuf/proto"
	"github.com/pkg/errors"
	"github.com/wings-software/portal/commons/go/lib/logs"
	addon "github.com/wings-software/portal/product/ci/addon/grpc"
	addonpb "github.com/wings-software/portal/product/ci/addon/proto"
	egrpc "github.com/wings-software/portal/product/ci/engine/grpc"
	mgrpc "github.com/wings-software/portal/product/ci/engine/grpc/mocks"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
	"google.golang.org/grpc"
	"google.golang.org/grpc/test/bufconn"
)

const bufSize = 1024 * 1024

var lis *bufconn.Listener

func init() {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	stopCh := make(chan bool)
	server := addon.NewCIAddonHandler(stopCh, log.Sugar())

	lis = bufconn.Listen(bufSize)
	s := grpc.NewServer()
	addonpb.RegisterCIAddonServer(s, server)
	go func() {
		if err := s.Serve(lis); err != nil {
			panic(fmt.Sprintf("Server exited with error: %d", err))
		}
	}()
}

func bufDialer(context.Context, string) (net.Conn, error) {
	return lis.Dial()
}

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

	oldClient := newCIAddonClient
	defer func() { newCIAddonClient = oldClient }()
	// Initialize a mock CI addon
	newCIAddonClient = func(port uint, log *zap.SugaredLogger) (egrpc.CIAddonClient, error) {
		return nil, errors.New("Could not create client")
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

	oldClient := newCIAddonClient
	defer func() { newCIAddonClient = oldClient }()
	// Initialize a mock CI addon
	newCIAddonClient = func(port uint, log *zap.SugaredLogger) (egrpc.CIAddonClient, error) {
		return nil, errors.New("Could not create client")
	}

	ExecuteStage(emptyStage, logPath, tmpFilePath, log.Sugar())
}

func TestStopAddonServer(t *testing.T) {
	// ctx := context.Background()
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	conn, err := grpc.DialContext(ctx, "bufnet", grpc.WithContextDialer(bufDialer), grpc.WithInsecure())
	if err != nil {
		t.Fatalf("Failed to dial bufnet: %v", err)
	}
	defer conn.Close()
	client := addonpb.NewCIAddonClient(conn)
	fmt.Println(client)
	oldClient := newCIAddonClient
	defer func() { newCIAddonClient = oldClient }()
	// Initialize a mock CI addon
	mockClient := mgrpc.NewMockCIAddonClient(ctrl)
	mockClient.EXPECT().CloseConn().Return(nil)
	mockClient.EXPECT().Client().Return(client)
	newCIAddonClient = func(port uint, log *zap.SugaredLogger) (egrpc.CIAddonClient, error) {
		return mockClient, nil
	}

	logPath := "/a/b"
	tmpFilePath := "/tmp"
	emptyStage := ""
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	executor := &stageExecutor{log.Sugar(), logPath, tmpFilePath, emptyStage}
	executor.stopAddonServer(ctx)
}
