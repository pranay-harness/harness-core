// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package steps

import (
	"context"
	"fmt"
	"os"
	"strconv"
	"testing"
	"time"

	"github.com/cenkalti/backoff/v4"
	"github.com/cespare/xxhash"
	"github.com/golang/mock/gomock"
	"github.com/minio/minio-go/v6"
	"github.com/pkg/errors"
	"github.com/stretchr/testify/assert"
	marchive "github.com/wings-software/portal/commons/go/lib/archive/mocks"
	"github.com/wings-software/portal/commons/go/lib/filesystem"
	"github.com/wings-software/portal/commons/go/lib/logs"
	uminio "github.com/wings-software/portal/commons/go/lib/minio"
	mminio "github.com/wings-software/portal/commons/go/lib/minio/mocks"
	mbackoff "github.com/wings-software/portal/commons/go/lib/utils/mocks"
	"github.com/wings-software/portal/product/ci/engine/output"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

type fileInfo struct{}

func (fi fileInfo) Name() string       { return "hello-world" }
func (fi fileInfo) Size() int64        { return int64(100) }
func (fi fileInfo) Mode() os.FileMode  { return os.ModeAppend }
func (fi fileInfo) ModTime() time.Time { return time.Now() }
func (fi fileInfo) IsDir() bool        { return false }
func (fi fileInfo) Sys() interface{}   { return nil }

func getRestoreStep(id, key string, failIfNotExist bool) *pb.UnitStep {
	restoreCache := &pb.UnitStep_RestoreCache{
		RestoreCache: &pb.RestoreCacheStep{
			Key:            key,
			FailIfNotExist: failIfNotExist,
		},
	}
	return &pb.UnitStep{
		Id:          id,
		DisplayName: "test save cache step",
		Step:        restoreCache,
	}
}

func TestNewRestoreCacheStep(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	key := "key"
	id := "test"
	tmpFilePath := "/tmp"

	fs := filesystem.NewMockFileSystem(ctrl)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	step := getRestoreStep(id, key, false)
	NewRestoreCacheStep(step, tmpFilePath, nil, fs, log.Sugar())
}

func TestRestoreCacheMinioClientErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	key := "key"
	id := "test"
	tmpFilePath := "/tmp"
	failIfNotExist := true

	archiver := marchive.NewMockArchiver(ctrl)
	fs := filesystem.NewMockFileSystem(ctrl)
	mockBackOff := mbackoff.NewMockBackOff(ctrl)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	s := &restoreCacheStep{
		id:              id,
		displayName:     "save cache test step",
		key:             key,
		failIfNotExist:  failIfNotExist,
		tmpFilePath:     tmpFilePath,
		ignoreUnarchive: false,
		archiver:        archiver,
		backoff:         mockBackOff,
		log:             log.Sugar(),
		fs:              fs,
	}

	mockBackOff.EXPECT().Reset()
	mockBackOff.EXPECT().NextBackOff().Return(backoff.Stop)
	err := s.Run(ctx)
	assert.NotEqual(t, err, nil)
}

func TestRestoreCachePassIfKeyNotExist(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	key := "key"
	id := "test"
	tmpFilePath := "/tmp"
	failIfNotExist := false

	archiver := marchive.NewMockArchiver(ctrl)
	fs := filesystem.NewMockFileSystem(ctrl)
	mockBackOff := mbackoff.NewMockBackOff(ctrl)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	mockMinio := mminio.NewMockClient(ctrl)

	oldClient := newMinioClient
	defer func() { newMinioClient = oldClient }()
	newMinioClient = func(log *zap.SugaredLogger) (uminio.Client, error) {
		return mockMinio, nil
	}

	s := &restoreCacheStep{
		id:              id,
		displayName:     "save cache test step",
		key:             key,
		failIfNotExist:  failIfNotExist,
		tmpFilePath:     tmpFilePath,
		ignoreUnarchive: false,
		archiver:        archiver,
		backoff:         mockBackOff,
		log:             log.Sugar(),
		fs:              fs,
	}
	notFoundErr := minio.ErrorResponse{
		Code:    "NoSuchKey",
		Message: "The specified key does not exist.",
		Key:     key,
	}
	mockBackOff.EXPECT().Reset()
	mockMinio.EXPECT().Stat(key).Return("", nil, notFoundErr)
	err := s.Run(ctx)
	assert.Equal(t, err, nil)
}

func TestRestoreCacheFailIfKeyNotExistErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	key := "key"
	id := "test"
	tmpFilePath := "/tmp"
	failIfNotExist := true

	archiver := marchive.NewMockArchiver(ctrl)
	fs := filesystem.NewMockFileSystem(ctrl)
	mockBackOff := mbackoff.NewMockBackOff(ctrl)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	mockMinio := mminio.NewMockClient(ctrl)

	oldClient := newMinioClient
	defer func() { newMinioClient = oldClient }()
	newMinioClient = func(log *zap.SugaredLogger) (uminio.Client, error) {
		return mockMinio, nil
	}

	s := &restoreCacheStep{
		id:              id,
		displayName:     "save cache test step",
		key:             key,
		failIfNotExist:  failIfNotExist,
		tmpFilePath:     tmpFilePath,
		ignoreUnarchive: false,
		archiver:        archiver,
		backoff:         mockBackOff,
		log:             log.Sugar(),
		fs:              fs,
	}
	notFoundErr := minio.ErrorResponse{
		Code:    "NoSuchKey",
		Message: "The specified key does not exist.",
		Key:     key,
	}
	mockBackOff.EXPECT().Reset()
	mockMinio.EXPECT().Stat(key).Return("", nil, notFoundErr)
	err := s.Run(ctx)
	assert.NotEqual(t, err, nil)
}

func TestRestoreCacheStatUnknownErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	key := "key"
	id := "test"
	tmpFilePath := "/tmp"
	failIfNotExist := true

	archiver := marchive.NewMockArchiver(ctrl)
	fs := filesystem.NewMockFileSystem(ctrl)
	mockBackOff := mbackoff.NewMockBackOff(ctrl)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	mockMinio := mminio.NewMockClient(ctrl)

	oldClient := newMinioClient
	defer func() { newMinioClient = oldClient }()
	newMinioClient = func(log *zap.SugaredLogger) (uminio.Client, error) {
		return mockMinio, nil
	}

	s := &restoreCacheStep{
		id:              id,
		displayName:     "save cache test step",
		key:             key,
		failIfNotExist:  failIfNotExist,
		tmpFilePath:     tmpFilePath,
		ignoreUnarchive: false,
		archiver:        archiver,
		backoff:         mockBackOff,
		log:             log.Sugar(),
		fs:              fs,
	}
	bucketNotFoundErr := minio.ErrorResponse{
		Code:    "NoSuchBucket",
		Message: "The specified bucket does not exist.",
		Key:     key,
	}
	mockBackOff.EXPECT().Reset()
	mockMinio.EXPECT().Stat(key).Return("", nil, bucketNotFoundErr)
	mockBackOff.EXPECT().NextBackOff().Return(backoff.Stop)
	err := s.Run(ctx)
	assert.NotEqual(t, err, nil)
}

func TestRestoreCacheDownloadErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	key := "key"
	id := "test"
	tmpFilePath := "/tmp"
	failIfNotExist := true

	archiver := marchive.NewMockArchiver(ctrl)
	fs := filesystem.NewMockFileSystem(ctrl)
	mockBackOff := mbackoff.NewMockBackOff(ctrl)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	mockMinio := mminio.NewMockClient(ctrl)

	oldClient := newMinioClient
	defer func() { newMinioClient = oldClient }()
	newMinioClient = func(log *zap.SugaredLogger) (uminio.Client, error) {
		return mockMinio, nil
	}

	s := &restoreCacheStep{
		id:              id,
		displayName:     "save cache test step",
		key:             key,
		failIfNotExist:  failIfNotExist,
		tmpFilePath:     tmpFilePath,
		ignoreUnarchive: false,
		archiver:        archiver,
		backoff:         mockBackOff,
		log:             log.Sugar(),
		fs:              fs,
	}
	clientErr := minio.ErrorResponse{
		Code:    "keyDownloadFailed",
		Message: "The specified key download failed.",
		Key:     key,
	}
	mockBackOff.EXPECT().Reset()
	mockMinio.EXPECT().Stat(key).Return("", nil, nil)
	mockMinio.EXPECT().Download(ctx, key, gomock.Any()).Return(clientErr)
	mockBackOff.EXPECT().NextBackOff().Return(backoff.Stop)
	err := s.Run(ctx)
	assert.NotEqual(t, err, nil)
}

