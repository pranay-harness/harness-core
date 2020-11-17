package grpc

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/logs"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"github.com/wings-software/portal/product/log-service/client"
	"github.com/wings-software/portal/product/log-service/mock"
	"github.com/wings-software/portal/product/log-service/stream"
	"go.uber.org/zap"
	"testing"
)

func TestWrite_Success(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	key := "key"
	var lines []*stream.Line
	l1 := &stream.Line{Level: "info", Message: "test", Args: map[string]string{"k1": "v1"}}
	json1, _ := json.Marshal(l1)
	l2 := &stream.Line{Level: "info", Message: "test2", Args: map[string]string{"k2": "v2"}}
	json2, _ := json.Marshal(l2)
	lines = append(lines, l1)
	lines = append(lines, l2)

	mLogClient := mock.NewMockClient(ctrl)
	mLogClient.EXPECT().Write(ctx, key, lines).Return(nil)

	oldLogHTTPClient := remoteLogClient
	defer func() { remoteLogClient = oldLogHTTPClient }()
	remoteLogClient = func() (client.Client, error) { return mLogClient, nil }

	var jsonLines []string
	jsonLines = append(jsonLines, string(json1))
	jsonLines = append(jsonLines, string(json2))

	in := &pb.WriteRequest{Key: key, Lines: jsonLines}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	h := NewLogProxyHandler(log.Sugar())
	_, err := h.Write(ctx, in)
	assert.Nil(t, err)
}

func TestWrite_Failure_IncorrectLineFormat(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	key := "key"

	mLogClient := mock.NewMockClient(ctrl)

	oldLogHTTPClient := remoteLogClient
	defer func() { remoteLogClient = oldLogHTTPClient }()
	remoteLogClient = func() (client.Client, error) { return mLogClient, nil }

	var lines []string
	lines = append(lines, "incorrect format")

	in := &pb.WriteRequest{Key: key, Lines: lines}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	h := NewLogProxyHandler(log.Sugar())
	_, err := h.Write(ctx, in)
	assert.NotNil(t, err)
}

func TestWrite_Failure_Client(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	key := "key"

	oldLogHTTPClient := remoteLogClient
	defer func() { remoteLogClient = oldLogHTTPClient }()
	remoteLogClient = func() (client.Client, error) { return nil, errors.New("couldn't create log HTTP client") }

	l1 := &stream.Line{Level: "info", Message: "test", Args: map[string]string{"k1": "v1"}}
	json1, _ := json.Marshal(l1)
	var lines []string
	lines = append(lines, string(json1))

	in := &pb.WriteRequest{Key: key, Lines: lines}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	h := NewLogProxyHandler(log.Sugar())
	_, err := h.Write(ctx, in)
	assert.NotNil(t, err)
}

func Test_UploadLink_Success(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	key := "key"
	strLink := "http://minio:9000"
	link := &client.Link{Value: strLink}

	mLogClient := mock.NewMockClient(ctrl)
	mLogClient.EXPECT().UploadLink(ctx, key).Return(link, nil)

	oldLogHTTPClient := remoteLogClient
	defer func() { remoteLogClient = oldLogHTTPClient }()
	remoteLogClient = func() (client.Client, error) { return mLogClient, nil }

	in := &pb.UploadLinkRequest{Key: key}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	h := NewLogProxyHandler(log.Sugar())
	resp, err := h.UploadLink(ctx, in)
	assert.Nil(t, err)
	assert.Equal(t, resp.GetLink(), strLink)
}

func Test_UploadLink_Failure(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	key := "key"

	mLogClient := mock.NewMockClient(ctrl)
	mLogClient.EXPECT().UploadLink(ctx, key).Return(nil, errors.New("upload link failure"))

	oldLogHTTPClient := remoteLogClient
	defer func() { remoteLogClient = oldLogHTTPClient }()
	remoteLogClient = func() (client.Client, error) { return mLogClient, nil }

	in := &pb.UploadLinkRequest{Key: key}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	h := NewLogProxyHandler(log.Sugar())
	_, err := h.UploadLink(ctx, in)
	assert.NotNil(t, err)
}

func Test_UploadLink_Failure_Client(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	key := "key"

	oldLogHTTPClient := remoteLogClient
	defer func() { remoteLogClient = oldLogHTTPClient }()
	remoteLogClient = func() (client.Client, error) { return nil, errors.New("couldn't create client") }

	in := &pb.UploadLinkRequest{Key: key}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	h := NewLogProxyHandler(log.Sugar())
	_, err := h.UploadLink(ctx, in)
	assert.NotNil(t, err)
}

