// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package steps

import (
	"io/ioutil"
	"os"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/logs"
	"go.uber.org/zap"
)

func testWriteFile(fileName, content string, t *testing.T) {
	err := ioutil.WriteFile(fileName, []byte(content), 0644)
	assert.Nil(t, err)
}

func TestNewMinioClient(t *testing.T) {
	log, _ := logs.GetObservedLogger(zap.InfoLevel)
	tests := []struct {
		name        string
		expectedErr bool
		envVars     map[string]string
	}{
		{
			name:        "endpoint not set",
			expectedErr: true,
			envVars:     nil,
		},
		{
			name:        "access key not set",
			expectedErr: true,
			envVars: map[string]string{
				minioEndpointEnv: "1.1.1.1:9000",
			},
		},
		{
			name:        "secret key not set",
			expectedErr: true,
			envVars: map[string]string{
				minioEndpointEnv:  "1.1.1.1:9000",
				minioAccessKeyEnv: "minio",
			},
		},
		{
			name:        "bucket env not set",
			expectedErr: true,
			envVars: map[string]string{
				minioEndpointEnv:  "1.1.1.1:9000",
				minioAccessKeyEnv: "minio",
				minioSecretKeyEnv: "minio123",
			},
		},
	}

	for _, tc := range tests {
		if tc.envVars != nil {
			for k, v := range tc.envVars {
				if err := os.Setenv(k, v); err != nil {
					t.Fatalf("%s: failed to set environment variable: %s, %s", tc.name, k, v)
				}
			}
		}
		_, got := newMinioClient(log.Sugar())
		if tc.expectedErr == (got == nil) {
			t.Fatalf("%s: expected error: %v, got: %v", tc.name, tc.expectedErr, got)
		}
		if tc.envVars != nil {
			for k := range tc.envVars {
				if err := os.Unsetenv(k); err != nil {
					t.Fatalf("%s: failed to unset environment variable: %s", tc.name, k)
				}
			}
		}
	}
}

func TestParseKeySuccess(t *testing.T) {
	log := logs.NewBuilder().MustBuild().Sugar()
	fname := "/tmp/tmpl_success"
	content := "gopher"
	testWriteFile(fname, content, t)
	defer os.Remove(fname)

	key := `Welcome_{{ checksum "/tmp/tmpl_success" }}_{{ arch }}_{{ os }}_{{ epoch }}`
	s, err := parseCacheKeyTmpl(key, log)
	assert.Nil(t, err)
	assert.NotEqual(t, s, "")
}

// Checksum file not present.
func TestParseKeyFileNotFoundErr(t *testing.T) {
	log := logs.NewBuilder().MustBuild().Sugar()
	key := `Welcome_{{ checksum "/tmp/tmpl_err" }}_{{ arch }}_{{ os }}`
	_, err := parseCacheKeyTmpl(key, log)
	assert.NotNil(t, err)
}

// Invalid template.
func TestParseKeyFileTmplErr(t *testing.T) {
	log := logs.NewBuilder().MustBuild().Sugar()
	key := `Welcome_{{ checksum "/tmp/tmpl_err"`
	_, err := parseCacheKeyTmpl(key, log)
	assert.NotNil(t, err)
}
