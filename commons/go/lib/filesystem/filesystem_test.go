// Copyright 2020 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package filesystem

import (
	"fmt"
	"io"
	"io/ioutil"
	"os"
	"path"
	"sync"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"go.uber.org/zap"
)

func WithTempDirectory(t *testing.T, test func(string)) {
	dir, err := ioutil.TempDir("", t.Name())
	if err != nil {
		t.Error(t)
	}
	defer func() {
		err := os.RemoveAll(dir)
		if err != nil {
			t.Error(t)
		}
	}()
	test(dir)
}

func TestOsFileSystem_ReadFile_Success(t *testing.T) {
	WithTempDirectory(t, func(dir string) {
		filename := path.Join(dir, t.Name())
		contents := []byte("upload artifact data")
		err := ioutil.WriteFile(filename, contents, 0644)
		defer os.Remove(filename)
		assert.NoError(t, err, "should not error out while writing data")

		fs := NewOSFileSystem(nil)
		var data []byte
		err = fs.ReadFile(filename, func(r io.Reader) error {
			data, err = ioutil.ReadAll(r)
			assert.NoError(t, err, "should not error while reading")
			return nil
		})
		assert.Equal(t, contents, data, "should read back the contents of the file")
		assert.NoError(t, err, "should not return an error if everything worked")
	})
}

func TestOsFileSystem_Open_Success(t *testing.T) {
	WithTempDirectory(t, func(dir string) {
		filename := path.Join(dir, t.Name())
		contents := []byte("upload artifact data")
		err := ioutil.WriteFile(filename, contents, 0644)
		defer os.Remove(filename)
		assert.NoError(t, err, "should not erro out while writing data")

		fs := NewOSFileSystem(nil)
		_, err = fs.Open(filename)
		assert.NoError(t, err, "should not return an error if everything worked")
	})
}

func TestOsFileSystem_Create_Success(t *testing.T) {
	WithTempDirectory(t, func(dir string) {
		filename := path.Join(dir, t.Name())
		contents := []byte("upload artifact data")
		err := ioutil.WriteFile(filename, contents, 0644)
		defer os.Remove(filename)
		assert.NoError(t, err, "should not erro out while writing data")

		fs := NewOSFileSystem(nil)
		_, err = fs.Create(filename)
		assert.NoError(t, err, "should not return an error if everything worked")
	})
}

func TestOsFileSystem_Stat_Success(t *testing.T) {
	WithTempDirectory(t, func(dir string) {
		filename := path.Join(dir, t.Name())
		contents := []byte("upload artifact data")
		err := ioutil.WriteFile(filename, contents, 0644)
		defer os.Remove(filename)
		assert.NoError(t, err, "should not erro out while writing data")

		fs := NewOSFileSystem(nil)
		fStat, err := fs.Stat(filename)
		assert.Equal(t, fStat.Name(), t.Name())
		assert.NoError(t, err, "should not return an error if everything worked")
	})
}

func TestOsFileSystem_Copy_Success(t *testing.T) {
	WithTempDirectory(t, func(dir string) {
		srcfilename := path.Join(dir, t.Name()+"src")
		dstfilename := path.Join(dir, t.Name()+"dst")
		contents := []byte("upload artifact data")
		err := ioutil.WriteFile(srcfilename, contents, 0644)
		defer os.Remove(srcfilename)
		assert.NoError(t, err, "should not erro out while writing data")

		srcFh, err := os.Open(srcfilename)
		assert.NoError(t, err, "should not error out while opening file")
		dstFh, err := os.Create(dstfilename)
		assert.NoError(t, err, "should not error out while creating file")

		fs := NewOSFileSystem(nil)
		size, err := fs.Copy(dstFh, srcFh)
		assert.Equal(t, size, int64(len(contents)))
		assert.NoError(t, err, "should not return an error if everything worked")
	})
}
func TestFileHandle_Close(t *testing.T) {
	WithTempDirectory(t, func(dir string) {
		filename := path.Join(dir, t.Name())
		fh := newFileHandle(filename)
		err := fh.Close()
		assert.NoError(t, err, "file should be closed successfully")
	})
}

