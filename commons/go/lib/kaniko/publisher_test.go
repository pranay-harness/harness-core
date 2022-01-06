// Copyright 2020 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package kaniko

import (
	"context"
	"errors"
	"fmt"
	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/filesystem"
	"github.com/wings-software/portal/commons/go/lib/logs"
	"go.uber.org/zap"
	"os"
	"os/exec"
	"strconv"
	"testing"
)

var mockedExitStatus = 0
var mockedStdout string
var mockedStderr string

func TestExecCommandHelper(t *testing.T) {
	if os.Getenv("GO_WANT_HELPER_PROCESS") != "1" {
		return
	}

	fmt.Fprintf(os.Stdout, os.Getenv("STDOUT"))
	fmt.Fprintf(os.Stderr, os.Getenv("STDERR"))
	i, _ := strconv.Atoi(os.Getenv("EXIT_STATUS"))
	os.Exit(i)
}

func fakeExecCommandWithContext(ctx context.Context, command string, args ...string) *exec.Cmd {
	cs := []string{"-test.run=TestExecCommandHelper", "--", command}
	cs = append(cs, args...)
	cmd := exec.Command(os.Args[0], cs...)
	es := strconv.Itoa(mockedExitStatus)
	cmd.Env = []string{"GO_WANT_HELPER_PROCESS=1",
		"STDOUT=" + mockedStdout,
		"STDERR=" + mockedStderr,
		"EXIT_STATUS=" + es}
	return cmd
}

func TestNewRegistryClientWithoutRegistry(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	_, err := NewRegistryClient(nil, nil)
	assert.NotEqual(t, err, nil)
}

func TestNewRegistryClientWithDockerhub(t *testing.T) {

	_, err := NewRegistryClient(nil, nil, WithDockerHubClient("user", "admin", ""))
	assert.Equal(t, err, nil)
}

func TestNewRegistryClientWithECR(t *testing.T) {

	_, err := NewRegistryClient(nil, nil, WithEcrClient("access-key", "secret-key"))
	assert.Equal(t, err, nil)
}

func TestNewRegistryClientWithGCR(t *testing.T) {
	_, err := NewRegistryClient(nil, nil, WithGcrClient("secret-path"), WithRegistryClientTimeoutSecs(10))
	assert.Equal(t, err, nil)
}

func TestRegistryClient_DockerHub_Publish_SetupSuccess(t *testing.T) {
	originalExecCommandCtx := execCommandWithContext
	defer func() { execCommandWithContext = originalExecCommandCtx }()
	execCommandWithContext = fakeExecCommandWithContext

	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	fs := filesystem.NewMockFileSystem(ctrl)
	log, _ := logs.GetObservedLogger(zap.ErrorLevel)
	fs.EXPECT().WriteFile(dockerConfigFilePath, gomock.Any()).Return(nil)

	fs.EXPECT().Remove(dockerConfigFilePath).Return(nil)

	r, _ := NewRegistryClient(log.Sugar(), fs, WithDockerHubClient("user", "admin", ""))
	err := r.Publish("file_path", "context", "dest")

	assert.Equal(t, err, nil)
}

func TestRegistryClient_DockerHub_Publish_SetupFailure(t *testing.T) {
	originalExecCommandCtx := execCommandWithContext
	defer func() { execCommandWithContext = originalExecCommandCtx }()
	execCommandWithContext = fakeExecCommandWithContext

	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	fs := filesystem.NewMockFileSystem(ctrl)
	log, _ := logs.GetObservedLogger(zap.ErrorLevel)
	fs.EXPECT().WriteFile(dockerConfigFilePath, gomock.Any()).Return(errors.New(fmt.Sprintf("Error while writing to file")))

	fs.EXPECT().Remove(dockerConfigFilePath).Return(nil)

	r, _ := NewRegistryClient(log.Sugar(), fs, WithDockerHubClient("user", "admin", ""))
	err := r.Publish("file_path", "context", "dest")

	assert.NotEqual(t, err, nil)
}

func TestRegistryClient_DockerHub_Publish_CleanupFailure(t *testing.T) {
	originalExecCommandCtx := execCommandWithContext
	defer func() { execCommandWithContext = originalExecCommandCtx }()
	execCommandWithContext = fakeExecCommandWithContext

	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	fs := filesystem.NewMockFileSystem(ctrl)
	log, _ := logs.GetObservedLogger(zap.ErrorLevel)
	fs.EXPECT().WriteFile(dockerConfigFilePath, gomock.Any()).Return(nil)

	fs.EXPECT().Remove(dockerConfigFilePath).Return(errors.New(
		fmt.Sprintf("Error while removing config file")))

	r, _ := NewRegistryClient(log.Sugar(), fs, WithDockerHubClient("user", "admin", ""))
	err := r.Publish("file_path", "context", "dest")

	assert.Equal(t, err, nil)
}

