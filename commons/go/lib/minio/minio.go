// Copyright 2020 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package minio

import (
	"context"
	"time"

	"github.com/minio/minio-go/v6"
	"github.com/wings-software/portal/commons/go/lib/utils"
	"go.uber.org/zap"
)

const (
	defaultContentType = "application/octet-stream"

	reducedRedundancy  = "REDUCED_REDUNDANCY"
	standardRedundancy = "STANDARD"
)

var minioNewClient = minio.New

//go:generate mockgen -source minio.go -package=minio -destination mocks/minio_mock.go Client

// Client provides an interface to interact with MinIO service
type Client interface {
	UploadWithOpts(ctx context.Context, key, filePath string, metadata map[string]string, isStandardRedundancy bool, partSize uint64) error
	Upload(ctx context.Context, key, filePath string, opts minio.PutObjectOptions) error
	Download(ctx context.Context, key, filePath string) error
	Stat(key string) (string, map[string]string, error)
}

type minioClient struct {
	client StorageClient
	bucket string
	log    *zap.SugaredLogger
}

// NewClient creates a new MinIO client and returns it.
func NewClient(endpoint, accessKey, secretKey, bucket string,
	sslEnabled bool, log *zap.SugaredLogger) (Client, error) {
	client, err := minioNewClient(endpoint, accessKey, secretKey, sslEnabled)
	if err != nil {
		return nil, err
	}

	return &minioClient{
		client: AdaptMinioClient(client),
		bucket: bucket,
		log:    log,
	}, nil
}

// Uploads a file to MinIO with configurable user metadata, redundancy and part size.
func (c *minioClient) UploadWithOpts(ctx context.Context, key, filePath string, metadata map[string]string, isStandardRedundancy bool, partSize uint64) error {
	opts := minio.PutObjectOptions{
		ContentType:  defaultContentType,
		UserMetadata: metadata,
		StorageClass: standardRedundancy,
	}
	// If part size is 0, default part size of 128MB is used by MinIO
	if partSize != 0 {
		opts.PartSize = partSize
	}
	if !isStandardRedundancy {
		opts.StorageClass = reducedRedundancy
	}

	return c.Upload(ctx, key, filePath, opts)
}

// Uploads a file to MinIO.
func (c *minioClient) Upload(ctx context.Context, key, filePath string, opts minio.PutObjectOptions) error {
	start := time.Now()
	size, err := c.client.FPutObjectWithContext(ctx, c.bucket, key, filePath, opts)
	if err != nil {
		c.log.Warnw(
			"failed to upload to MinIO",
			"bucket", c.bucket,
			"key", key,
			"opts", opts,
			"elapsed_time_ms", utils.TimeSince(start),
			zap.Error(err),
		)
		return err
	}

	c.log.Infow(
		"uploaded to minio",
		"bucket", c.bucket,
		"key", key,
		"opts", opts,
		"upload_size", size,
		"elapsed_time_ms", utils.TimeSince(start),
	)
	return nil
}

// Downloads an object from MinIO.
func (c *minioClient) Download(ctx context.Context, key, filePath string) error {
	start := time.Now()
	err := c.client.FGetObjectWithContext(ctx, c.bucket, key, filePath, minio.GetObjectOptions{})
	if err != nil {
		c.log.Warnw(
			"failed to download from MinIO",
			"bucket", c.bucket,
			"key", key,
			"elapsed_time_ms", utils.TimeSince(start),
			zap.Error(err),
		)
		return err
	}

	c.log.Infow(
		"downloaded from minio",
		"bucket", c.bucket,
		"key", key,
		"elapsed_time_ms", utils.TimeSince(start),
	)
	return nil
}

// Fetches stat of an object from MinIO.
func (c *minioClient) Stat(key string) (string, map[string]string, error) {
	start := time.Now()
	objInfo, err := c.client.StatObject(c.bucket, key, minio.StatObjectOptions{})
	if err != nil {
		c.log.Warnw(
			"failed to fetch MinIO object stat",
			"bucket", c.bucket,
			"key", key,
			"elapsed_time_ms", utils.TimeSince(start),
			zap.Error(err),
		)
		return "", nil, err
	}

	c.log.Infow(
		"fetched minio object stat",
		"bucket", c.bucket,
		"key", key,
		"elapsed_time_ms", utils.TimeSince(start),
	)
	return objInfo.ETag, objInfo.UserMetadata, nil
}