func TestRestoreCacheDownloadXXHashCalculateErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	key := "key"
	id := "test"
	tmpFilePath := "/tmp"
	failIfNotExist := true

	archiver := marchive.NewMockArchiver(ctrl)
	fs := filesystem.NewMockFileSystem(ctrl)
	mockBackOff := mbackoff.NewMockBackOff(ctrl)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	mockMinio := mminio.NewMockClient(ctrl)
	metadata := make(map[string]string)
	metadata[xxHashSumKey] = "test"

	oldClient := newMinioClient
	defer func() { newMinioClient = oldClient }()
	newMinioClient = func(log *zap.SugaredLogger) (uminio.Client, error) {
		return mockMinio, nil
	}

	s := &restoreCacheStep{
		id:              id,
		displayName:     "save cache test step",
		key:             key,
		failIfNotExist:  failIfNotExist,
		tmpFilePath:     tmpFilePath,
		ignoreUnarchive: false,
		archiver:        archiver,
		backoff:         mockBackOff,
		log:             log.Sugar(),
		fs:              fs,
	}

	mockBackOff.EXPECT().Reset()
	mockMinio.EXPECT().Stat(key).Return("", metadata, nil)
	mockMinio.EXPECT().Download(ctx, key, gomock.Any()).Return(nil)
	fs.EXPECT().Open(gomock.Any()).Return(nil, os.ErrNotExist)
	mockBackOff.EXPECT().NextBackOff().Return(backoff.Stop)
	err := s.Run(ctx)
	assert.NotEqual(t, err, nil)
}

func TestRestoreCacheDownloadIntegrityErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	key := "key"
	id := "test"
	tmpFilePath := "/tmp"
	failIfNotExist := true

	archiver := marchive.NewMockArchiver(ctrl)
	fs := filesystem.NewMockFileSystem(ctrl)
	mockBackOff := mbackoff.NewMockBackOff(ctrl)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	mockMinio := mminio.NewMockClient(ctrl)
	mockFile := filesystem.NewMockFile(ctrl)
	metadata := make(map[string]string)
	metadata[xxHashSumKey] = "test"

	oldClient := newMinioClient
	defer func() { newMinioClient = oldClient }()
	newMinioClient = func(log *zap.SugaredLogger) (uminio.Client, error) {
		return mockMinio, nil
	}

	s := &restoreCacheStep{
		id:              id,
		displayName:     "save cache test step",
		key:             key,
		failIfNotExist:  failIfNotExist,
		tmpFilePath:     tmpFilePath,
		ignoreUnarchive: false,
		archiver:        archiver,
		backoff:         mockBackOff,
		log:             log.Sugar(),
		fs:              fs,
	}

	mockBackOff.EXPECT().Reset()
	mockMinio.EXPECT().Stat(key).Return("", metadata, nil)
	mockMinio.EXPECT().Download(ctx, key, gomock.Any()).Return(nil)
	fs.EXPECT().Open(gomock.Any()).Return(mockFile, nil)
	fs.EXPECT().Copy(gomock.Any(), mockFile).Return(int64(0), nil)
	mockFile.EXPECT().Close().Return(nil)
	mockBackOff.EXPECT().NextBackOff().Return(backoff.Stop)
	err := s.Run(ctx)
	assert.NotEqual(t, err, nil)
}

func TestRestoreCacheUnarchiveErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	key := "key"
	id := "test"
	tmpFilePath := "/tmp"
	failIfNotExist := true

	archiver := marchive.NewMockArchiver(ctrl)
	fs := filesystem.NewMockFileSystem(ctrl)
	mockBackOff := mbackoff.NewMockBackOff(ctrl)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	mockMinio := mminio.NewMockClient(ctrl)
	mockFile := filesystem.NewMockFile(ctrl)
	keyHash := strconv.FormatUint(xxhash.New().Sum64(), 10)
	metadata := make(map[string]string)
	metadata[xxHashSumKey] = keyHash

	oldClient := newMinioClient
	defer func() { newMinioClient = oldClient }()
	newMinioClient = func(log *zap.SugaredLogger) (uminio.Client, error) {
		return mockMinio, nil
	}

	s := &restoreCacheStep{
		id:              id,
		displayName:     "save cache test step",
		key:             key,
		failIfNotExist:  failIfNotExist,
		tmpFilePath:     tmpFilePath,
		ignoreUnarchive: false,
		archiver:        archiver,
		backoff:         mockBackOff,
		log:             log.Sugar(),
		fs:              fs,
	}

	fi := fileInfo{}

	mockBackOff.EXPECT().Reset()
	mockMinio.EXPECT().Stat(key).Return("", metadata, nil)
	mockMinio.EXPECT().Download(ctx, key, gomock.Any()).Return(nil)
	fs.EXPECT().Open(gomock.Any()).Return(mockFile, nil)
	fs.EXPECT().Copy(gomock.Any(), mockFile).Return(int64(0), nil)
	fs.EXPECT().Stat(gomock.Any()).Return(fi, nil)
	mockFile.EXPECT().Close().Return(nil)
	archiver.EXPECT().Unarchive(gomock.Any(), "").Return(errors.New("unarchive err"))
	err := s.Run(ctx)
	assert.NotEqual(t, err, nil)
}