func TestRegistryClient_ECR_Publish_SetupSuccess(t *testing.T) {
	originalExecCommandCtx := execCommandWithContext
	defer func() { execCommandWithContext = originalExecCommandCtx }()
	accessKey := "access-key"
	secretKey := "secret-key"
	execCommandWithContext = fakeExecCommandWithContext

	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	fs := filesystem.NewMockFileSystem(ctrl)
	log, _ := logs.GetObservedLogger(zap.ErrorLevel)

	// Set environment variables and docker file
	fs.EXPECT().Setenv(ecrAccessKeyEnv, accessKey).Return(nil)
	fs.EXPECT().Setenv(ecrSecretKeyEnv, secretKey).Return(nil)
	fs.EXPECT().WriteFile(dockerConfigFilePath, gomock.Any()).Return(nil)

	// Unset environment variables and remove docker file
	fs.EXPECT().Unsetenv(ecrAccessKeyEnv)
	fs.EXPECT().Unsetenv(ecrSecretKeyEnv)
	fs.EXPECT().Remove(dockerConfigFilePath).Return(nil)

	r, _ := NewRegistryClient(log.Sugar(), fs, WithEcrClient(accessKey, secretKey))
	err := r.Publish("file_path", "context", "repo/dest-v0.1")

	assert.Equal(t, err, nil)
}

func TestRegistryClient_ECR_Publish_SetupFailure_Env1(t *testing.T) {
	originalExecCommandCtx := execCommandWithContext
	defer func() { execCommandWithContext = originalExecCommandCtx }()
	accessKey := "access-key"
	secretKey := "secret-key"
	execCommandWithContext = fakeExecCommandWithContext

	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	fs := filesystem.NewMockFileSystem(ctrl)
	log, _ := logs.GetObservedLogger(zap.ErrorLevel)

	// Set environment variables and docker file
	fs.EXPECT().Setenv(ecrAccessKeyEnv, accessKey).Return(nil)
	fs.EXPECT().Setenv(ecrSecretKeyEnv, secretKey).Return(errors.New(fmt.Sprintf("Error while trying to set secret key")))

	// Unset environment variables and remove docker file
	fs.EXPECT().Unsetenv(ecrSecretKeyEnv).Return(errors.New(fmt.Sprintf("Error while trying to unset secret key")))

	r, _ := NewRegistryClient(log.Sugar(), fs, WithEcrClient(accessKey, secretKey))
	err := r.Publish("file_path", "context", "repo/dest-v0.1")

	assert.NotEqual(t, err, nil)
}

func TestRegistryClient_ECR_Publish_SetupFailure_Env2(t *testing.T) {
	originalExecCommandCtx := execCommandWithContext
	defer func() { execCommandWithContext = originalExecCommandCtx }()
	accessKey := "access-key"
	secretKey := "secret-key"
	execCommandWithContext = fakeExecCommandWithContext

	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	fs := filesystem.NewMockFileSystem(ctrl)
	log, _ := logs.GetObservedLogger(zap.ErrorLevel)

	// Set environment variables and docker file
	fs.EXPECT().Setenv(ecrAccessKeyEnv, accessKey).Return(errors.New(
		fmt.Sprintf("Error while trying to set access key")))

	// Unset environment variables and remove docker file
	fs.EXPECT().Unsetenv(ecrSecretKeyEnv)
	fs.EXPECT().Unsetenv(ecrAccessKeyEnv).Return(errors.New(fmt.Sprintf("Could not unset env variable")))

	r, _ := NewRegistryClient(log.Sugar(), fs, WithEcrClient(accessKey, secretKey))
	err := r.Publish("file_path", "context", "repo/dest-v0.1")

	assert.NotEqual(t, err, nil)
}

func TestRegistryClient_ECR_Publish_SetupFailure_File(t *testing.T) {
	originalExecCommandCtx := execCommandWithContext
	defer func() { execCommandWithContext = originalExecCommandCtx }()
	accessKey := "access-key"
	secretKey := "secret-key"
	execCommandWithContext = fakeExecCommandWithContext

	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	fs := filesystem.NewMockFileSystem(ctrl)
	log, _ := logs.GetObservedLogger(zap.ErrorLevel)

	// Set environment variables and docker file
	fs.EXPECT().Setenv(ecrAccessKeyEnv, accessKey).Return(nil)
	fs.EXPECT().Setenv(ecrSecretKeyEnv, secretKey).Return(nil)
	fs.EXPECT().WriteFile(dockerConfigFilePath, gomock.Any()).Return(errors.New(
		fmt.Sprintf("Could not remove docker config file")))

	// Unset environment variables and remove docker file
	fs.EXPECT().Unsetenv(ecrAccessKeyEnv)
	fs.EXPECT().Unsetenv(ecrSecretKeyEnv)
	fs.EXPECT().Remove(dockerConfigFilePath).Return(errors.New(
		fmt.Sprintf("Could not remove docker config file")))

	r, _ := NewRegistryClient(log.Sugar(), fs, WithEcrClient(accessKey, secretKey))
	err := r.Publish("file_path", "context", "repo/dest-v0.1")

	assert.NotEqual(t, err, nil)
}

