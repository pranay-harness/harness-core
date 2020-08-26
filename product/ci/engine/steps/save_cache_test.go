package steps

import (
	"context"
	"os"
	"strconv"
	"testing"

	"github.com/cenkalti/backoff/v4"
	"github.com/cespare/xxhash"
	"github.com/golang/mock/gomock"
	"github.com/pkg/errors"
	"github.com/stretchr/testify/assert"
	marchive "github.com/wings-software/portal/commons/go/lib/archive/mocks"
	"github.com/wings-software/portal/commons/go/lib/filesystem"
	"github.com/wings-software/portal/commons/go/lib/logs"
	"github.com/wings-software/portal/commons/go/lib/minio"
	mminio "github.com/wings-software/portal/commons/go/lib/minio/mocks"
	mbackoff "github.com/wings-software/portal/commons/go/lib/utils/mocks"
	"github.com/wings-software/portal/product/ci/engine/output"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

func getSaveStep(id, key string, paths []string) *pb.UnitStep {
	saveCacheStep := &pb.UnitStep_SaveCache{
		SaveCache: &pb.SaveCacheStep{
			Key:   key,
			Paths: paths,
		},
	}
	return &pb.UnitStep{
		Id:          id,
		DisplayName: "test save cache step",
		Step:        saveCacheStep,
	}
}

func TestNewSaveCacheStep(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	key := "key"
	paths := []string{"test"}
	id := "test"
	tmpFilePath := "/tmp"

	fs := filesystem.NewMockFileSystem(ctrl)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	step := getSaveStep(id, key, paths)
	NewSaveCacheStep(step, tmpFilePath, nil, fs, log.Sugar())
}

func TestSaveCacheRunWithArchiveErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	key := "key"
	paths := []string{"test"}
	id := "test"
	tmpFilePath := "/tmp"

	archiver := marchive.NewMockArchiver(ctrl)
	fs := filesystem.NewMockFileSystem(ctrl)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	s := &saveCacheStep{
		id:          id,
		displayName: "save cache test step",
		key:         key,
		paths:       paths,
		partSize:    0,
		tmpFilePath: tmpFilePath,
		archiver:    archiver,
		log:         log.Sugar(),
		fs:          fs,
	}

	archiver.EXPECT().Archive(paths, gomock.Any()).Return(os.ErrExist)

	err := s.Run(ctx)
	assert.NotEqual(t, err, nil)
}

func TestSaveCacheRunWithXXHashErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	key := "key"
	paths := []string{"test"}
	id := "test"
	tmpFilePath := "/tmp"
	filePath := "/tmp/test"

	archiver := marchive.NewMockArchiver(ctrl)
	fs := filesystem.NewMockFileSystem(ctrl)
	mockBackOff := mbackoff.NewMockBackOff(ctrl)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	s := &saveCacheStep{
		id:          id,
		displayName: "save cache test step",
		key:         key,
		paths:       paths,
		partSize:    0,
		tmpFilePath: tmpFilePath,
		archiver:    archiver,
		backoff:     mockBackOff,
		log:         log.Sugar(),
		fs:          fs,
	}

	archiver.EXPECT().Archive(paths, gomock.Any()).Return(nil)
	fs.EXPECT().Open(filePath).Return(nil, os.ErrExist)
	err := s.Run(ctx)
	assert.NotEqual(t, err, nil)
}

