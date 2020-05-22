package filesystem

import (
	"io"
	"os"
	"sync"

	"github.com/wings-software/portal/commons/go/lib/logs"
)

//go:generate mockgen -source filesystem.go -destination mocks/filesystem_mock.go -package filesystem FileSystem

//FileSystem provides mock-able interfaces for common filesystem operations
type FileSystem interface {
	ReadFile(filename string, op func(io.Reader) error) error
}

//osFileSystem implements a FileSystem that reads from and writes to the local OS file system.
type osFileSystem struct {
	log logs.SugaredLoggerIface
	// A sync controlled map of things being written to. Adding to and removing from the map are protected
	writeFileHandles struct {
		sync.Mutex
		Map map[string]*fileHandle
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
	if err := f.Tmpfile.Close(); err != nil {
		return err
	}

	if err := os.Rename(f.Tmpfile.Name(), f.FileName); err != nil {
		return err
	}
	return nil
}
