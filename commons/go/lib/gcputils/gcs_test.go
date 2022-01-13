// Copyright 2020 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package gcputils

import (
	"context"
	"errors"
	"os"
	"testing"

	"cloud.google.com/go/storage"
	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/filesystem"
	"github.com/wings-software/portal/commons/go/lib/logs"
	"go.uber.org/zap"
	"google.golang.org/api/option"
)

func Test_gcs_DeleteObject(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	bucketName := "test-bucket"
	objectName := "test-object"

	client := NewMockStorageClient(ctrl)
	fs := filesystem.NewMockFileSystem(ctrl)
	bucketHandle := NewMockBucketHandle(ctrl)
	objectHandle := NewMockObjectHandle(ctrl)
	log, _ := logs.GetObservedLogger(zap.ErrorLevel)
	gcs := &gcs{client, fs, log.Sugar(), int64(5), ""}

	client.EXPECT().Bucket(bucketName).Return(bucketHandle)
	bucketHandle.EXPECT().Object(objectName).Return(objectHandle)
	objectHandle.EXPECT().Delete(gomock.Any()).Return(nil)

	err := gcs.DeleteObject(ctx, bucketName, objectName)
	assert.Equal(t, err, nil)
}

func Test_gcs_DeleteObject_NotExistError(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	bucketName := "test-bucket"
	objectName := "test-object"

	client := NewMockStorageClient(ctrl)
	fs := filesystem.NewMockFileSystem(ctrl)
	bucketHandle := NewMockBucketHandle(ctrl)
	objectHandle := NewMockObjectHandle(ctrl)
	log, _ := logs.GetObservedLogger(zap.ErrorLevel)
	gcs := &gcs{client, fs, log.Sugar(), int64(5), ""}

	client.EXPECT().Bucket(bucketName).Return(bucketHandle)
	bucketHandle.EXPECT().Object(objectName).Return(objectHandle)
	objectHandle.EXPECT().Delete(gomock.Any()).Return(storage.ErrObjectNotExist)

	err := gcs.DeleteObject(ctx, bucketName, objectName)
	assert.Equal(t, err, storage.ErrObjectNotExist)
}

func Test_gcs_GetObjectMetadata_NotExistError(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	bucketName := "test-bucket"
	objectName := "test-object"

	client := NewMockStorageClient(ctrl)
	fs := filesystem.NewMockFileSystem(ctrl)
	bucketHandle := NewMockBucketHandle(ctrl)
	objectHandle := NewMockObjectHandle(ctrl)
	log, _ := logs.GetObservedLogger(zap.ErrorLevel)
	gcs := &gcs{client, fs, log.Sugar(), int64(5), ""}

	client.EXPECT().Bucket(bucketName).Return(bucketHandle)
	bucketHandle.EXPECT().Object(objectName).Return(objectHandle)
	objectHandle.EXPECT().Attrs(gomock.Any()).Return(nil, storage.ErrObjectNotExist)

	_, err := gcs.GetObjectMetadata(ctx, bucketName, objectName)
	assert.Equal(t, err, storage.ErrObjectNotExist)
}

func Test_gcs_GetObjectMetadata(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	bucketName := "test-bucket"
	objectName := "test-object"

	metadataKey := "key"
	metadataVal := "val"
	metadata := make(map[string]string)
	metadata[metadataKey] = metadataVal
	mockObjectAttrs := &storage.ObjectAttrs{Bucket: bucketName, Name: objectName, Metadata: metadata}

	client := NewMockStorageClient(ctrl)
	fs := filesystem.NewMockFileSystem(ctrl)
	bucketHandle := NewMockBucketHandle(ctrl)
	objectHandle := NewMockObjectHandle(ctrl)
	log, _ := logs.GetObservedLogger(zap.ErrorLevel)
	gcs := &gcs{client, fs, log.Sugar(), int64(5), ""}

	client.EXPECT().Bucket(bucketName).Return(bucketHandle)
	bucketHandle.EXPECT().Object(objectName).Return(objectHandle)
	objectHandle.EXPECT().Attrs(gomock.Any()).Return(mockObjectAttrs, nil)

	m, err := gcs.GetObjectMetadata(ctx, bucketName, objectName)
	assert.Equal(t, err, nil)
	assert.Equal(t, m[metadataKey], metadataVal)
}