func TestSaveCacheRunWithMinioClientErr(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	key := "key"
	paths := []string{"test"}
	id := "test"
	tmpFilePath := "/tmp"
	filePath := "/tmp/test"

	archiver := marchive.NewMockArchiver(ctrl)
	fs := filesystem.NewMockFileSystem(ctrl)
	mockBackOff := mbackoff.NewMockBackOff(ctrl)
	mockFile := filesystem.NewMockFile(ctrl)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	s := &saveCacheStep{
		id:          id,
		displayName: "save cache test step",
		key:         key,
		paths:       paths,
		partSize:    0,
		tmpFilePath: tmpFilePath,
		archiver:    archiver,
		backoff:     mockBackOff,
		log:         log.Sugar(),
		fs:          fs,
	}

	archiver.EXPECT().Archive(paths, gomock.Any()).Return(nil)
	fs.EXPECT().Open(filePath).Return(mockFile, nil)
	fs.EXPECT().Copy(gomock.Any(), mockFile).Return(int64(0), nil)
	mockFile.EXPECT().Close().Return(nil)
	mockBackOff.EXPECT().Reset()
	mockBackOff.EXPECT().NextBackOff().Return(backoff.Stop)
	err := s.Run(ctx)
	assert.NotEqual(t, err, nil)
}

func TestSaveCacheRunWithUploadFailure(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	key := "key"
	paths := []string{"test"}
	id := "test"
	tmpFilePath := "/tmp"
	filePath := "/tmp/test"

	archiver := marchive.NewMockArchiver(ctrl)
	fs := filesystem.NewMockFileSystem(ctrl)
	mockBackOff := mbackoff.NewMockBackOff(ctrl)
	mockFile := filesystem.NewMockFile(ctrl)
	mockMinio := mminio.NewMockClient(ctrl)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	oldClient := newMinioClient
	defer func() { newMinioClient = oldClient }()
	newMinioClient = func(log *zap.SugaredLogger) (minio.Client, error) {
		return mockMinio, nil
	}

	s := &saveCacheStep{
		id:          id,
		displayName: "save cache test step",
		key:         key,
		paths:       paths,
		partSize:    0,
		tmpFilePath: tmpFilePath,
		archiver:    archiver,
		backoff:     mockBackOff,
		log:         log.Sugar(),
		fs:          fs,
	}

	archiver.EXPECT().Archive(paths, gomock.Any()).Return(nil)
	fs.EXPECT().Open(filePath).Return(mockFile, nil)
	fs.EXPECT().Copy(gomock.Any(), mockFile).Return(int64(0), nil)
	mockFile.EXPECT().Close().Return(nil)
	mockBackOff.EXPECT().Reset()
	mockMinio.EXPECT().Stat(key).Return("", nil, nil)
	mockMinio.EXPECT().UploadWithOpts(ctx, key, filePath, gomock.Any(), true, gomock.Any()).Return(os.ErrNotExist)
	mockBackOff.EXPECT().NextBackOff().Return(backoff.Stop)
	err := s.Run(ctx)
	assert.NotEqual(t, err, nil)
}

func TestSaveCacheRunWithUploadSuccess(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	key := "key"
	paths := []string{"test"}
	id := "test"
	tmpFilePath := "/tmp"
	filePath := "/tmp/test"

	archiver := marchive.NewMockArchiver(ctrl)
	fs := filesystem.NewMockFileSystem(ctrl)
	mockBackOff := mbackoff.NewMockBackOff(ctrl)
	mockFile := filesystem.NewMockFile(ctrl)
	mockMinio := mminio.NewMockClient(ctrl)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	oldClient := newMinioClient
	defer func() { newMinioClient = oldClient }()
	newMinioClient = func(log *zap.SugaredLogger) (minio.Client, error) {
		return mockMinio, nil
	}

	s := &saveCacheStep{
		id:          id,
		displayName: "save cache test step",
		key:         key,
		paths:       paths,
		partSize:    0,
		tmpFilePath: tmpFilePath,
		archiver:    archiver,
		backoff:     mockBackOff,
		log:         log.Sugar(),
		fs:          fs,
	}

	archiver.EXPECT().Archive(paths, gomock.Any()).Return(nil)
	fs.EXPECT().Open(filePath).Return(mockFile, nil)
	fs.EXPECT().Copy(gomock.Any(), mockFile).Return(int64(0), nil)
	mockFile.EXPECT().Close().Return(nil)
	mockBackOff.EXPECT().Reset()
	mockMinio.EXPECT().Stat(key).Return("", nil, nil)
	mockMinio.EXPECT().UploadWithOpts(ctx, key, filePath, gomock.Any(), true, gomock.Any()).Return(nil)
	err := s.Run(ctx)
	assert.Equal(t, err, nil)
}

