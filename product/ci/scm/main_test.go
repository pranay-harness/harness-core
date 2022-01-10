// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

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