func Test_Open_Success(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	key := "key"

	mLogClient := mock.NewMockClient(ctrl)
	mLogClient.EXPECT().Open(ctx, key).Return(nil)

	oldLogHTTPClient := remoteLogClient
	defer func() { remoteLogClient = oldLogHTTPClient }()
	remoteLogClient = func() (client.Client, error) { return mLogClient, nil }

	in := &pb.OpenRequest{Key: key}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	h := NewLogProxyHandler(log.Sugar())
	_, err := h.Open(ctx, in)
	assert.Nil(t, err)
}

func Test_Open_Failure(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	key := "key"

	mLogClient := mock.NewMockClient(ctrl)
	mLogClient.EXPECT().Open(ctx, key).Return(errors.New("failed to open stream"))

	oldLogHTTPClient := remoteLogClient
	defer func() { remoteLogClient = oldLogHTTPClient }()
	remoteLogClient = func() (client.Client, error) { return mLogClient, nil }

	in := &pb.OpenRequest{Key: key}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	h := NewLogProxyHandler(log.Sugar())
	_, err := h.Open(ctx, in)
	assert.NotNil(t, err)
}

func Test_Open_Failure_Client(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	key := "key"

	oldLogHTTPClient := remoteLogClient
	defer func() { remoteLogClient = oldLogHTTPClient }()
	remoteLogClient = func() (client.Client, error) { return nil, errors.New("could not create client") }

	in := &pb.OpenRequest{Key: key}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	h := NewLogProxyHandler(log.Sugar())
	_, err := h.Open(ctx, in)
	assert.NotNil(t, err)
}

func Test_Close_Success(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	key := "key"

	mLogClient := mock.NewMockClient(ctrl)
	mLogClient.EXPECT().Close(ctx, key).Return(nil)

	oldLogHTTPClient := remoteLogClient
	defer func() { remoteLogClient = oldLogHTTPClient }()
	remoteLogClient = func() (client.Client, error) { return mLogClient, nil }

	in := &pb.CloseRequest{Key: key}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	h := NewLogProxyHandler(log.Sugar())
	_, err := h.Close(ctx, in)
	assert.Nil(t, err)
}

func Test_Close_Failure(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	key := "key"

	mLogClient := mock.NewMockClient(ctrl)
	mLogClient.EXPECT().Close(ctx, key).Return(errors.New("failed to open stream"))

	oldLogHTTPClient := remoteLogClient
	defer func() { remoteLogClient = oldLogHTTPClient }()
	remoteLogClient = func() (client.Client, error) { return mLogClient, nil }

	in := &pb.CloseRequest{Key: key}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	h := NewLogProxyHandler(log.Sugar())
	_, err := h.Close(ctx, in)
	assert.NotNil(t, err)
}

func Test_Close_Failure_Client(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	key := "key"

	oldLogHTTPClient := remoteLogClient
	defer func() { remoteLogClient = oldLogHTTPClient }()
	remoteLogClient = func() (client.Client, error) { return nil, errors.New("could not create client") }

	in := &pb.CloseRequest{Key: key}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	h := NewLogProxyHandler(log.Sugar())
	_, err := h.Close(ctx, in)
	assert.NotNil(t, err)
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

	mLogClient := mock.NewMockClient(ctrl)
	mLogClient.EXPECT().UploadUsingLink(ctx, link, data).Return(nil)

	oldLogHTTPClient := remoteLogClient
	defer func() { remoteLogClient = oldLogHTTPClient }()
	remoteLogClient = func() (client.Client, error) { return mLogClient, nil }

	in := &pb.UploadUsingLinkRequest{Link: link, Lines: jsonLines}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	h := NewLogProxyHandler(log.Sugar())
	_, err := h.UploadUsingLink(ctx, in)
	assert.Nil(t, err)
}

func Test_UploadUsingLink_Failure(t *testing.T) {
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

	mLogClient := mock.NewMockClient(ctrl)
	mLogClient.EXPECT().UploadUsingLink(ctx, link, data).Return(errors.New("upload using link failure"))

	oldLogHTTPClient := remoteLogClient
	defer func() { remoteLogClient = oldLogHTTPClient }()
	remoteLogClient = func() (client.Client, error) { return mLogClient, nil }

	in := &pb.UploadUsingLinkRequest{Link: link, Lines: jsonLines}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	h := NewLogProxyHandler(log.Sugar())
	_, err := h.UploadUsingLink(ctx, in)
	assert.NotNil(t, err)
}

func Test_UploadUsingLink_Failure_Client(t *testing.T) {
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

	oldLogHTTPClient := remoteLogClient
	defer func() { remoteLogClient = oldLogHTTPClient }()
	remoteLogClient = func() (client.Client, error) { return nil, errors.New("error while creating client") }

	in := &pb.UploadUsingLinkRequest{Link: link, Lines: jsonLines}

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	h := NewLogProxyHandler(log.Sugar())
	_, err := h.UploadUsingLink(ctx, in)
	assert.NotNil(t, err)
}
