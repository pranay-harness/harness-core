// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package minio

import (
	"context"

	"github.com/minio/minio-go/v6"
)

//go:generate mockgen -source minio_adapter.go  -package=minio -destination minio_adapter_mock.go StorageClient

type (
	storageClient struct{ *minio.Client }
)

// StorageClient denotes the required methods on MinIO storage client.
type StorageClient interface {
	FPutObjectWithContext(ctx context.Context, bucketName, objectName, filePath string, opts minio.PutObjectOptions) (n int64, err error)
	FGetObjectWithContext(ctx context.Context, bucketName, objectName, filePath string, opts minio.GetObjectOptions) error
	StatObject(bucketName, objectName string, opts minio.StatObjectOptions) (minio.ObjectInfo, error)
}

// AdaptMinioClient adapts a minio.Client so that it satisfies StorageClient.
func AdaptMinioClient(c *minio.Client) StorageClient {
	return storageClient{c}
}
