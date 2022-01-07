// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package filesystem

import (
	"io"
	"io/ioutil"
	"os"
	"path"
	"sync"

	"github.com/hashicorp/go-multierror"
	"github.com/wings-software/portal/commons/go/lib/logs"
)

//go:generate mockgen -source filesystem.go -destination filesystem_mock.go -package filesystem FileSystem File
// Mocked files are generated in the same directory rather than mocks folder since generated mocked files are unable to find the dependencies.
// This issue comes up when an interface having methods that returns an interface is mocked.

//FileSystem provides mock-able interfaces for common filesystem operations
type FileSystem interface {
	CloseWriters() error
	ReadFile(filename string, op func(io.Reader) error) error
	Open(name string) (File, error)
	Stat(name string) (os.FileInfo, error)
	Create(name string) (File, error)
	Copy(dst io.Writer, src io.Reader) (int64, error)
	Remove(name string) error
	Setenv(name, value string) error
	Unsetenv(name string) error
	MkdirAll(name string, perm os.FileMode) error
	WriteFile(filename string, op func(at io.WriterAt) error) error
}

// File represents methods allowed for a *os.File.
type File interface {
	io.Closer
	io.Reader
	io.ReaderAt
	io.Writer
	io.Seeker

	Stat() (os.FileInfo, error)
}

//osFileSystem implements a FileSystem that reads from and writes to the local OS file system.
type osFileSystem struct {
	log logs.SugaredLoggerIface
	// A sync controlled map of things being written to. Adding to and removing from the map are protected
	writeFileHandles struct {
		sync.Mutex
		Map map[string]*fileHandle //key=filename, value=fileHandle
	}
}

type fileHandle struct {
	//Filename is the eventual name of the file, once writing is finished.
	FileName string
	//File is the temporary file being written to
	Tmpfile *os.File
}

func newFileHandle(fileName string) *fileHandle {
	return &fileHandle{FileName: fileName}
}

func (*osFileSystem) Open(name string) (File, error)                   { return os.Open(name) }
func (*osFileSystem) Create(name string) (File, error)                 { return os.Create(name) }
func (*osFileSystem) Stat(name string) (os.FileInfo, error)            { return os.Stat(name) }
func (*osFileSystem) Copy(dst io.Writer, src io.Reader) (int64, error) { return io.Copy(dst, src) }
func (*osFileSystem) Remove(name string) error                         { return os.Remove(name) }
func (*osFileSystem) Setenv(name, value string) error                  { return os.Setenv(name, value) }
func (*osFileSystem) Unsetenv(name string) error                       { return os.Unsetenv(name) }
func (*osFileSystem) MkdirAll(name string, perm os.FileMode) error     { return os.MkdirAll(name, perm) }
func (*osFileSystem) ReadFile(filename string, op func(io.Reader) error) error {
	f, err := os.Open(filename)
	if err != nil {
		return err
	}
	defer f.Close()
	return op(f)
}

//NewOSFileSystem returns a new FileSystem backed by the real O.S
func NewOSFileSystem(log logs.SugaredLoggerIface) FileSystem {
	fs := &osFileSystem{
		log: log,
	}
	fs.writeFileHandles.Map = make(map[string]*fileHandle)
	return fs
}

//Close closes the fileHandle's tmp file and renames it to its final filename, returning an error if either of those
// operations fail.
func (f *fileHandle) Close() error {
	if f.Tmpfile == nil {
		return nil
	}

	if err := f.Tmpfile.Close(); err != nil {
		return err
	}

	if err := os.Rename(f.Tmpfile.Name(), f.FileName); err != nil {
		return err
	}
	return nil
}

//WriteFile writes a file, treating all of op as an atomic function using this technique:
// a temp file is written then moved into place after writing is done
func (fs *osFileSystem) WriteFile(filename string, op func(at io.WriterAt) error) error {
	// If temp and destination files are not in target directory, there might be LinkError
	//	like 'invalid cross-device link'.
	//This occurs when they are not on the same block device/filesystem/partition/etc

	tmpfile, err := ioutil.TempFile(path.Dir(filename), path.Base(filename))
	if err != nil {
		return err
	}

	f := &fileHandle{
		FileName: filename,
		Tmpfile:  tmpfile,
	}

	fs.addFileHandle(f)
	defer fs.removeFileHandle(f.Tmpfile.Name())

	//write to the temp file
	err = op(tmpfile)
	if err != nil {
		_ = tmpfile.Close()
		return err
	}

	//flush pending writes, close  temp file and rename to desired file name
	return f.Close()
}

func (fs *osFileSystem) addFileHandle(handle *fileHandle) {
	fs.writeFileHandles.Lock()
	fs.writeFileHandles.Map[handle.Tmpfile.Name()] = handle
	fs.writeFileHandles.Unlock()
}

func (fs *osFileSystem) removeFileHandle(name string) *fileHandle {
	fs.writeFileHandles.Lock()
	handle := fs.writeFileHandles.Map[name]
	delete(fs.writeFileHandles.Map, name)
	fs.writeFileHandles.Unlock()

	return handle
}

// CloseWriters forecefully but gracefully shuts down all of the file system's currently open writers.
//Writing to a file is PREVENTED until this is finished. Returns and error if anything failed to write
//but this doesnt not necessarily mean the operation failed entirely, only that closing at least one
//of the file handles failed
func (fs *osFileSystem) CloseWriters() error {
	fs.writeFileHandles.Lock()

	var errs error
	for tmpfile, fileHandle := range fs.writeFileHandles.Map {
		if err := fileHandle.Close(); errs != nil {
			errs = multierror.Append(errs, err)
		}
		delete(fs.writeFileHandles.Map, tmpfile)
	}
	fs.writeFileHandles.Unlock()
	return errs
}
