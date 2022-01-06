// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package logutil

import (
	"bytes"
	"context"
	"encoding/json"
	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	grpcclient "github.com/wings-software/portal/product/ci/engine/grpc/client"
	mclient "github.com/wings-software/portal/product/ci/engine/grpc/client/mocks"
	"github.com/wings-software/portal/product/log-service/stream"
	"go.uber.org/zap"
	"testing"
)

func Test_UploadLink_Success(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	key := "key"

	oldLogProxyClient := newLogProxyClient
	defer func() {
		newLogProxyClient = oldLogProxyClient
	}()
	mGrpcClient := NewMockGrpcLogProxyClient(nil)
	mEngineClient := mclient.NewMockLogProxyClient(ctrl)
	mEngineClient.EXPECT().Client().Return(mGrpcClient)

	newLogProxyClient = func(port uint, log *zap.SugaredLogger) (grpcclient.LogProxyClient, error) {
		return mEngineClient, nil
	}
	gc, err := NewGrpcRemoteClient()
	assert.Nil(t, err)
	_, err = gc.UploadLink(ctx, key)
	assert.Nil(t, err)
	assert.Equal(t, len(mGrpcClient.ops), 1)
	assert.Equal(t, mGrpcClient.ops[0], "uploadlink")
}

func Test_Open_Success(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	key := "key"

	oldLogProxyClient := newLogProxyClient
	defer func() {
		newLogProxyClient = oldLogProxyClient
	}()
	mGrpcClient := NewMockGrpcLogProxyClient(nil)
	mGrpcClient.wg.Add(1)
	mEngineClient := mclient.NewMockLogProxyClient(ctrl)
	mEngineClient.EXPECT().Client().Return(mGrpcClient)

	newLogProxyClient = func(port uint, log *zap.SugaredLogger) (grpcclient.LogProxyClient, error) {
		return mEngineClient, nil
	}
	gc, err := NewGrpcRemoteClient()
	assert.Nil(t, err)
	err = gc.Open(ctx, key)

	mGrpcClient.wg.Wait() // wait for the Open call
	assert.Nil(t, err)
	assert.Equal(t, len(mGrpcClient.ops), 1)
	assert.Equal(t, mGrpcClient.ops[0], "open")
}

func Test_Close_Success(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	key := "key"

	oldLogProxyClient := newLogProxyClient
	defer func() {
		newLogProxyClient = oldLogProxyClient
	}()
	mGrpcClient := NewMockGrpcLogProxyClient(nil)
	mEngineClient := mclient.NewMockLogProxyClient(ctrl)
	mEngineClient.EXPECT().Client().Return(mGrpcClient)

	newLogProxyClient = func(port uint, log *zap.SugaredLogger) (grpcclient.LogProxyClient, error) {
		return mEngineClient, nil
	}
	gc, err := NewGrpcRemoteClient()
	assert.Nil(t, err)
	err = gc.Close(ctx, key)
	assert.Nil(t, err)
	assert.Equal(t, len(mGrpcClient.ops), 1)
	assert.Equal(t, mGrpcClient.ops[0], "close")
}

func Test_Write_Success(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	key := "key"
	var lines []*stream.Line
	l1 := &stream.Line{Level: "info", Message: "test", Args: map[string]string{"k1": "v1"}}
	l2 := &stream.Line{Level: "info", Message: "test2", Args: map[string]string{"k2": "v2"}}
	lines = append(lines, l1)
	lines = append(lines, l2)

	oldLogProxyClient := newLogProxyClient
	defer func() {
		newLogProxyClient = oldLogProxyClient
	}()
	mGrpcClient := NewMockGrpcLogProxyClient(nil)
	mEngineClient := mclient.NewMockLogProxyClient(ctrl)
	mEngineClient.EXPECT().Client().Return(mGrpcClient)

	newLogProxyClient = func(port uint, log *zap.SugaredLogger) (grpcclient.LogProxyClient, error) {
		return mEngineClient, nil
	}
	gc, err := NewGrpcRemoteClient()
	assert.Nil(t, err)
	err = gc.Write(ctx, key, lines)
	assert.Nil(t, err)
	assert.Equal(t, len(mGrpcClient.ops), 1)
	assert.Equal(t, mGrpcClient.ops[0], "write")
}

func Test_UploadUsingLink_Success(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	link := "http://minio:9000"
	l1 := &stream.Line{Level: "info", Message: "test", Args: map[string]string{"k1": "v1"}}
	json1, _ := json.Marshal(l1)
	l2 := &stream.Line{Level: "info", Message: "test2", Args: map[string]string{"k2": "v2"}}
	json2, _ := json.Marshal(l2)

	var jsonLines []string
	jsonLines = append(jsonLines, string(json1))
	jsonLines = append(jsonLines, string(json2))

	data := new(bytes.Buffer)
	for _, line := range jsonLines {
		data.Write([]byte(line))
	}

	oldLogProxyClient := newLogProxyClient
	defer func() {
		newLogProxyClient = oldLogProxyClient
	}()
	mGrpcClient := NewMockGrpcLogProxyClient(nil)
	mEngineClient := mclient.NewMockLogProxyClient(ctrl)
	mEngineClient.EXPECT().Client().Return(mGrpcClient)

	newLogProxyClient = func(port uint, log *zap.SugaredLogger) (grpcclient.LogProxyClient, error) {
		return mEngineClient, nil
	}
	gc, err := NewGrpcRemoteClient()
	assert.Nil(t, err)
	err = gc.UploadUsingLink(ctx, link, data)
	assert.Nil(t, err)
	assert.Equal(t, len(mGrpcClient.ops), 1)
	assert.Equal(t, mGrpcClient.ops[0], "uploadusinglink")
}
