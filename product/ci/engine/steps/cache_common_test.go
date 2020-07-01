package steps

import (
	"os"
	"testing"

	"github.com/wings-software/portal/commons/go/lib/logs"
	"go.uber.org/zap"
)

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
