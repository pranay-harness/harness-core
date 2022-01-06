// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package steps

import (
	"crypto/md5"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"runtime"
	"strconv"
	"strings"
	"text/template"
	"time"

	"github.com/cespare/xxhash"
	"github.com/pkg/errors"
	"github.com/wings-software/portal/commons/go/lib/archive"
	"github.com/wings-software/portal/commons/go/lib/filesystem"
	"github.com/wings-software/portal/commons/go/lib/minio"
	"github.com/wings-software/portal/commons/go/lib/utils"
	"go.uber.org/zap"
)

const (
	minioEndpointEnv  = "ENDPOINT_MINIO"
	minioAccessKeyEnv = "ACCESS_KEY_MINIO"
	minioSecretKeyEnv = "SECRET_KEY_MINIO"
	minioBucketEnv    = "BUCKET_MINIO"

	archiveFormat = archive.TarFormat
	xxHashSumKey  = "Xxhash"
)

var newMinioClient = func(log *zap.SugaredLogger) (minio.Client, error) {
	endpoint, ok := os.LookupEnv(minioEndpointEnv)
	if !ok {
		return nil, errors.New(fmt.Sprintf("%s environment variable is not set", minioEndpointEnv))
	}
	accessKey, ok := os.LookupEnv(minioAccessKeyEnv)
	if !ok {
		return nil, errors.New(fmt.Sprintf("%s environment variable is not set", minioAccessKeyEnv))
	}
	secretKey, ok := os.LookupEnv(minioSecretKeyEnv)
	if !ok {
		return nil, errors.New(fmt.Sprintf("%s environment variable is not set", minioSecretKeyEnv))
	}
	bucket, ok := os.LookupEnv(minioBucketEnv)
	if !ok {
		return nil, errors.New(fmt.Sprintf("%s environment variable is not set", minioBucketEnv))
	}

	return minio.NewClient(endpoint, accessKey, secretKey, bucket, false, log)
}

// Returns xxHash of the given file
func getFileXXHash(filename string, fs filesystem.FileSystem, log *zap.SugaredLogger) (string, error) {
	start := time.Now()
	file, err := fs.Open(filename)
	if err != nil {
		return "", errors.Wrap(err, fmt.Sprintf("failed to open file: %s", filename))
	}
	defer file.Close()

	hasher := xxhash.New()
	if _, err := fs.Copy(hasher, file); err != nil {
		return "", errors.Wrap(err, fmt.Sprintf("failed to copy from file: %s", filename))
	}
	hashSum := strconv.FormatUint(hasher.Sum64(), 10)

	log.Infow(
		"Calculated file xxhash",
		"file", filename,
		"elapsed_time_ms", utils.TimeSince(start),
	)
	return hashSum, nil
}

// parseCacheKeyTmpl parses cache key template
// Example: {{ checksum "go.mod" }}_{{ checksum "go.sum" }}_{{ arch }}_{{ os }}
func parseCacheKeyTmpl(key string, log *zap.SugaredLogger) (string, error) {
	f := template.FuncMap{
		"checksum": checksumFunc(),
		"epoch":    func() string { return strconv.FormatInt(time.Now().Unix(), 10) },
		"arch":     func() string { return runtime.GOARCH },
		"os":       func() string { return runtime.GOOS },
	}
	t, err := template.New("key").Funcs(f).Parse(key)
	if err != nil {
		log.Errorw("failed to parse template", "key", key, zap.Error(err))
		return "", errors.Wrap(err, fmt.Sprintf("failed to parse template: %s", key))
	}

	var b strings.Builder
	err = t.Execute(&b, nil)
	if err != nil {
		log.Errorw("failed to evaluate template", "key", key, zap.Error(err))
		return "", errors.Wrap(err, fmt.Sprintf("failed to evaulate template: %s", key))
	}

	return b.String(), nil
}

// checksumFunc returns a method to generate checksum of a file
func checksumFunc() func(string) (string, error) {
	return func(p string) (string, error) {
		path, err := filepath.Abs(filepath.Clean(p))
		if err != nil {
			return "", err
		}

		f, err := os.Open(path)
		if err != nil {
			return "", err
		}
		defer f.Close()

		hashSum, err := readerHasher(f)
		if err != nil {
			return "", err
		}
		return hashSum, nil
	}
}

// readerHasher generic md5 hash generater from io.Reader.
func readerHasher(r io.Reader) (string, error) {
	h := md5.New()
	if _, err := io.Copy(h, r); err != nil {
		return "", err
	}
	return fmt.Sprintf("%x", h.Sum(nil)), nil
}
