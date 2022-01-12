// Copyright 2020 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package minio

import (
	"context"
	"errors"
	"os"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/minio/minio-go/v6"
	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/logs"
	"go.uber.org/zap"
)

func TestMinioUploadWithOptsSuccess(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	bucket := "test-bucket"
	key := "test-key"
	filePath := "/tmp/hello-world"
	size := int64(101)
	log, _ := logs.GetObservedLogger(zap.ErrorLevel)
	client := NewMockStorageClient(ctrl)
	minio := &minioClient{client, bucket, log.Sugar()}

	client.EXPECT().FPutObjectWithContext(ctx, bucket, key, filePath, gomock.Any()).Return(size, nil)
	err := minio.UploadWithOpts(ctx, key, filePath, nil, true, 0)
	assert.Equal(t, err, nil)
}

func TestMinioUploadWithOptsErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	bucket := "test-bucket"
	key := "test-key"
	filePath := "/tmp/hello-world"
	size := int64(101)
	partSize := uint64(1000)
	log, _ := logs.GetObservedLogger(zap.ErrorLevel)
	client := NewMockStorageClient(ctrl)
	minio := &minioClient{client, bucket, log.Sugar()}

	client.EXPECT().FPutObjectWithContext(ctx, bucket, key, filePath, gomock.Any()).Return(size, os.ErrNotExist)
	err := minio.UploadWithOpts(ctx, key, filePath, nil, false, partSize)
	assert.Equal(t, err, os.ErrNotExist)
}

func TestMinioDownloadSuccess(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	bucket := "test-bucket"
	key := "test-key"
	filePath := "/tmp/hello-world"
	log, _ := logs.GetObservedLogger(zap.ErrorLevel)
	client := NewMockStorageClient(ctrl)
	minio := &minioClient{client, bucket, log.Sugar()}

	client.EXPECT().FGetObjectWithContext(ctx, bucket, key, filePath, gomock.Any()).Return(nil)
	err := minio.Download(ctx, key, filePath)
	assert.Equal(t, err, nil)
}

func TestMinioDownloadErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	bucket := "test-bucket"
	key := "test-key"
	filePath := "/tmp/hello-world"
	log, _ := logs.GetObservedLogger(zap.ErrorLevel)
	client := NewMockStorageClient(ctrl)
	minio := &minioClient{client, bucket, log.Sugar()}

	client.EXPECT().FGetObjectWithContext(ctx, bucket, key, filePath, gomock.Any()).Return(os.ErrNotExist)
	err := minio.Download(ctx, key, filePath)
	assert.Equal(t, err, os.ErrNotExist)
}

func TestMinioStatSuccess(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	bucket := "test-bucket"
	key := "test-key"
	log, _ := logs.GetObservedLogger(zap.ErrorLevel)
	client := NewMockStorageClient(ctrl)
	mc := &minioClient{client, bucket, log.Sugar()}

	client.EXPECT().StatObject(bucket, key, gomock.Any()).Return(minio.ObjectInfo{}, nil)
	_, _, err := mc.Stat(key)
	assert.Equal(t, err, nil)
}

func TestMinioStatErr(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	bucket := "test-bucket"
	key := "test-key"
	log, _ := logs.GetObservedLogger(zap.ErrorLevel)
	client := NewMockStorageClient(ctrl)
	mc := &minioClient{client, bucket, log.Sugar()}

	client.EXPECT().StatObject(bucket, key, gomock.Any()).Return(minio.ObjectInfo{}, os.ErrNotExist)
	_, _, err := mc.Stat(key)
	assert.Equal(t, err, os.ErrNotExist)
}

func TestMinioClientSuccess(t *testing.T) {
	// assign the original Minio client function to a variable.
	originalMinioClient := minioNewClient
	// defer the re-assignment back to the original validate function.
	defer func() {
		minioNewClient = originalMinioClient
	}()

	endpoint := "1.1.1.1:9000"
	accessKey := "minio"
	secretKey := "minio123"
	bucket := "test-bucket"
	sslEnabled := true

	mockMinioClient := &minio.Client{}
	minioNewClient = func(endpoint, accessKey, secretKey string, secure bool) (*minio.Client, error) {
		return mockMinioClient, nil
	}
	_ = mockMinioClient
	_, err := NewClient(endpoint, accessKey, secretKey, bucket, sslEnabled, nil)
	assert.Equal(t, err, nil)
}

func TestMinioClientError(t *testing.T) {
	// assign the original Minio client function to a variable.
	originalMinioClient := minioNewClient
	// defer the re-assignment back to the original validate function.
	defer func() {
		minioNewClient = originalMinioClient
	}()

	endpoint := "1.1.1.1:9000"
	accessKey := "minio"
	secretKey := "minio123"
	bucket := "test-bucket"
	sslEnabled := true

	mockMinioClient := &minio.Client{}
	minioNewClient = func(endpoint, accessKey, secretKey string, secure bool) (*minio.Client, error) {
		return nil, errors.New("failed to create client")
	}
	_ = mockMinioClient
	_, err := NewClient(endpoint, accessKey, secretKey, bucket, sslEnabled, nil)
	assert.NotEqual(t, err, nil)
}
