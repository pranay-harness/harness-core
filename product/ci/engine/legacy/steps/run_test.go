// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

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
	so := make(map[string]*output.StepOutput)
	so["step1"] = &output.StepOutput{}
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

	executor := NewRunStep(step, tmpPath, so, log.Sugar())
	o, n, err := executor.Run(ctx)
	assert.Nil(t, err)
	assert.Equal(t, o.Output.Variables[outputKey], outputVal)
	assert.Equal(t, n, numRetries)
}
