package filesystem

import (
	"io"
	"io/ioutil"
	"os"
	"path"
	"testing"

	"github.com/stretchr/testify/assert"
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
		assert.NoError(t, err, "should not erro out while writing data")

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
