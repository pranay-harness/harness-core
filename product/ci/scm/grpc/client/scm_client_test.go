// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package grpcclient

import (
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/logs"
	"go.uber.org/zap"
)

func TestValidClientClose(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	client, err := NewSCMClient(65534, log.Sugar())
	assert.Nil(t, err)
	err = client.CloseConn()
	assert.Nil(t, err)
}

func TestMultipleClose(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	client, err := NewSCMClient(65534, log.Sugar())
	fmt.Println(client.Client())
	assert.Nil(t, err)
	err = client.CloseConn()
	assert.Nil(t, err)
	err = client.CloseConn()
	fmt.Println(err)
	assert.NotNil(t, err)
	assert.NotNil(t, client.Client())
}
