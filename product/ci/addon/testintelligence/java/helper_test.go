package java

import (
	"context"
	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/filesystem"
	"github.com/wings-software/portal/commons/go/lib/logs"
	"go.uber.org/zap"
	"os"
	"testing"
)

const (
	pkgDetectTestdata = "testdata/pkg_detection"
)

func TestDetectJavaPkgs(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	log, _ := logs.GetObservedLogger(zap.InfoLevel)

	wd, err := os.Getwd()
	if err != nil {
		t.Fatalf("could not get working directory: %s", err)
	}
	wd = wd + pkgDetectTestdata

	oldGetWorkspace := getWorkspace
	defer func() {
		getWorkspace = oldGetWorkspace
	}()
	getWorkspace = func() (string, error) {
		return pkgDetectTestdata, nil
	}

	l, err := DetectPkgs(log.Sugar(), filesystem.NewOSFileSystem(log.Sugar()))
	assert.Contains(t, l, "com.google.test.test")
	assert.Contains(t, l, "xyz")
	assert.Contains(t, l, "test1.test1")
	assert.Len(t, l, 3)
	assert.Nil(t, err)
}
