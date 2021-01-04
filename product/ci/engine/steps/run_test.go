package steps

import (
	"context"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/pkg/errors"
	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/logs"
	caddon "github.com/wings-software/portal/product/ci/addon/grpc/client"
	amgrpc "github.com/wings-software/portal/product/ci/addon/grpc/client/mocks"
	addonpb "github.com/wings-software/portal/product/ci/addon/proto"
	"github.com/wings-software/portal/product/ci/engine/output"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
	"google.golang.org/grpc"
)

type mockClient struct {
	response *addonpb.ExecuteStepResponse
	err      error
}

func (c *mockClient) ExecuteStep(ctx context.Context, in *addonpb.ExecuteStepRequest, opts ...grpc.CallOption) (*addonpb.ExecuteStepResponse, error) {
	return c.response, c.err
}

func (c *mockClient) SignalStop(ctx context.Context, in *addonpb.SignalStopRequest, opts ...grpc.CallOption) (*addonpb.SignalStopResponse, error) {
	return nil, nil
}

func TestRunStepValidate(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	e := runStep{
		log: log.Sugar(),
	}
	err := e.validate()
	assert.NotNil(t, err)

	e = runStep{
		command: "ls",
		log:     log.Sugar(),
	}
	err = e.validate()
	assert.NotNil(t, err)

	e = runStep{
		command:       "ls",
		containerPort: uint32(8000),
		log:           log.Sugar(),
	}
	err = e.validate()
	assert.Nil(t, err)
}

func TestRunValidateErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	tmpPath := "/tmp/"
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	executor := NewRunStep(nil, tmpPath, nil, log.Sugar())
	o, numRetries, err := executor.Run(ctx)
	assert.NotNil(t, err)
	assert.Nil(t, o)
	assert.Equal(t, numRetries, int32(1))
}

// Client creation failing
func TestRunExecuteClientErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()
	tmpPath := "/tmp/"
	port := uint32(8000)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	step := &pb.UnitStep{
		Id: "test",
		Step: &pb.UnitStep_Run{
			Run: &pb.RunStep{
				Command:       "cd . ; ls",
				ContainerPort: port,
			},
		},
	}

	oldClient := newAddonClient
	defer func() { newAddonClient = oldClient }()
	newAddonClient = func(port uint, log *zap.SugaredLogger) (caddon.AddonClient, error) {
		return nil, errors.New("client create error")
	}

	executor := NewRunStep(step, tmpPath, nil, log.Sugar())
	o, numRetries, err := executor.Run(ctx)
	assert.NotNil(t, err)
	assert.Nil(t, o)
	assert.Equal(t, numRetries, int32(1))
}

// Failed to send GRPC request
func TestRunExecuteServerErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()
	tmpPath := "/tmp/"
	port := uint32(8000)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	step := &pb.UnitStep{
		Id: "test",
		Step: &pb.UnitStep_Run{
			Run: &pb.RunStep{
				Command:       "cd . ; ls",
				ContainerPort: port,
			},
		},
	}

	c := &mockClient{
		response: nil,
		err:      errors.New("server not running"),
	}
	mClient := amgrpc.NewMockAddonClient(ctrl)
	mClient.EXPECT().CloseConn().Return(nil)
	mClient.EXPECT().Client().Return(c)

	oldClient := newAddonClient
	defer func() { newAddonClient = oldClient }()
	newAddonClient = func(port uint, log *zap.SugaredLogger) (caddon.AddonClient, error) {
		return mClient, nil
	}

	executor := NewRunStep(step, tmpPath, nil, log.Sugar())
	o, numRetries, err := executor.Run(ctx)
	assert.NotNil(t, err)
	assert.Nil(t, o)
	assert.Equal(t, numRetries, int32(1))
}

// Success
func TestRunExecuteSuccess(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()
	tmpPath := "/tmp/"
	port := uint32(8000)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	step := &pb.UnitStep{
		Id: "test",
		Step: &pb.UnitStep_Run{
			Run: &pb.RunStep{
				Command:       "cd . ; ls",
				ContainerPort: port,
			},
		},
	}

	outputKey := "foo"
	outputVal := "bar"
	numRetries := int32(3)
	c := &mockClient{
		response: &addonpb.ExecuteStepResponse{
			Output:     map[string]string{outputKey: outputVal},
			NumRetries: numRetries,
		},
		err: nil,
	}
	mClient := amgrpc.NewMockAddonClient(ctrl)
	mClient.EXPECT().CloseConn().Return(nil)
	mClient.EXPECT().Client().Return(c)

	oldClient := newAddonClient
	defer func() { newAddonClient = oldClient }()
	newAddonClient = func(port uint, log *zap.SugaredLogger) (caddon.AddonClient, error) {
		return mClient, nil
	}

	executor := NewRunStep(step, tmpPath, nil, log.Sugar())
	o, n, err := executor.Run(ctx)
	assert.Nil(t, err)
	assert.Equal(t, o.Output[outputKey], outputVal)
	assert.Equal(t, n, numRetries)
}

func TestRunStepResolveJEXL(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	jCmd1 := "${step1.output.foo}"
	cmd1Val := "bar"

	tests := []struct {
		name        string
		command     string
		resolvedCmd string
		jexlEvalRet map[string]string
		jexlEvalErr error
		expectedErr bool
	}{
		{
			name:        "jexl evaluate error",
			command:     jCmd1,
			jexlEvalRet: nil,
			jexlEvalErr: errors.New("evaluation failed"),
			expectedErr: true,
		},
		{
			name:        "jexl successfully evaluated",
			command:     jCmd1,
			jexlEvalRet: map[string]string{jCmd1: cmd1Val},
			jexlEvalErr: nil,
			resolvedCmd: cmd1Val,
			expectedErr: false,
		},
	}
	oldJEXLEval := evaluateJEXL
	defer func() { evaluateJEXL = oldJEXLEval }()
	for _, tc := range tests {
		s := &runStep{
			command: tc.command,
			log:     log.Sugar(),
		}
		// Initialize a mock CI addon
		evaluateJEXL = func(ctx context.Context, stepID string, expressions []string, o output.StageOutput,
			log *zap.SugaredLogger) (map[string]string, error) {
			return tc.jexlEvalRet, tc.jexlEvalErr
		}
		got := s.resolveJEXL(ctx)
		if tc.expectedErr == (got == nil) {
			t.Fatalf("%s: expected error: %v, got: %v", tc.name, tc.expectedErr, got)
		}

		if got == nil {
			assert.Equal(t, s.command, tc.resolvedCmd)
		}
	}
}
