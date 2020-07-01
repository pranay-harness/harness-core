package steps

import (
	"fmt"
	"os"
	"strconv"

	"github.com/cespare/xxhash"
	"github.com/pkg/errors"
	"github.com/wings-software/portal/commons/go/lib/archive"
	"github.com/wings-software/portal/commons/go/lib/filesystem"
	"github.com/wings-software/portal/commons/go/lib/minio"
	"go.uber.org/zap"
)

const (
	minioEndpointEnv  = "ENDPOINT_MINIO"
	minioAccessKeyEnv = "ACCESS_KEY_MINIO"
	minioSecretKeyEnv = "SECRET_KEY_MINIO"
	minioBucketEnv    = "BUCKET_MINIO"

	archiveFormat = archive.TarFormat
	xxHashSumKey  = "Xxhash"
)

var newMinioClient = func(log *zap.SugaredLogger) (minio.Client, error) {
	endpoint, ok := os.LookupEnv(minioEndpointEnv)
	if !ok {
		return nil, errors.New(fmt.Sprintf("%s environment variable is not set", minioEndpointEnv))
	}
	accessKey, ok := os.LookupEnv(minioAccessKeyEnv)
	if !ok {
		return nil, errors.New(fmt.Sprintf("%s environment variable is not set", minioAccessKeyEnv))
	}
	secretKey, ok := os.LookupEnv(minioSecretKeyEnv)
	if !ok {
		return nil, errors.New(fmt.Sprintf("%s environment variable is not set", minioSecretKeyEnv))
	}
	bucket, ok := os.LookupEnv(minioBucketEnv)
	if !ok {
		return nil, errors.New(fmt.Sprintf("%s environment variable is not set", minioBucketEnv))
	}

	return minio.NewClient(endpoint, accessKey, secretKey, bucket, false, log)
}

// Returns xxHash of the given file
func getFileXXHash(filename string, fs filesystem.FileSystem) (string, error) {
	file, err := fs.Open(filename)
	if err != nil {
		return "", errors.Wrap(err, fmt.Sprintf("failed to open file: %s", filename))
	}
	defer file.Close()

	hasher := xxhash.New()
	if _, err := fs.Copy(hasher, file); err != nil {
		return "", errors.Wrap(err, fmt.Sprintf("failed to copy from file: %s", filename))
	}
	return strconv.FormatUint(hasher.Sum64(), 10), nil
}
