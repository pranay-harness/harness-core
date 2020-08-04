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
	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/logs"
	addon "github.com/wings-software/portal/product/ci/addon/grpc"
	caddon "github.com/wings-software/portal/product/ci/addon/grpc/client"
	mgrpc "github.com/wings-software/portal/product/ci/addon/grpc/client/mocks"
	addonpb "github.com/wings-software/portal/product/ci/addon/proto"
	cengine "github.com/wings-software/portal/product/ci/engine/grpc/client"
	emgrpc "github.com/wings-software/portal/product/ci/engine/grpc/client/mocks"
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
	lis = bufconn.Listen(bufSize)

	addonServer := addon.NewAddonHandler(stopCh, log.Sugar())
	s1 := grpc.NewServer()
	addonpb.RegisterAddonServer(s1, addonServer)
	go func() {
		if err := s1.Serve(lis); err != nil {
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
				Step: &pb.Step_Unit{
					Unit: &pb.UnitStep{
						Id: "test1",
						Step: &pb.UnitStep_Run{
							Run: &pb.RunStep{},
						},
					},
				},
			},
		},
	}
	executionProto2 := &pb.Execution{
		Steps: []*pb.Step{
			{
				Step: &pb.Step_Unit{
					Unit: &pb.UnitStep{
						Step: &pb.UnitStep_Run{
							Run: &pb.RunStep{},
						},
					},
				},
			},
		},
	}
	executionProto3 := &pb.Execution{
		Steps: []*pb.Step{
			{
				Step: &pb.Step_Parallel{
					Parallel: &pb.ParallelStep{},
				},
			},
		},
	}

	encodedExecutionProto1 := getEncodedStageProto(t, executionProto1)
	encodedExecutionProto2 := getEncodedStageProto(t, executionProto2)
	encodedExecutionProto3 := getEncodedStageProto(t, executionProto3)
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
		{
			name:         "parallel step ID is not set in stage",
			encodedStage: encodedExecutionProto3,
			logPath:      logPath,
			expectedErr:  true,
		},
	}

	oldClient := newAddonClient
	defer func() { newAddonClient = oldClient }()
	// Initialize a mock CI addon
	newAddonClient = func(port uint, log *zap.SugaredLogger) (caddon.AddonClient, error) {
		return nil, errors.New("Could not create client")
	}

	for _, tc := range tests {
		e := NewStageExecutor(tc.encodedStage, tc.logPath, tmpFilePath, nil, false, log.Sugar())
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
	ports := []uint{9006}
	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	oldAClient := newAddonClient
	defer func() { newAddonClient = oldAClient }()
	// Initialize a mock CI addon
	newAddonClient = func(port uint, log *zap.SugaredLogger) (caddon.AddonClient, error) {
		return nil, errors.New("Could not create client")
	}

	oldEClient := newLiteEngineClient
	defer func() { newLiteEngineClient = oldEClient }()
	newLiteEngineClient = func(port uint, log *zap.SugaredLogger) (cengine.LiteEngineClient, error) {
		return nil, errors.New("Could not create client")
	}

	ExecuteStage(emptyStage, logPath, tmpFilePath, ports, false, log.Sugar())
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
	client := addonpb.NewAddonClient(conn)
	fmt.Println(client)
	oldClient := newAddonClient
	defer func() { newAddonClient = oldClient }()
	// Initialize a mock CI addon
	mockClient := mgrpc.NewMockAddonClient(ctrl)
	mockClient.EXPECT().CloseConn().Return(nil)
	mockClient.EXPECT().Client().Return(client)
	newAddonClient = func(port uint, log *zap.SugaredLogger) (caddon.AddonClient, error) {
		return mockClient, nil
	}

	logPath := "/a/b"
	tmpFilePath := "/tmp"
	emptyStage := ""
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	executor := &stageExecutor{
		log:          log.Sugar(),
		stepLogPath:  logPath,
		tmpFilePath:  tmpFilePath,
		encodedStage: emptyStage,
	}
	err = executor.stopAddonServer(ctx)
	assert.Equal(t, err, nil)
}

func TestStopEngineServerError(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	conn, err := grpc.DialContext(ctx, "bufnet", grpc.WithContextDialer(bufDialer), grpc.WithInsecure())
	if err != nil {
		t.Fatalf("Failed to dial bufnet: %v", err)
	}
	defer conn.Close()
	client := pb.NewLiteEngineClient(conn)
	oldClient := newLiteEngineClient
	defer func() { newLiteEngineClient = oldClient }()
	// Initialize a mock Lite engine
	mockClient := emgrpc.NewMockLiteEngineClient(ctrl)
	mockClient.EXPECT().CloseConn().Return(nil)
	mockClient.EXPECT().Client().Return(client)
	newLiteEngineClient = func(port uint, log *zap.SugaredLogger) (cengine.LiteEngineClient, error) {
		return mockClient, nil
	}

	logPath := "/a/b"
	tmpFilePath := "/tmp"
	emptyStage := ""
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	executor := &stageExecutor{
		log:          log.Sugar(),
		stepLogPath:  logPath,
		tmpFilePath:  tmpFilePath,
		encodedStage: emptyStage,
	}
	err = executor.stopWorkerLiteEngine(ctx, uint(3))
	assert.NotEqual(t, err, nil)
}
