// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

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
	"github.com/wings-software/portal/product/ci/engine/output"
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
	stageOutput     output.StageOutput
	archiver        archive.Archiver
	backoff         backoff.BackOff
	log             *zap.SugaredLogger
	fs              filesystem.FileSystem
}

// NewRestoreCacheStep creates a restore cache step executor
func NewRestoreCacheStep(step *pb.UnitStep, tmpFilePath string, so output.StageOutput,
	fs filesystem.FileSystem, log *zap.SugaredLogger) RestoreCacheStep {
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
		stageOutput:     so,
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

// resolveJEXL resolves JEXL expressions present in restore cache step input
func (s *restoreCacheStep) resolveJEXL(ctx context.Context) error {
	// JEXL expressions are only present in key for restore cache
	key := s.key
	resolvedExprs, err := evaluateJEXL(ctx, s.id, []string{key}, s.stageOutput, false, s.log)
	if err != nil {
		return err
	}

	// Updating key with the resolved value of JEXL expression
	if val, ok := resolvedExprs[key]; ok {
		s.key = val
	}
	return nil
}

// resolveExpression resolves JEXL expressions & key checksum template present in
// in restore cache step input
func (s *restoreCacheStep) resolveExpression(ctx context.Context) error {
	if err := s.resolveJEXL(ctx); err != nil {
		return err
	}

	// Resolving checksum key template
	val, err := parseCacheKeyTmpl(s.key, s.log)
	if err != nil {
		return err
	}
	s.key = val

	return nil
}

func (s *restoreCacheStep) Run(ctx context.Context) error {
	start := time.Now()
	if err := s.resolveExpression(ctx); err != nil {
		return err
	}

	tmpArchivePath := filepath.Join(s.tmpFilePath, s.id)
	err := s.downloadWithRetries(ctx, tmpArchivePath)
	if err != nil {
		s.log.Errorw(
			"failed to download from cache",
			"key", s.key,
			"elapsed_time_ms", utils.TimeSince(start),
			zap.Error(err),
		)
		return err
	}

	if s.ignoreUnarchive {
		s.log.Infow(
			"Key does not exist. Continuing without restoring cache",
			"key", s.key,
			"elapsed_time_ms", utils.TimeSince(start),
		)
		return nil
	}

	err = s.unarchiveFiles(tmpArchivePath)
	if err != nil {
		s.log.Errorw(
			"failed to unarchive file",
			"file_path", tmpArchivePath,
			"key", s.key,
			"elapsed_time_ms", utils.TimeSince(start),
			zap.Error(err),
		)
		return err
	}

	s.log.Infow(
		"Successfully restored cache",
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
			s.log.Errorw(
				"failed to download from cache",
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
				s.log.Errorw(
					"Continuing on Key not exist error from cache",
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
		"key", s.key,
		"size", fi.Size(),
		"file_path", tmpArchivePath,
		"elapsed_time_ms", utils.TimeSince(start),
	)
	return nil
}
