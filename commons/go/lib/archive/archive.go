// Copyright 2020 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package archive

import (
	"archive/tar"
	"compress/gzip"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"time"

	"github.com/pkg/errors"
	"github.com/wings-software/portal/commons/go/lib/filesystem"
	"github.com/wings-software/portal/commons/go/lib/utils"
	"go.uber.org/zap"
)

// Format type
type Format int

const (
	// TarFormat is tar file format
	TarFormat Format = iota
	// GzipFormat is gzip file format
	GzipFormat
)

//go:generate mockgen -source archive.go -package=archive -destination mocks/archive_mock.go Archiver

// Archiver represents an interface to archive or un-archive files.
type Archiver interface {
	Archive(srcFilePaths []string, dstFilePath string) error
	Unarchive(filename, dstPath string) error
}

type archiver struct {
	format Format
	fs     filesystem.FileSystem
	log    *zap.SugaredLogger
}

// NewArchiver creates an archiver and returns it.
func NewArchiver(format Format, fs filesystem.FileSystem, log *zap.SugaredLogger) Archiver {
	return &archiver{format, fs, log}
}

// Archives files present in srcFilePaths to tar/gzip format
func (a *archiver) Archive(srcFilePaths []string, dstFilePath string) error {
	start := time.Now()
	out, err := a.fs.Create(dstFilePath)
	if err != nil {
		a.log.Warnw(
			"failed to open destination archive file",
			"destination_file_path", dstFilePath,
			"elapsed_time_ms", utils.TimeSince(start),
			zap.Error(err),
		)
		return err
	}
	defer out.Close()

	var buf io.WriteCloser = out
	if a.format == GzipFormat {
		buf = gzip.NewWriter(out)
		defer buf.Close()
	}

	tw := tar.NewWriter(buf)
	defer tw.Close()

	pathsPresentInArchive := make(map[string]bool)
	for _, filePath := range srcFilePaths {
		err := a.addSrcPathToArchive(tw, filePath, pathsPresentInArchive)
		if err != nil {
			a.log.Warnw(
				"failed to archive file",
				"file_path", filePath,
				"elapsed_time_ms", utils.TimeSince(start),
				zap.Error(err),
			)
			return err
		}
	}

	a.log.Infow(
		"Archived files",
		"source_files", srcFilePaths,
		"destination_file_path", dstFilePath,
		"elapsed_time_ms", utils.TimeSince(start),
	)
	return nil
}

// Adds all the files present in srcfilePath to the archive.
// srcFilePath can be a file/directory or even a regex consisting of files and directories.
func (a *archiver) addSrcPathToArchive(tw *tar.Writer, srcFilePath string, pathsPresentInArchive map[string]bool) error {
	// Resolve ~ in file path
	path, err := filesystem.ExpandTilde(srcFilePath)
	if err != nil {
		return errors.Wrap(err, fmt.Sprintf("failed to expand %s", srcFilePath))
	}

	// Resolve regex in file path
	files, err := filepath.Glob(path)
	if err != nil {
		return errors.Wrap(err, fmt.Sprintf("failed to resolve path: %s", path))
	}

	a.log.Infow("Adding files to archive", "files", files)
	for _, file := range files {
		err := filepath.Walk(file, func(path string, info os.FileInfo, err error) error {
			if err != nil {
				return errors.Wrap(err, fmt.Sprintf("failed to iterate files at path: %s", file))
			}
			if _, present := pathsPresentInArchive[path]; present {
				return nil
			}
			// return on non-regular files
			if !(info.Mode().IsRegular() || info.IsDir()) {
				return nil
			}

			err = a.addToArchive(tw, info, path)
			if err != nil {
				return errors.Wrap(err, fmt.Sprintf("failed to add file %s to archive", path))
			}

			pathsPresentInArchive[path] = true
			return nil
		})
		if err != nil {
			return errors.Wrap(err, fmt.Sprintf("failed to archive files at path: %s", file))
		}
	}
	return nil
}

