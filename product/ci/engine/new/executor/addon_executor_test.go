// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package executor

import (
	"bytes"
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
	err error
}

func (c *mockClient) ExecuteStep(ctx context.Context, in *addonpb.ExecuteStepRequest, opts ...grpc.CallOption) (*addonpb.ExecuteStepResponse, error) {
	return nil, c.err
}

func (c *mockClient) SignalStop(ctx context.Context, in *addonpb.SignalStopRequest, opts ...grpc.CallOption) (*addonpb.SignalStopResponse, error) {
	return nil, c.err
}

func TestAddonExecuteClientErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	port := uint32(8000)
	tmpPath := "/tmp"
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	step := &pb.UnitStep{
		Id: "test",
		Step: &pb.UnitStep_Plugin{
			Plugin: &pb.PluginStep{
				Image: "plugin/drone-git",
			},
		},
		ContainerPort: port,
	}

	oldClient := newAddonClient
	defer func() { newAddonClient = oldClient }()
	newAddonClient = func(port uint, log *zap.SugaredLogger) (caddon.AddonClient, error) {
		return nil, errors.New("client create error")
	}

	_, _, err := ExecuteStepOnAddon(ctx, step, tmpPath, log.Sugar(), new(bytes.Buffer))
	assert.NotNil(t, err)
}

// Failed to send GRPC request
func TestAddonExecuteServerErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	port := uint32(8000)
	tmpPath := "/tmp"
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	step := &pb.UnitStep{
		Id: "test",
		Step: &pb.UnitStep_Plugin{
			Plugin: &pb.PluginStep{
				Image: "plugin/drone-git",
			},
		},
		ContainerPort: port,
	}

	c := &mockClient{
		err: errors.New("server not running"),
	}
	mClient := amgrpc.NewMockAddonClient(ctrl)
	mClient.EXPECT().CloseConn().Return(nil)
	mClient.EXPECT().Client().Return(c)

	oldClient := newAddonClient
	defer func() { newAddonClient = oldClient }()
	newAddonClient = func(port uint, log *zap.SugaredLogger) (caddon.AddonClient, error) {
		return mClient, nil
	}

	_, _, err := ExecuteStepOnAddon(ctx, step, tmpPath, log.Sugar(), new(bytes.Buffer))
	assert.NotNil(t, err)
}

// Success
func TestAddonExecuteSuccess(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	port := uint32(8000)
	tmpPath := "/tmp"
	so := make(map[string]*output.StepOutput)
	so["step1"] = &output.StepOutput{}
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	step := &pb.UnitStep{
		Id: "test",
		Step: &pb.UnitStep_Plugin{
			Plugin: &pb.PluginStep{
				Image: "plugin/drone-git",
			},
		},
		ContainerPort: port,
	}

	c := &mockClient{
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

	_, _, err := ExecuteStepOnAddon(ctx, step, tmpPath, log.Sugar(), new(bytes.Buffer))
	assert.Nil(t, err)
}

// Client creation failing
func TestStopAddonErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	port := uint32(8000)
	stepID := "test"
	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	oldClient := newAddonClient
	defer func() { newAddonClient = oldClient }()
	newAddonClient = func(port uint, log *zap.SugaredLogger) (caddon.AddonClient, error) {
		return nil, errors.New("client create error")
	}

	err := StopAddon(ctx, stepID, port, log.Sugar())
	assert.NotNil(t, err)
}

// Failed to send GRPC request
func TestStopAddonServerErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	port := uint32(8000)
	stepID := "test"
	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	c := &mockClient{
		err: errors.New("server not running"),
	}
	mClient := amgrpc.NewMockAddonClient(ctrl)
	mClient.EXPECT().CloseConn().Return(nil)
	mClient.EXPECT().Client().Return(c)

	oldClient := newAddonClient
	defer func() { newAddonClient = oldClient }()
	newAddonClient = func(port uint, log *zap.SugaredLogger) (caddon.AddonClient, error) {
		return mClient, nil
	}

	err := StopAddon(ctx, stepID, port, log.Sugar())
	assert.NotNil(t, err)
}

// Success
func TestStopAddonSuccess(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	port := uint32(8000)
	stepID := "test"
	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	c := &mockClient{
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

	err := StopAddon(ctx, stepID, port, log.Sugar())
	assert.Nil(t, err)
}
