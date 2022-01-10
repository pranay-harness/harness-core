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
	"github.com/pkg/errors"
	"github.com/wings-software/portal/commons/go/lib/archive"
	"github.com/wings-software/portal/commons/go/lib/filesystem"
	"github.com/wings-software/portal/commons/go/lib/utils"
	"github.com/wings-software/portal/product/ci/engine/output"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

const (
	saveCacheMaxRetries = 5
	outputKey           = "key"
)

//go:generate mockgen -source save_cache.go -package=steps -destination mocks/save_cache_mock.go SaveCacheStep

// SaveCacheStep represents interface to execute a save cache step
type SaveCacheStep interface {
	Run(ctx context.Context) (*output.StepOutput, error)
}

type saveCacheStep struct {
	id          string
	displayName string
	key         string
	paths       []string
	partSize    uint64
	tmpFilePath string
	stageOutput output.StageOutput
	archiver    archive.Archiver
	backoff     backoff.BackOff
	log         *zap.SugaredLogger
	fs          filesystem.FileSystem
}

// NewSaveCacheStep creates a save cache step executor
func NewSaveCacheStep(step *pb.UnitStep, tmpFilePath string, so output.StageOutput,
	fs filesystem.FileSystem, log *zap.SugaredLogger) SaveCacheStep {
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
		stageOutput: so,
		archiver:    archiver,
		backoff:     backoff,
		fs:          fs,
		log:         log,
	}
}

// resolveJEXL resolves JEXL expressions present in save cache step input
func (s *saveCacheStep) resolveJEXL(ctx context.Context) error {
	// JEXL expressions are only present in key & paths for save cache
	var exprsToResolve []string
	exprsToResolve = append(exprsToResolve, s.key)
	for _, path := range s.paths {
		exprsToResolve = append(exprsToResolve, path)
	}

	resolvedExprs, err := evaluateJEXL(ctx, s.id, exprsToResolve, s.stageOutput, false, s.log)
	if err != nil {
		return err
	}

	// Updating key with the resolved value of JEXL expression
	if val, ok := resolvedExprs[s.key]; ok {
		s.key = val
	}
	var resolvedPaths []string
	for _, path := range s.paths {
		if val, ok := resolvedExprs[path]; ok {
			resolvedPaths = append(resolvedPaths, val)
		} else {
			resolvedPaths = append(resolvedPaths, path)
		}
	}
	s.paths = resolvedPaths
	return nil
}

// resolveExpression resolves JEXL expressions & key checksum template present in
// in save cache step input
func (s *saveCacheStep) resolveExpression(ctx context.Context) error {
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

func (s *saveCacheStep) Run(ctx context.Context) (*output.StepOutput, error) {
	start := time.Now()
	if err := s.resolveExpression(ctx); err != nil {
		return nil, err
	}

	tmpArchivePath := filepath.Join(s.tmpFilePath, s.id)
	err := s.archiveFiles(tmpArchivePath)
	if err != nil {
		s.log.Errorw(
			"failed to archive files",
			"key", s.key,
			"files", s.paths,
			"elapsed_time_ms", utils.TimeSince(start),
			zap.Error(err),
		)
		return nil, err
	}

	xxhashSum, err := getFileXXHash(tmpArchivePath, s.fs, s.log)
	if err != nil {
		s.log.Errorw(
			"failed to compute xxhash",
			"key", s.key,
			"elapsed_time_ms", utils.TimeSince(start),
			zap.Error(err),
		)
		return nil, err
	}

	err = s.uploadWithRetries(ctx, xxhashSum, tmpArchivePath)
	if err != nil {
		s.log.Errorw(
			"error while uploading file to cache",
			"key", s.key,
			"elapsed_time_ms", utils.TimeSince(start),
			zap.Error(err),
		)
		return nil, err
	}

	s.log.Infow(
		"Successfully saved cache",
		"key", s.key,
		"elapsed_time_ms", utils.TimeSince(start),
	)
	o := &output.StepOutput{}
	o.Output.Variables = map[string]string{outputKey: s.key}
	return o, nil
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
			s.log.Errorw(
				"failed to upload to cache",
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
				s.log.Infow("Key is already cached", "key", s.key)
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
