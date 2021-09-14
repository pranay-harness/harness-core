// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

// Package store defines the log storage interface.
package store

import (
	"context"
	"io"
	"time"
)

// Store defines the log stoage interface.
type Store interface {
	// Download downloads the blob from the datastore.
	Download(ctx context.Context, key string) (io.ReadCloser, error)

	// DownloadLink returns a pre-signed download link.
	DownloadLink(ctx context.Context, key string, expire time.Duration) (string, error)

	// Upload uploads the blob to the datastore.
	Upload(ctx context.Context, key string, r io.Reader) error

	// UploadLink returns a pre-singed upload link.
	UploadLink(ctx context.Context, key string, expire time.Duration) (string, error)

	// Delete deletes the blob from the datastore.
	Delete(ctx context.Context, key string) error
}