func Test_gcs_UpdateObjectMetadata(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	bucketName := "test-bucket"
	objectName := "test-object"

	metadataKey := "key"
	metadataVal := "val"
	metadata := make(map[string]string)
	metadata[metadataKey] = metadataVal
	mockObjectAttrs := &storage.ObjectAttrs{Bucket: bucketName, Name: objectName, Metadata: metadata}

	client := NewMockStorageClient(ctrl)
	fs := filesystem.NewMockFileSystem(ctrl)
	bucketHandle := NewMockBucketHandle(ctrl)
	objectHandle := NewMockObjectHandle(ctrl)
	log, _ := logs.GetObservedLogger(zap.ErrorLevel)
	gcs := &gcs{client, fs, log.Sugar(), int64(5), ""}

	client.EXPECT().Bucket(bucketName).Return(bucketHandle)
	bucketHandle.EXPECT().Object(objectName).Return(objectHandle)
	objectHandle.EXPECT().Update(gomock.Any(), gomock.Any()).Return(mockObjectAttrs, nil)

	err := gcs.UpdateObjectMetadata(ctx, bucketName, objectName, metadata)
	assert.Equal(t, err, nil)
}

func Test_gcs_UpdateObjectMetadata_ObjectNotFound(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	bucketName := "test-bucket"
	objectName := "test-object"

	metadataKey := "key"
	metadataVal := "val"
	metadata := make(map[string]string)
	metadata[metadataKey] = metadataVal
	mockObjectAttrs := &storage.ObjectAttrs{Bucket: bucketName, Name: objectName, Metadata: metadata}

	client := NewMockStorageClient(ctrl)
	fs := filesystem.NewMockFileSystem(ctrl)
	bucketHandle := NewMockBucketHandle(ctrl)
	objectHandle := NewMockObjectHandle(ctrl)
	log, _ := logs.GetObservedLogger(zap.ErrorLevel)
	gcs := &gcs{client, fs, log.Sugar(), int64(5), ""}

	client.EXPECT().Bucket(bucketName).Return(bucketHandle)
	bucketHandle.EXPECT().Object(objectName).Return(objectHandle)
	objectHandle.EXPECT().Update(gomock.Any(), gomock.Any()).Return(mockObjectAttrs, storage.ErrObjectNotExist)

	err := gcs.UpdateObjectMetadata(ctx, bucketName, objectName, metadata)
	assert.Equal(t, err, storage.ErrObjectNotExist)
}

func Test_gcs_UploadOject_FileOpenError(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	bucketName := "test-bucket"
	objectName := "test-object"
	srcFilePath := "/a/b"

	client := NewMockStorageClient(ctrl)
	fs := filesystem.NewMockFileSystem(ctrl)
	log, _ := logs.GetObservedLogger(zap.ErrorLevel)
	gcs := &gcs{client, fs, log.Sugar(), int64(5), ""}

	fs.EXPECT().Open(srcFilePath).Return(nil, os.ErrNotExist)
	err := gcs.UploadObject(ctx, bucketName, objectName, srcFilePath)
	assert.Equal(t, err, os.ErrNotExist)
}

func Test_gcs_UploadOject_IOCopyError(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	var errShortBuffer = errors.New("short buffer")

	bucketName := "test-bucket"
	objectName := "test-object"
	srcFilePath := "/a/b"

	client := NewMockStorageClient(ctrl)
	fs := filesystem.NewMockFileSystem(ctrl)
	file := filesystem.NewMockFile(ctrl)
	log, _ := logs.GetObservedLogger(zap.ErrorLevel)
	bucketHandle := NewMockBucketHandle(ctrl)
	objectHandle := NewMockObjectHandle(ctrl)
	objectWriter := NewMockStorageWriter(ctrl)
	fileSize := int64(100)
	gcs := &gcs{client, fs, log.Sugar(), int64(5), ""}

	fs.EXPECT().Open(srcFilePath).Return(file, nil)
	client.EXPECT().Bucket(bucketName).Return(bucketHandle)
	bucketHandle.EXPECT().Object(objectName).Return(objectHandle)
	objectHandle.EXPECT().NewWriter(gomock.Any()).Return(objectWriter)
	fs.EXPECT().Copy(objectWriter, file).Return(fileSize, errShortBuffer)
	file.EXPECT().Close().Return(nil)

	err := gcs.UploadObject(ctx, bucketName, objectName, srcFilePath)
	assert.Equal(t, err, errShortBuffer)
}

