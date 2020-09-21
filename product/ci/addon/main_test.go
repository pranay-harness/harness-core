package main

import (
	"context"
	"os"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/wings-software/portal/commons/go/lib/logs"
	"github.com/wings-software/portal/product/ci/addon/grpc"
	mgrpcserver "github.com/wings-software/portal/product/ci/addon/grpc/mocks"
	"go.uber.org/zap"
)

func Test_MainWithGrpc(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	oldLogger := newRemoteLogger
	defer func() { newRemoteLogger = oldLogger }()
	newRemoteLogger = func(key string) (rl *logs.RemoteLogger, err error) {
		log, _ := logs.GetObservedLogger(zap.InfoLevel)
		return &logs.RemoteLogger{BaseLogger: log.Sugar(), Writer: logs.NopWriter()}, nil
	}

	mockServer := mgrpcserver.NewMockAddonServer(ctrl)
	s := func(uint, *zap.SugaredLogger) (grpc.AddonServer, error) {
		return mockServer, nil
	}

	oldArgs := os.Args
	defer func() { os.Args = oldArgs }()
	os.Args = []string{"addon", "--port", "20000"}

	oldAddonServer := addonServer
	defer func() { addonServer = oldAddonServer }()
	addonServer = s

	mockServer.EXPECT().Start()
	mockServer.EXPECT().Stop().AnyTimes()
	main()
}
