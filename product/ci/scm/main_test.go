// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package main

import (
	"context"
	"os"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/wings-software/portal/product/ci/scm/grpc"
	mgrpcserver "github.com/wings-software/portal/product/ci/scm/grpc/mocks"
	"go.uber.org/zap"
)

func TestMainWithGrpc(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()
	mockServer := mgrpcserver.NewMockSCMServer(ctrl)
	s := func(uint, string, *zap.SugaredLogger) (grpc.SCMServer, error) {
		return mockServer, nil
	}

	oldArgs := os.Args
	defer func() { os.Args = oldArgs }()
	os.Args = []string{"scm", "--port", "20000"}

	oldSCMServer := scmServer
	defer func() { scmServer = oldSCMServer }()
	scmServer = s

	mockServer.EXPECT().Start()
	mockServer.EXPECT().Stop().AnyTimes()
	main()
}