func Test_gcs_UploadOject_FileCloseError(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	var errCloseFile = errors.New("failed to close file")

	bucketName := "test-bucket"
	objectName := "test-object"
	srcFilePath := "/a/b"

	client := NewMockStorageClient(ctrl)
	fs := filesystem.NewMockFileSystem(ctrl)
	file := filesystem.NewMockFile(ctrl)
	log, _ := logs.GetObservedLogger(zap.ErrorLevel)
	bucketHandle := NewMockBucketHandle(ctrl)
	objectHandle := NewMockObjectHandle(ctrl)
	objectWriter := NewMockStorageWriter(ctrl)
	fileSize := int64(100)
	gcs := &gcs{client, fs, log.Sugar(), int64(5), ""}

	fs.EXPECT().Open(srcFilePath).Return(file, nil)
	client.EXPECT().Bucket(bucketName).Return(bucketHandle)
	bucketHandle.EXPECT().Object(objectName).Return(objectHandle)
	objectHandle.EXPECT().NewWriter(gomock.Any()).Return(objectWriter)
	fs.EXPECT().Copy(objectWriter, file).Return(fileSize, nil)
	objectWriter.EXPECT().Close().Return(errCloseFile)
	file.EXPECT().Close().Return(nil)

	err := gcs.UploadObject(ctx, bucketName, objectName, srcFilePath)
	assert.Equal(t, err, errCloseFile)
}

func Test_gcs_UploadOject_Success(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	bucketName := "test-bucket"
	objectName := "test-object"
	srcFilePath := "/a/b"

	client := NewMockStorageClient(ctrl)
	fs := filesystem.NewMockFileSystem(ctrl)
	file := filesystem.NewMockFile(ctrl)
	log, _ := logs.GetObservedLogger(zap.ErrorLevel)
	bucketHandle := NewMockBucketHandle(ctrl)
	objectHandle := NewMockObjectHandle(ctrl)
	objectWriter := NewMockStorageWriter(ctrl)
	fileSize := int64(100)
	gcs := &gcs{client, fs, log.Sugar(), int64(5), ""}

	fs.EXPECT().Open(srcFilePath).Return(file, nil)
	client.EXPECT().Bucket(bucketName).Return(bucketHandle)
	bucketHandle.EXPECT().Object(objectName).Return(objectHandle)
	objectHandle.EXPECT().NewWriter(gomock.Any()).Return(objectWriter)
	fs.EXPECT().Copy(objectWriter, file).Return(fileSize, nil)
	objectWriter.EXPECT().Close().Return(nil)
	file.EXPECT().Close().Return(nil)

	err := gcs.UploadObject(ctx, bucketName, objectName, srcFilePath)
	assert.Equal(t, err, nil)
}

func Test_gcs_DownloadOject_Success(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	bucketName := "test-bucket"
	objectName := "test-object"
	dstFilePath := "/a/b"

	client := NewMockStorageClient(ctrl)
	fs := filesystem.NewMockFileSystem(ctrl)
	file := filesystem.NewMockFile(ctrl)
	log, _ := logs.GetObservedLogger(zap.ErrorLevel)
	bucketHandle := NewMockBucketHandle(ctrl)
	objectHandle := NewMockObjectHandle(ctrl)
	objectReader := NewMockStorageReader(ctrl)
	fileSize := int64(100)
	gcs := &gcs{client, fs, log.Sugar(), int64(5), ""}

	fs.EXPECT().Create(dstFilePath).Return(file, nil)
	client.EXPECT().Bucket(bucketName).Return(bucketHandle)
	bucketHandle.EXPECT().Object(objectName).Return(objectHandle)
	objectHandle.EXPECT().NewReader(gomock.Any()).Return(objectReader, nil)
	fs.EXPECT().Copy(file, objectReader).Return(fileSize, nil)
	objectReader.EXPECT().Close().Return(nil)
	file.EXPECT().Close().Return(nil)

	err := gcs.DownloadObject(ctx, bucketName, objectName, dstFilePath)
	assert.Equal(t, err, nil)
}

func Test_gcs_DownloadOject_FileCreateError(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	bucketName := "test-bucket"
	objectName := "test-object"
	dstFilePath := "/a/b"

	var fileCreateErr = errors.New("file create error")
	client := NewMockStorageClient(ctrl)
	fs := filesystem.NewMockFileSystem(ctrl)
	log, _ := logs.GetObservedLogger(zap.ErrorLevel)
	gcs := &gcs{client, fs, log.Sugar(), int64(5), ""}

	fs.EXPECT().Create(dstFilePath).Return(nil, fileCreateErr)

	err := gcs.DownloadObject(ctx, bucketName, objectName, dstFilePath)
	assert.Equal(t, err, fileCreateErr)
}