func TestSaveCacheRunWithSameKeyExists(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	key := "key"
	paths := []string{"test"}
	id := "test"
	tmpFilePath := "/tmp"
	filePath := "/tmp/test"

	archiver := marchive.NewMockArchiver(ctrl)
	fs := filesystem.NewMockFileSystem(ctrl)
	mockBackOff := mbackoff.NewMockBackOff(ctrl)
	mockFile := filesystem.NewMockFile(ctrl)
	mockMinio := mminio.NewMockClient(ctrl)
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	keyHash := strconv.FormatUint(xxhash.New().Sum64(), 10)
	metadata := make(map[string]string)
	metadata[xxHashSumKey] = keyHash

	oldClient := newMinioClient
	defer func() { newMinioClient = oldClient }()
	newMinioClient = func(log *zap.SugaredLogger) (minio.Client, error) {
		return mockMinio, nil
	}

	s := &saveCacheStep{
		id:          id,
		displayName: "save cache test step",
		key:         key,
		paths:       paths,
		partSize:    0,
		tmpFilePath: tmpFilePath,
		archiver:    archiver,
		backoff:     mockBackOff,
		log:         log.Sugar(),
		fs:          fs,
	}

	archiver.EXPECT().Archive(paths, gomock.Any()).Return(nil)
	fs.EXPECT().Open(filePath).Return(mockFile, nil)
	fs.EXPECT().Copy(gomock.Any(), mockFile).Return(int64(0), nil)
	mockFile.EXPECT().Close().Return(nil)
	mockBackOff.EXPECT().Reset()
	mockMinio.EXPECT().Stat(key).Return("", metadata, nil)
	err := s.Run(ctx)
	assert.Equal(t, err, nil)
}

func TestSaveCacheResolveJEXL(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	jKey := "${step1.output.foo}"
	keyVal := "bar"
	jPath1 := "${step1.output.hello}"
	path1Val := "world"

	tests := []struct {
		name          string
		key           string
		paths         []string
		resolvedKey   string
		resolvedPaths []string
		jexlEvalRet   map[string]string
		jexlEvalErr   error
		expectedErr   bool
	}{
		{
			name:        "jexl evaluate error",
			key:         jKey,
			paths:       []string{jPath1},
			jexlEvalRet: nil,
			jexlEvalErr: errors.New("evaluation failed"),
			expectedErr: true,
		},
		{
			name:          "jexl successfully evaluated",
			key:           jKey,
			paths:         []string{jPath1},
			jexlEvalRet:   map[string]string{jKey: keyVal, jPath1: path1Val},
			jexlEvalErr:   nil,
			resolvedKey:   keyVal,
			resolvedPaths: []string{path1Val},
			expectedErr:   false,
		},
	}
	oldJEXLEval := evaluateJEXL
	defer func() { evaluateJEXL = oldJEXLEval }()
	for _, tc := range tests {
		s := &saveCacheStep{
			key:   tc.key,
			paths: tc.paths,
			log:   log.Sugar(),
		}
		// Initialize a mock CI addon
		evaluateJEXL = func(ctx context.Context, expressions []string, o output.StageOutput,
			log *zap.SugaredLogger) (map[string]string, error) {
			return tc.jexlEvalRet, tc.jexlEvalErr
		}
		got := s.resolveJEXL(ctx)
		if tc.expectedErr == (got == nil) {
			t.Fatalf("%s: expected error: %v, got: %v", tc.name, tc.expectedErr, got)
		}

		if got == nil {
			assert.Equal(t, s.key, tc.resolvedKey)
			assert.Equal(t, s.paths, tc.resolvedPaths)
		}

	}
}