func TestOsFileSystem_ReadFile_ErrOpening(t *testing.T) {
	WithTempDirectory(t, func(dir string) {
		fs := NewOSFileSystem(nil)
		err := fs.ReadFile("TestOsFileSystem_ReadFile_ErrOpening", func(r io.Reader) error {
			assert.Fail(t, "should not have called callback")
			return nil
		})
		assert.Error(t, err, "should error if it cannot read the file")
	})

}

func TestOsFileSystem_ReadFile_ErrDuringCallback(t *testing.T) {
	WithTempDirectory(t, func(dir string) {
		filename := path.Join(dir, t.Name())
		content := []byte("covid-19 pandemic has disrupted the whole planet")
		err := ioutil.WriteFile(filename, content, 0644)
		assert.NoError(t, err, "should not error out while writing data")

		fs := NewOSFileSystem(nil)
		err = fs.ReadFile(filename, func(r io.Reader) error {
			return fmt.Errorf("errored out")
		})
		assert.Error(t, err, "should return the error from the callback")
	})
}

func TestOsFileSystem_WriteFile_TempFileErr(t *testing.T) {
	WithTempDirectory(t, func(dir string) {
		fs := NewOSFileSystem(nil)
		err := fs.WriteFile("/nonexists/test", func(w io.WriterAt) error {
			t.Error("ioutil.Tempfile failure not handled correctly")
			return nil
		})
		assert.Error(t, err, "ioutil.Tempfile should have failed")
	})
}

func TestOsFileSystem_WriteFile_CreateFile(t *testing.T) {
	WithTempDirectory(t, func(dir string) {
		filename := path.Join(dir, t.Name())
		content := []byte("more than 30% of world population is in lockdown due to covid-19")
		os.Remove(filename)

		total := 0
		fs := NewOSFileSystem(nil)
		err := fs.WriteFile(filename, func(w io.WriterAt) error {
			n, err := w.WriteAt(content, 0)
			total = n
			assert.NoError(t, err, "should not error while writing")
			return nil
		})
		assert.NoError(t, err, "should not return an error")

		data, err := ioutil.ReadFile(filename)
		assert.NoError(t, err, "should not error while reading back file")
		assert.Equal(t, content, data, "file content should match what was written")
		assert.Equal(t, len(content), total, "expected writeAt to return bytes written")
	})
}

func TestFileHandle_Close_AllFileHandlersClosed(t *testing.T) {
	WithTempDirectory(t, func(dir string) {
		fs := NewOSFileSystem(zap.NewNop().Sugar())

		writeFile := func(filename string) int64 {
			written := int64(0)
			err := fs.WriteFile(filename, func(writer io.WriterAt) error {
				// write until we get an error trying to write more
				for {
					n, err := writer.WriteAt([]byte(filename), written)
					if err != nil {
						return err
					}
					written += int64(n)
					time.Sleep(4 * time.Millisecond)
				}
			})
			assert.NotNil(t, err, "expected writeFiles to error out when writing to closed file")
			return written
		}

		files := []string{"file1", "file2", "file3", "file4", "file5"}
		for i, file := range files {
			files[i] = path.Join(dir, file)
		}

		var wg sync.WaitGroup
		for _, file := range files {
			wg.Add(1)
			go func(f string) {
				defer wg.Done()
				written := writeFile(f)
				if written > 0 {
					data, err := ioutil.ReadFile(f)
					assert.Nil(t, err)
					assert.Equal(t, written, int64(len(data)))
				}
			}(file)
		}
		time.Sleep(25 * time.Millisecond) // let the files write for a while
		assert.NoError(t, fs.CloseWriters())
		wg.Wait()
	})

}