func Test_gcs_DownloadOject_ObjectNotFoundError(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	bucketName := "test-bucket"
	objectName := "test-object"
	dstFilePath := "/a/b"

	client := NewMockStorageClient(ctrl)
	fs := filesystem.NewMockFileSystem(ctrl)
	file := filesystem.NewMockFile(ctrl)
	log, _ := logs.GetObservedLogger(zap.ErrorLevel)
	bucketHandle := NewMockBucketHandle(ctrl)
	objectHandle := NewMockObjectHandle(ctrl)
	gcs := &gcs{client, fs, log.Sugar(), int64(5), ""}

	fs.EXPECT().Create(dstFilePath).Return(file, nil)
	client.EXPECT().Bucket(bucketName).Return(bucketHandle)
	bucketHandle.EXPECT().Object(objectName).Return(objectHandle)
	objectHandle.EXPECT().NewReader(gomock.Any()).Return(nil, storage.ErrObjectNotExist)
	file.EXPECT().Close().Return(nil)

	err := gcs.DownloadObject(ctx, bucketName, objectName, dstFilePath)
	assert.Equal(t, err, storage.ErrObjectNotExist)
}

func Test_gcs_DownloadOject_FileCopyError(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	bucketName := "test-bucket"
	objectName := "test-object"
	dstFilePath := "/a/b"

	fileCopyErr := errors.New("file copy error")

	client := NewMockStorageClient(ctrl)
	fs := filesystem.NewMockFileSystem(ctrl)
	file := filesystem.NewMockFile(ctrl)
	log, _ := logs.GetObservedLogger(zap.ErrorLevel)
	bucketHandle := NewMockBucketHandle(ctrl)
	objectHandle := NewMockObjectHandle(ctrl)
	objectReader := NewMockStorageReader(ctrl)
	fileSize := int64(100)
	gcs := &gcs{client, fs, log.Sugar(), int64(5), ""}

	fs.EXPECT().Create(dstFilePath).Return(file, nil)
	client.EXPECT().Bucket(bucketName).Return(bucketHandle)
	bucketHandle.EXPECT().Object(objectName).Return(objectHandle)
	objectHandle.EXPECT().NewReader(gomock.Any()).Return(objectReader, nil)
	fs.EXPECT().Copy(file, objectReader).Return(fileSize, fileCopyErr)
	objectReader.EXPECT().Close().Return(nil)
	file.EXPECT().Close().Return(nil)

	err := gcs.DownloadObject(ctx, bucketName, objectName, dstFilePath)
	assert.Equal(t, err, fileCopyErr)
}

func Test_gcs_Close(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	client := NewMockStorageClient(ctrl)
	fs := filesystem.NewMockFileSystem(ctrl)
	log, _ := logs.GetObservedLogger(zap.ErrorLevel)

	gcs := &gcs{client, fs, log.Sugar(), int64(5), ""}

	client.EXPECT().Close().Return(nil)
	err := gcs.Close()
	assert.Equal(t, err, nil)
}

func Test_NewGCSClient_DefaultClient(t *testing.T) {
	// assign the original storage client function to a variable.
	originalStorageClient := storageNewClient
	// defer the re-assignment back to the original validate function.
	defer func() {
		storageNewClient = originalStorageClient
	}()

	mockStorageClient := &storage.Client{}
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()
	fs := filesystem.NewMockFileSystem(ctrl)

	storageNewClient = func(context.Context, ...option.ClientOption) (*storage.Client, error) {
		return mockStorageClient, nil
	}
	_ = storageNewClient
	_, err := NewGCSClient(ctx, fs, nil)
	assert.Equal(t, err, nil)
}

func Test_NewGCSClient_WithOpts(t *testing.T) {
	// assign the original storage client function to a variable.
	originalStorageClient := storageNewClient
	// defer the re-assignment back to the original validate function.
	defer func() {
		storageNewClient = originalStorageClient
	}()

	timeoutSecs := int64(10)
	credFile := "/a/b/c"
	mockStorageClient := &storage.Client{}
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()
	fs := filesystem.NewMockFileSystem(ctrl)

	storageNewClient = func(context.Context, ...option.ClientOption) (*storage.Client, error) {
		return mockStorageClient, nil
	}
	_ = storageNewClient
	_, err := NewGCSClient(ctx, fs, nil, WithGCSClientTimeout(timeoutSecs), WithGCSCredentialsFile(credFile))
	assert.Equal(t, err, nil)
}

func Test_NewGCSClient_Error(t *testing.T) {
	// assign the original storage client function to a variable.
	originalStorageClient := storageNewClient
	// defer the re-assignment back to the original validate function.
	defer func() {
		storageNewClient = originalStorageClient
	}()

	var clientCreateErr error = errors.New("failed to create client")
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()
	fs := filesystem.NewMockFileSystem(ctrl)

	storageNewClient = func(context.Context, ...option.ClientOption) (*storage.Client, error) {
		return nil, clientCreateErr
	}
	_ = storageNewClient
	_, err := NewGCSClient(ctx, fs, nil)
	assert.Equal(t, err, clientCreateErr)
}
