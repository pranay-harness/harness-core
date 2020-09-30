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
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

func TestPluginStepValidate(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	e := pluginStep{
		log: log.Sugar(),
	}
	_, err := e.Run(ctx)
	assert.NotNil(t, err)

	e = pluginStep{
		image: "plugin/drone-git",
		log:   log.Sugar(),
	}
	_, err = e.Run(ctx)
	assert.NotNil(t, err)

	e = pluginStep{
		image:         "plugin/drone-git",
		containerPort: uint32(8000),
		log:           log.Sugar(),
	}
	err = e.validate()
	assert.Nil(t, err)
}

// Client creation failing
func TestPluginExecuteClientErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	port := uint32(8000)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	step := &pb.UnitStep{
		Id: "test",
		Step: &pb.UnitStep_Plugin{
			Plugin: &pb.PluginStep{
				Image:         "plugin/drone-git",
				ContainerPort: port,
			},
		},
	}

	oldClient := newAddonClient
	defer func() { newAddonClient = oldClient }()
	newAddonClient = func(port uint, log *zap.SugaredLogger) (caddon.AddonClient, error) {
		return nil, errors.New("client create error")
	}

	executor := NewPluginStep(step, log.Sugar())
	numRetries, err := executor.Run(ctx)
	assert.NotNil(t, err)
	assert.Equal(t, numRetries, int32(1))
}

// Failed to send GRPC request
func TestPluginExecuteServerErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	port := uint32(8000)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	step := &pb.UnitStep{
		Id: "test",
		Step: &pb.UnitStep_Plugin{
			Plugin: &pb.PluginStep{
				Image:         "plugin/drone-git",
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

	executor := NewPluginStep(step, log.Sugar())
	numRetries, err := executor.Run(ctx)
	assert.NotNil(t, err)
	assert.Equal(t, numRetries, int32(1))
}

// Success
func TestPluginExecuteSuccess(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	port := uint32(8000)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	step := &pb.UnitStep{
		Id: "test",
		Step: &pb.UnitStep_Plugin{
			Plugin: &pb.PluginStep{
				Image:         "plugin/drone-git",
				ContainerPort: port,
			},
		},
	}

	numRetries := int32(3)
	c := &mockClient{
		response: &addonpb.ExecuteStepResponse{
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

	executor := NewPluginStep(step, log.Sugar())
	n, err := executor.Run(ctx)
	assert.Nil(t, err)
	assert.Equal(t, n, numRetries)
}
