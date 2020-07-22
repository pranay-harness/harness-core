package steps

import (
	"context"
	"fmt"
	"path/filepath"
	"time"

	"github.com/cenkalti/backoff/v4"
	"github.com/minio/minio-go/v6"
	"github.com/pkg/errors"
	"github.com/wings-software/portal/commons/go/lib/archive"
	"github.com/wings-software/portal/commons/go/lib/filesystem"
	"github.com/wings-software/portal/commons/go/lib/utils"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

const (
	restoreCacheMaxRetries = 5
)

//go:generate mockgen -source restore_cache.go -package=steps -destination mocks/restore_cache_mock.go RestoreCacheStep

// RestoreCacheStep provides an interface to interact execute restore cache step
type RestoreCacheStep interface {
	Run(ctx context.Context) error
}

type restoreCacheStep struct {
	id              string
	displayName     string
	key             string
	failIfNotExist  bool
	tmpFilePath     string
	ignoreUnarchive bool
	archiver        archive.Archiver
	backoff         backoff.BackOff
	log             *zap.SugaredLogger
	fs              filesystem.FileSystem
}

// NewRestoreCacheStep creates a restore cache step executor
func NewRestoreCacheStep(step *pb.Step, tmpFilePath string, fs filesystem.FileSystem,
	log *zap.SugaredLogger) RestoreCacheStep {
	archiver := archive.NewArchiver(archiveFormat, fs, log)
	backoff := utils.WithMaxRetries(utils.NewExponentialBackOffFactory(), restoreCacheMaxRetries).NewBackOff()

	r := step.GetRestoreCache()
	return &restoreCacheStep{
		id:              step.GetId(),
		displayName:     step.GetDisplayName(),
		key:             r.GetKey(),
		failIfNotExist:  r.GetFailIfNotExist(),
		tmpFilePath:     tmpFilePath,
		ignoreUnarchive: false,
		archiver:        archiver,
		backoff:         backoff,
		fs:              fs,
		log:             log,
	}
}

func (s *restoreCacheStep) unarchiveFiles(archivePath string) error {
	err := s.archiver.Unarchive(archivePath, "")
	if err != nil {
		return err
	}
	return nil
}

func (s *restoreCacheStep) Run(ctx context.Context) error {
	start := time.Now()
	tmpArchivePath := filepath.Join(s.tmpFilePath, s.id)
	err := s.downloadWithRetries(ctx, tmpArchivePath)
	if err != nil {
		s.log.Warnw(
			"failed to download from cache",
			"step_id", s.id,
			"key", s.key,
			"elapsed_time_ms", utils.TimeSince(start),
			zap.Error(err),
		)
		return err
	}

	if s.ignoreUnarchive {
		s.log.Infow(
			"Key does not exist. Continuing without restoring cache",
			"step_id", s.id,
			"key", s.key,
			"elapsed_time_ms", utils.TimeSince(start),
		)
		return nil
	}

	err = s.unarchiveFiles(tmpArchivePath)
	if err != nil {
		s.log.Warnw(
			"failed to unarchive file",
			"file_path", tmpArchivePath,
			"step_id", s.id,
			"key", s.key,
			"elapsed_time_ms", utils.TimeSince(start),
			zap.Error(err),
		)
		return err
	}

	s.log.Infow(
		"Successfully restored cache",
		"step_id", s.id,
		"key", s.key,
		"elapsed_time_ms", utils.TimeSince(start),
	)
	return nil
}

func (s *restoreCacheStep) downloadWithRetries(ctx context.Context, tmpArchivePath string) error {
	downloader := func() error {
		start := time.Now()
		err := s.download(ctx, tmpArchivePath)
		if err != nil {
			s.log.Warnw(
				"failed to download from cache",
				"step_id", s.id,
				"key", s.key,
				"elapsed_time_ms", utils.TimeSince(start),
				zap.Error(err),
			)
			return err
		}
		return nil
	}
	err := backoff.Retry(downloader, s.backoff)
	if err != nil {
		return errors.Wrap(err, fmt.Sprintf("failed to download key %s with retries", s.key))
	}
	return nil
}

func (s *restoreCacheStep) download(ctx context.Context, tmpArchivePath string) error {
	start := time.Now()
	c, err := newMinioClient(s.log)
	if err != nil {
		return errors.Wrap(err, "failed to create minio client")
	}

	_, metadata, err := c.Stat(s.key)
	if err != nil {
		resp := minio.ToErrorResponse(err)
		if resp.Code == "NoSuchKey" {
			if !s.failIfNotExist {
				s.ignoreUnarchive = true
				s.log.Warnw(
					"Continuing on Key not exist error from cache",
					"step_id", s.id,
					"key", s.key,
					"failIfNotExist", s.failIfNotExist,
				)
				return nil
			}
			return backoff.Permanent(errors.Wrap(err, fmt.Sprintf("failed to find key %s", s.key)))
		}
		return err
	}

	err = c.Download(ctx, s.key, tmpArchivePath)
	if err != nil {
		return errors.Wrap(err, fmt.Sprintf("failed to download key %s", s.key))
	}

	// Integrity check for downloaded file from object store MinIO.
	xxHash := metadata[xxHashSumKey]
	if xxHash != "" {
		downloadedFileXXHash, err := getFileXXHash(tmpArchivePath, s.fs, s.log)
		if err != nil {
			return errors.Wrap(err, fmt.Sprintf("failed to get file xxHash %s", s.key))
		}
		if xxHash != downloadedFileXXHash {
			msg := fmt.Sprintf("downloaded file hash %s does not match object store hash %s",
				downloadedFileXXHash, xxHash)
			return errors.New(msg)
		}
	}

	// Log downloaded file size
	fi, err := s.fs.Stat(tmpArchivePath)
	if err != nil {
		return errors.Wrap(err, fmt.Sprintf("failed to stat file: %s", tmpArchivePath))
	}
	s.log.Infow(
		"Downloaded file from cache",
		"step_id", s.id,
		"key", s.key,
		"size", fi.Size(),
		"file_path", tmpArchivePath,
		"elapsed_time_ms", utils.TimeSince(start),
	)
	return nil
}