func TestRestoreCacheSuccess(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	key := "key"
	id := "test"
	tmpFilePath := "/tmp"
	failIfNotExist := true

	archiver := marchive.NewMockArchiver(ctrl)
	fs := filesystem.NewMockFileSystem(ctrl)
	mockBackOff := mbackoff.NewMockBackOff(ctrl)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	mockMinio := mminio.NewMockClient(ctrl)
	mockFile := filesystem.NewMockFile(ctrl)
	keyHash := strconv.FormatUint(xxhash.New().Sum64(), 10)
	metadata := make(map[string]string)
	metadata[xxHashSumKey] = keyHash

	oldClient := newMinioClient
	defer func() { newMinioClient = oldClient }()
	newMinioClient = func(log *zap.SugaredLogger) (uminio.Client, error) {
		return mockMinio, nil
	}

	s := &restoreCacheStep{
		id:              id,
		displayName:     "save cache test step",
		key:             key,
		failIfNotExist:  failIfNotExist,
		tmpFilePath:     tmpFilePath,
		ignoreUnarchive: false,
		archiver:        archiver,
		backoff:         mockBackOff,
		log:             log.Sugar(),
		fs:              fs,
	}

	fi := fileInfo{}

	mockBackOff.EXPECT().Reset()
	mockMinio.EXPECT().Stat(key).Return("", metadata, nil)
	mockMinio.EXPECT().Download(ctx, key, gomock.Any()).Return(nil)
	fs.EXPECT().Open(gomock.Any()).Return(mockFile, nil)
	fs.EXPECT().Copy(gomock.Any(), mockFile).Return(int64(0), nil)
	fs.EXPECT().Stat(gomock.Any()).Return(fi, nil)
	mockFile.EXPECT().Close().Return(nil)
	archiver.EXPECT().Unarchive(gomock.Any(), "").Return(nil)
	err := s.Run(ctx)
	assert.Equal(t, err, nil)
}

func TestRestoreCacheResolveJEXL(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	jKey := "${step1.output.foo}"
	keyVal := "bar"

	tests := []struct {
		name        string
		key         string
		resolvedKey string
		jexlEvalRet map[string]string
		jexlEvalErr error
		expectedErr bool
	}{
		{
			name:        "jexl evaluate error",
			key:         jKey,
			jexlEvalRet: nil,
			jexlEvalErr: errors.New("evaluation failed"),
			expectedErr: true,
		},
		{
			name:        "jexl successfully evaluated",
			key:         jKey,
			jexlEvalRet: map[string]string{jKey: keyVal},
			jexlEvalErr: nil,
			resolvedKey: keyVal,
			expectedErr: false,
		},
	}
	oldJEXLEval := evaluateJEXL
	defer func() { evaluateJEXL = oldJEXLEval }()
	for _, tc := range tests {
		s := &restoreCacheStep{
			key: tc.key,
			log: log.Sugar(),
		}
		// Initialize a mock CI addon
		evaluateJEXL = func(ctx context.Context, stepID string, expressions []string, o output.StageOutput,
			isSkipCondition bool, log *zap.SugaredLogger) (map[string]string, error) {
			return tc.jexlEvalRet, tc.jexlEvalErr
		}
		got := s.resolveJEXL(ctx)
		if tc.expectedErr == (got == nil) {
			t.Fatalf("%s: expected error: %v, got: %v", tc.name, tc.expectedErr, got)
		}

		if got == nil {
			assert.Equal(t, s.key, tc.resolvedKey)
		}
	}
}

func TestRestoreCacheResolveExpression(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	tmplKey1 := "hello-{{world"
	tmplKey2 := "hello-{{ epoch }}"

	tests := []struct {
		name        string
		key         string
		expectedErr bool
	}{
		{
			name:        "template error",
			key:         tmplKey1,
			expectedErr: true,
		},
		{
			name:        "template cache key successfully evaluated",
			key:         tmplKey2,
			expectedErr: false,
		},
	}
	for _, tc := range tests {
		s := &restoreCacheStep{
			key: tc.key,
			log: log.Sugar(),
		}
		got := s.resolveExpression(ctx)
		if tc.expectedErr == (got == nil) {
			t.Fatalf("%s: expected error: %v, got: %v", tc.name, tc.expectedErr, got)
		}
		if got == nil {
			fmt.Println(s.key)
		}
	}
}
