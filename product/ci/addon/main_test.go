package main

import (
	"context"
	"os"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/wings-software/portal/product/ci/addon/grpc"
	mgrpcserver "github.com/wings-software/portal/product/ci/addon/grpc/mocks"
	"go.uber.org/zap"
)

func Test_MainWithGrpc(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()
	mockServer := mgrpcserver.NewMockCIAddonServer(ctrl)
	s := func(uint, *zap.SugaredLogger) (grpc.CIAddonServer, error) {
		return mockServer, nil
	}

	oldArgs := os.Args
	defer func() { os.Args = oldArgs }()
	os.Args = []string{"addon", "--port", "20000"}

	oldAddonServer := ciAddonServer
	defer func() { ciAddonServer = oldAddonServer }()
	ciAddonServer = s

	mockServer.EXPECT().Start()
	main()
}