// Adds a file to the archive.
// info is os file info of the file to archive.
// filename is the absolute path of file to archive.
func (a *archiver) addToArchive(tw *tar.Writer, info os.FileInfo, filename string) error {
	header, err := tar.FileInfoHeader(info, info.Name())
	if err != nil {
		return errors.Wrap(err, fmt.Sprintf("failed to create header for file: %s", filename))
	}

	// Set full path as file header name, otherwise, directory structure won't be preserved.
	// https://golang.org/src/archive/tar/common.go?#L626
	header.Name = filename

	// Writes the file header to the archive
	err = tw.WriteHeader(header)
	if err != nil {
		return errors.Wrap(err, fmt.Sprintf("failed to write file header for: %s", filename))
	}

	if info.IsDir() {
		return nil
	}

	// Opens the file to add to archive
	file, err := a.fs.Open(filename)
	if err != nil {
		return errors.Wrap(err, fmt.Sprintf("failed to open file: %s", filename))
	}
	defer file.Close()

	// Copy file content to archive
	_, err = a.fs.Copy(tw, file)
	if err != nil {
		return errors.Wrap(err, fmt.Sprintf("failed to copy file content for: %s", filename))
	}
	return nil
}

// Unarchive method un-archives the provided archived file.
// dstPath is the relative path where files need to be un-archived.
func (a *archiver) Unarchive(archivedFilePath, dstPath string) error {
	start := time.Now()
	r, err := a.fs.Open(archivedFilePath)
	if err != nil {
		a.log.Warnw(
			"failed to open archived file",
			"file_path", archivedFilePath,
			"elapsed_time_ms", utils.TimeSince(start),
			zap.Error(err),
		)
		return err
	}
	defer r.Close()

	err = a.unarchiveReader(r, dstPath)
	if err != nil {
		a.log.Warnw(
			"failed to un-archive file",
			"file_path", archivedFilePath,
			"elapsed_time_ms", utils.TimeSince(start),
			zap.Error(err),
		)
		return err
	}

	a.log.Infow(
		"Un-archived files",
		"input_file", archivedFilePath,
		"destination_path", dstPath,
		"elapsed_time_ms", utils.TimeSince(start),
	)
	return nil
}

// Unarchive a reader
// dstPath is the relative path where reader is unarchived.
func (a *archiver) unarchiveReader(r io.ReadCloser, dstPath string) error {
	var buf io.ReadCloser = r
	if a.format == GzipFormat {
		var err error
		buf, err = gzip.NewReader(r)
		if err != nil {
			return errors.Wrap(err, "failed to read gzip")
		}
		defer buf.Close()
	}

	madeDir := map[string]bool{}
	tr := tar.NewReader(buf)
	for {
		header, err := tr.Next()
		if err == io.EOF {
			return nil
		} else if err != nil {
			return errors.Wrap(err, "failed to read next file in archive")
		} else if header == nil {
			return errors.New("header is empty")
		}

		rel := filepath.FromSlash(header.Name)
		// the target location where the dir/file should be created
		target := filepath.Join(dstPath, rel)

		switch header.Typeflag {
		// create a directory if it doesn't exist
		case tar.TypeDir:
			if err := os.MkdirAll(target, 0755); err != nil {
				return errors.Wrap(err, fmt.Sprintf("failed to create directory at path %s", target))
			}
			madeDir[target] = true
		case tar.TypeReg:
			fdir := filepath.Dir(target)
			if !madeDir[fdir] {
				if err := os.MkdirAll(fdir, 0755); err != nil {
					return errors.Wrap(err, fmt.Sprintf("failed to create directory at path %s", fdir))
				}
				madeDir[fdir] = true
			}

			f, err := os.OpenFile(target, os.O_RDWR|os.O_CREATE|os.O_TRUNC, os.FileMode(header.Mode))
			if err != nil {
				return errors.Wrap(err, fmt.Sprintf("failed to create file at path %s", target))
			}
			n, err := io.Copy(f, tr)
			if closeErr := f.Close(); closeErr != nil && err == nil {
				err = closeErr
			}
			if err != nil {
				return errors.Wrap(err, fmt.Sprintf("error writing to %s", target))
			}
			if n != header.Size {
				return fmt.Errorf("only wrote %d bytes to %s; expected %d", n, target, header.Size)
			}
		default:
			return errors.New(
				fmt.Sprintf("unknown file type while un-archiving: %v in %s", header.Typeflag, header.Name))
		}
	}
}
