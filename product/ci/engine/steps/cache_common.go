package steps

import (
	"fmt"
	"os"
	"strconv"
	"time"

	"github.com/cespare/xxhash"
	"github.com/pkg/errors"
	"github.com/wings-software/portal/commons/go/lib/archive"
	"github.com/wings-software/portal/commons/go/lib/filesystem"
	"github.com/wings-software/portal/commons/go/lib/minio"
	"github.com/wings-software/portal/commons/go/lib/utils"
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
func getFileXXHash(filename string, fs filesystem.FileSystem, log *zap.SugaredLogger) (string, error) {
	start := time.Now()
	file, err := fs.Open(filename)
	if err != nil {
		return "", errors.Wrap(err, fmt.Sprintf("failed to open file: %s", filename))
	}
	defer file.Close()

	hasher := xxhash.New()
	if _, err := fs.Copy(hasher, file); err != nil {
		return "", errors.Wrap(err, fmt.Sprintf("failed to copy from file: %s", filename))
	}
	hashSum := strconv.FormatUint(hasher.Sum64(), 10)

	log.Infow(
		"Calculated file xxhash",
		"file", filename,
		"elapsed_time_ms", utils.TimeSince(start),
	)
	return hashSum, nil
}