func TestRegistryClient_ECR_Publish_SetupFailure_Destination(t *testing.T) {
	originalExecCommandCtx := execCommandWithContext
	defer func() { execCommandWithContext = originalExecCommandCtx }()
	accessKey := "access-key"
	secretKey := "secret-key"
	execCommandWithContext = fakeExecCommandWithContext

	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	fs := filesystem.NewMockFileSystem(ctrl)
	log, _ := logs.GetObservedLogger(zap.ErrorLevel)

	// Set environment variables and docker file
	fs.EXPECT().Setenv(ecrAccessKeyEnv, accessKey).Return(nil)
	fs.EXPECT().Setenv(ecrSecretKeyEnv, secretKey).Return(nil)

	// Unset environment variables and remove docker file
	fs.EXPECT().Unsetenv(ecrAccessKeyEnv)
	fs.EXPECT().Unsetenv(ecrSecretKeyEnv)
	fs.EXPECT().Remove(dockerConfigFilePath).Return(errors.New(
		fmt.Sprintf("Could not remove docker config file")))

	r, _ := NewRegistryClient(log.Sugar(), fs, WithEcrClient(accessKey, secretKey))
	err := r.Publish("file_path", "context", "dest-v0.1")

	fmt.Println(err)
	assert.NotEqual(t, err, nil)
}

func TestRegistryClient_GCR_Publish_SetupSuccess(t *testing.T) {
	originalExecCommandCtx := execCommandWithContext
	defer func() { execCommandWithContext = originalExecCommandCtx }()
	secretPath := "secret-path"
	execCommandWithContext = fakeExecCommandWithContext

	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	fs := filesystem.NewMockFileSystem(ctrl)
	log, _ := logs.GetObservedLogger(zap.ErrorLevel)

	// Set environment variable
	fs.EXPECT().Setenv(gcrEnvVariable, secretPath).Return(nil)

	// Unset environment variables and remove docker file
	fs.EXPECT().Unsetenv(gcrEnvVariable)
	fs.EXPECT().Remove(dockerConfigFilePath).Return(errors.New(
		fmt.Sprintf("Could not clean up config file")))

	r, _ := NewRegistryClient(log.Sugar(), fs, WithGcrClient(secretPath))
	err := r.Publish("file_path", "context", "repo/dest-v0.1")

	assert.Equal(t, err, nil)
}

func TestRegistryClient_GCR_Publish_SetupFailure(t *testing.T) {
	originalExecCommandCtx := execCommandWithContext
	defer func() { execCommandWithContext = originalExecCommandCtx }()
	secretPath := "secret-path"
	execCommandWithContext = fakeExecCommandWithContext

	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	fs := filesystem.NewMockFileSystem(ctrl)
	log, _ := logs.GetObservedLogger(zap.ErrorLevel)

	// Set environment variable
	fs.EXPECT().Setenv(gcrEnvVariable, secretPath).Return(errors.New(
		fmt.Sprintf("Error while trying to set gcr environment variable")))

	// Unset environment variables and remove docker file
	fs.EXPECT().Unsetenv(gcrEnvVariable)
	fs.EXPECT().Remove(dockerConfigFilePath).Return(nil)

	r, _ := NewRegistryClient(log.Sugar(), fs, WithGcrClient(secretPath))
	err := r.Publish("file_path", "context", "repo/dest-v0.1")

	assert.NotEqual(t, err, nil)
}

func TestRegistryClient_GCR_Publish_CleanupFailure(t *testing.T) {
	originalExecCommandCtx := execCommandWithContext
	defer func() { execCommandWithContext = originalExecCommandCtx }()
	secretPath := "secret-path"
	execCommandWithContext = fakeExecCommandWithContext

	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	fs := filesystem.NewMockFileSystem(ctrl)
	log, _ := logs.GetObservedLogger(zap.ErrorLevel)

	// Set environment variable
	fs.EXPECT().Setenv(gcrEnvVariable, secretPath).Return(nil)

	// Unset environment variables and remove docker file
	fs.EXPECT().Unsetenv(gcrEnvVariable).Return(errors.New(
		fmt.Sprintf("Could not cleanup environment variable")))

	r, _ := NewRegistryClient(log.Sugar(), fs, WithGcrClient(secretPath))
	err := r.Publish("file_path", "context", "repo/dest-v0.1")

	assert.Equal(t, err, nil)
}

func TestRegistryClient_Exec_Error(t *testing.T) {
	originalExecCommandCtx := execCommandWithContext
	defer func() { execCommandWithContext = originalExecCommandCtx }()
	execCommandWithContext = fakeExecCommandWithContext

	// Inject error for sub process run
	mockedExitStatus = 2
	mockedStdout = ""
	mockedStderr = "err"

	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	fs := filesystem.NewMockFileSystem(ctrl)
	log, _ := logs.GetObservedLogger(zap.ErrorLevel)
	fs.EXPECT().WriteFile(dockerConfigFilePath, gomock.Any()).Return(nil)

	fs.EXPECT().Remove(dockerConfigFilePath).Return(nil)

	r, _ := NewRegistryClient(log.Sugar(), fs, WithDockerHubClient("user", "admin", ""))
	err := r.Publish("file_path", "context", "dest")

	assert.NotEqual(t, err, nil)
}
