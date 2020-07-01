package steps

import (
	"context"
	"fmt"
	"path/filepath"
	"time"

	"github.com/cenkalti/backoff/v4"
	"github.com/pkg/errors"
	"github.com/wings-software/portal/commons/go/lib/archive"
	"github.com/wings-software/portal/commons/go/lib/filesystem"
	"github.com/wings-software/portal/commons/go/lib/utils"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

const (
	saveCacheMaxRetries = 5
)

//go:generate mockgen -source save_cache.go -package=steps -destination mocks/save_cache_mock.go SaveCacheStep

// SaveCacheStep represents interface to execute a save cache step
type SaveCacheStep interface {
	Run(ctx context.Context) error
}

type saveCacheStep struct {
	id          string
	displayName string
	key         string
	paths       []string
	partSize    uint64
	tmpFilePath string
	archiver    archive.Archiver
	backoff     backoff.BackOff
	log         *zap.SugaredLogger
	fs          filesystem.FileSystem
}

// NewSaveCacheStep creates a save cache step executor
func NewSaveCacheStep(step *pb.Step, tmpFilePath string, fs filesystem.FileSystem,
	log *zap.SugaredLogger) SaveCacheStep {
	archiver := archive.NewArchiver(archiveFormat, fs, log)
	backoff := utils.WithMaxRetries(utils.NewExponentialBackOffFactory(), saveCacheMaxRetries).NewBackOff()

	r := step.GetSaveCache()
	return &saveCacheStep{
		id:          step.GetId(),
		displayName: step.GetDisplayName(),
		paths:       r.GetPaths(),
		key:         r.GetKey(),
		partSize:    0,
		tmpFilePath: tmpFilePath,
		archiver:    archiver,
		backoff:     backoff,
		fs:          fs,
		log:         log,
	}
}

func (s *saveCacheStep) Run(ctx context.Context) error {
	start := time.Now()
	tmpArchivePath := filepath.Join(s.tmpFilePath, s.id)
	err := s.archiveFiles(tmpArchivePath)
	if err != nil {
		s.log.Warnw(
			"failed to archive files",
			"step_id", s.id,
			"key", s.key,
			"files", s.paths,
			"elapsed_time_ms", utils.TimeSince(start),
			zap.Error(err),
		)
		return err
	}

	xxhashSum, err := getFileXXHash(tmpArchivePath, s.fs)
	if err != nil {
		s.log.Warnw(
			"failed to compute xxhash",
			"step_id", s.id,
			"key", s.key,
			"elapsed_time_ms", utils.TimeSince(start),
			zap.Error(err),
		)
		return err
	}

	err = s.uploadWithRetries(ctx, xxhashSum, tmpArchivePath)
	if err != nil {
		s.log.Warnw(
			"error while uploading file to cache",
			"step_id", s.id,
			"key", s.key,
			"elapsed_time_ms", utils.TimeSince(start),
			zap.Error(err),
		)
		return err
	}

	s.log.Infow(
		"Successfully saved cache",
		"step_id", s.id,
		"key", s.key,
		"elapsed_time_ms", utils.TimeSince(start),
	)
	return nil
}

// Archive the files
func (s *saveCacheStep) archiveFiles(archivePath string) error {
	err := s.archiver.Archive(s.paths, archivePath)
	if err != nil {
		return errors.Wrap(err, fmt.Sprintf("failed to archive files: %s", s.paths))
	}

	return nil
}

func (s *saveCacheStep) uploadWithRetries(ctx context.Context, xxhashSum, tmpArchivePath string) error {
	uploader := func() error {
		start := time.Now()
		err := s.upload(ctx, xxhashSum, tmpArchivePath)
		if err != nil {
			s.log.Warnw(
				"failed to upload to cache",
				"step_id", s.id,
				"key", s.key,
				"elapsed_time_ms", utils.TimeSince(start),
				zap.Error(err),
			)
			return err
		}
		return nil
	}
	err := backoff.Retry(uploader, s.backoff)
	if err != nil {
		return errors.Wrap(err, fmt.Sprintf("failed to upload key %s with retries", s.key))
	}
	return nil
}

func (s *saveCacheStep) upload(ctx context.Context, xxhashSum, tmpArchivePath string) error {
	c, err := newMinioClient(s.log)
	if err != nil {
		return errors.Wrap(err, "failed to create MinIO client")
	}

	// Check whether key is already present with same hashsum. If present, skip save cache.
	_, uMetadata, err := c.Stat(s.key)
	if err == nil {
		uXXHashSum, found := uMetadata[xxHashSumKey]
		if found {
			s.log.Debugw("Key already exists", "key", s.key, "existing_xxhash",
				uXXHashSum, "current_xxhash", xxhashSum)
			if uXXHashSum == xxhashSum {
				s.log.Infow("Key is already cached", "step_id", s.id, "key", s.key)
				return nil
			}
		}
	}

	metadata := make(map[string]string)
	metadata[xxHashSumKey] = xxhashSum
	err = c.UploadWithOpts(ctx, s.key, tmpArchivePath, metadata, true, s.partSize)
	if err != nil {
		return errors.Wrap(err, fmt.Sprintf("failed to upload key: %s", s.key))
	}
	return nil
}
