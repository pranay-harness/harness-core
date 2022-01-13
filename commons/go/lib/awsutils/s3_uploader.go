// Copyright 2020 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package awsutils

import (
	"context"
	"io"
	"time"

	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/aws/session"
	"github.com/aws/aws-sdk-go/service/s3/s3manager"
	"github.com/opentracing/opentracing-go"
	"github.com/wings-software/portal/commons/go/lib/filesystem"
	"github.com/wings-software/portal/commons/go/lib/logs"
	"github.com/wings-software/portal/commons/go/lib/utils"
	xtrace "github.com/wings-software/portal/commons/go/lib/x/trace"

	"go.uber.org/zap"
)

var defaultACL = "bucket-owner-full-control"

//go:generate mockgen -source s3_uploader.go -destination mocks/s3_uploader_mock.go -package awsutils S3Uploader S3UploadClient

var _ S3UploadClient = &s3manager.Uploader{}

// S3UploadClient denotes the required methods to upload to S3
// s3iface unfortunately doesn't have Uploader interface
type S3UploadClient interface {
	Upload(input *s3manager.UploadInput, options ...func(*s3manager.Uploader)) (*s3manager.UploadOutput, error)
	UploadWithContext(ctx context.Context, input *s3manager.UploadInput, options ...func(*s3manager.Uploader)) (*s3manager.UploadOutput, error)
}

//S3Uploader represents a simple interface to upload things to S3
type S3Uploader interface {
	//UploadReader calls underlying UploadReaderWithContext with background context
	UploadReader(key string, reader io.Reader) (string, string, error)

	//UploadReaderWithContext calls underlying UploadReaderWithContext with the given context
	UploadReaderWithContext(ctx context.Context, key string, reader io.Reader) (string, string, error)

	//UploadFile uploads the given file with Background context
	UploadFile(key string, filename string) (string, string, error)

	//UploadFileWithContext uploads the given file with given context
	UploadFileWithContext(ctx context.Context, key string, filename string) (string, string, error)
}

type prefixedS3Uploader struct {
	uploader S3Uploader
	prefix   string
}

func (p prefixedS3Uploader) UploadReader(key string, reader io.Reader) (string, string, error) {
	return p.uploader.UploadReader(p.prefix+key, reader)
}

func (p prefixedS3Uploader) UploadReaderWithContext(ctx context.Context, key string, reader io.Reader) (string, string, error) {
	return p.uploader.UploadReaderWithContext(ctx, p.prefix+key, reader)
}

func (p prefixedS3Uploader) UploadFile(key string, filename string) (string, string, error) {
	return p.uploader.UploadFile(p.prefix+key, filename)
}

func (p prefixedS3Uploader) UploadFileWithContext(ctx context.Context, key string, filename string) (string, string, error) {
	return p.uploader.UploadFileWithContext(ctx, p.prefix+key, filename)
}

//NewPrefixedS3Uploader wraps an S3Uploader so that it uploads all files under the given prefix
// No changes are made to the prefix, make sure leading/trailing slashes are correct
func NewPrefixedS3Uploader(uploader S3Uploader, prefix string) S3Uploader {
	return &prefixedS3Uploader{uploader, prefix}
}

type s3Uploader struct {
	bucket string
	client S3UploadClient
	fs     filesystem.FileSystem
	log    logs.SugaredLoggerIface
}

// TracedS3UploadClient is an S3UploadClient that emits tracing data
type TracedS3UploadClient struct {
	S3UploadClient
}

//UploadReader calls underlying UploadReaderWithContext with background context
func (s *s3Uploader) UploadReader(key string, reader io.Reader) (string, string, error) {
	return s.UploadReaderWithContext(context.Background(), key, reader)
}

//UploadReaderWithContext calls underlying UploadReaderWithContext with the given context
func (s *s3Uploader) UploadReaderWithContext(ctx context.Context, key string, reader io.Reader) (string, string, error) {
	return s.uploadReader(ctx, key, reader)
}

//UploadFile uploads the given file with Background context
func (s *s3Uploader) UploadFile(key string, filename string) (string, string, error) {
	return s.UploadFileWithContext(context.Background(), key, filename)
}

//UploadFileWithContext uploads the given file with given context
func (s *s3Uploader) UploadFileWithContext(ctx context.Context, key string, filename string) (string, string, error) {
	var uploadedKey, uploadedBucket string

	err := s.fs.ReadFile(filename, func(reader io.Reader) error {
		k, b, err := s.uploadReader(ctx, key, reader)
		uploadedKey, uploadedBucket = k, b
		return err
	})
	return uploadedKey, uploadedBucket, err
}

//NewS3Uploader constructs a new S3Uploader based on an S3UploadClient
func NewS3Uploader(bucket string, client S3UploadClient, fs filesystem.FileSystem, log logs.SugaredLoggerIface) S3Uploader {
	return &s3Uploader{bucket, &TracedS3UploadClient{client}, fs, log}
}

//NewS3UploadClient returns a new S3UploadClient with tracing, using the given session
func NewS3UploadClient(sess *session.Session) S3UploadClient {
	return TracedS3UploadClient{s3manager.NewUploader(sess)}
}

//UploadWithContext emits tracing data if there is a span present within the context, and calls the
// underlying UploadWithContext function
func (t TracedS3UploadClient) UploadWithContext(ctx aws.Context, input *s3manager.UploadInput, options ...func(*s3manager.Uploader)) (*s3manager.UploadOutput, error) {
	if parent := opentracing.SpanFromContext(ctx); parent != nil {
		span := opentracing.StartSpan("s3.Upload", opentracing.ChildOf(parent.Context()), opentracing.Tags{
			"acl":           input.ACL,
			"key":           input.Key,
			"bucket":        input.Bucket,
			"storage_class": input.StorageClass,
			"cache_control": input.CacheControl,
		})
		defer span.Finish()

		output, err := t.S3UploadClient.UploadWithContext(ctx, input, options...)
		if err != nil {
			xtrace.LogError(span, err)
		}
		return output, err
	}
	return t.S3UploadClient.UploadWithContext(ctx, input, options...)
}

func (s *s3Uploader) uploadReader(ctx context.Context, key string, reader io.Reader) (string, string, error) {
	start := time.Now()
	resp, err := s.client.UploadWithContext(ctx, &s3manager.UploadInput{
		ACL:    &defaultACL,
		Bucket: &s.bucket,
		Key:    &key,
		Body:   reader,
	})

	if err != nil {
		s.log.Warnw(
			"error encountered while uploading to s3",
			"bucket", s.bucket,
			"key", key,
			"elapsed_time_ms", utils.TimeSince(start),
			zap.Error(err),
		)
		return s.bucket, key, err
	}

	s.log.Infow(
		"uploaded to S3",
		"bucket", s.bucket,
		"key", key,
		"url", resp.Location,
		"s3_upload_id", resp.UploadID,
		"elapsed_time_ms", utils.TimeSince(start),
	)
	return s.bucket, key, nil
}
