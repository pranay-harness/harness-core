// Copyright 2020 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package jfrogutils

import (
	"context"
	"encoding/json"
	"fmt"
	"os"
	"os/exec"
	"strconv"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/logs"
	"go.uber.org/zap"
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

func TestNewArtifactoryClientWithNoAuth(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()
	_, err := NewArtifactoryClient("jfrog", nil)
	assert.NotEqual(t, err, nil)
}

func TestNewArtifactoryClientWithBasicAuth(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	usrName := "admin"
	password := "password"
	_, err := NewArtifactoryClient("jfrog", nil, WithArtifactoryClientBasicAuth(usrName, password), WithArtifactoryClientRetries(3))
	assert.Equal(t, err, nil)
}

func TestNewArtifactoryClientWithAccessToken(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	accessToken := "token"
	_, err := NewArtifactoryClient("jfrog", nil, WithArtifactoryClientAccessTokenAuth(accessToken), WithArtifactoryClientTimeout(1))
	assert.Equal(t, err, nil)
}

func TestNewArtifactoryClientWithAPIKey(t *testing.T) {
	ctrl, _ := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	apiKey := "apiKey"
	_, err := NewArtifactoryClient("jfrog", nil, WithArtifactoryClientAPIKeyAuth(apiKey))
	assert.Equal(t, err, nil)
}

func Test_artifactory_Upload_BasicAuthRunErr(t *testing.T) {
	originalExecCommandCtx := execCommandWithContext
	defer func() { execCommandWithContext = originalExecCommandCtx }()
	execCommandWithContext = fakeExecCommandWithContext
	mockedExitStatus = 1
	mockedStdout = ""
	mockedStderr = "Invalid number of arguments"

	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	usrName := "admin"
	password := "password"
	srcFilePattern := "/tmp/mock.jar"
	targetRepoPath := "tmp"
	artifactoryURL := "mock-url"
	log, _ := logs.GetObservedLogger(zap.ErrorLevel)
	rt := &artifactory{"jfrog", usrName, password, "", "", defaultNumRetries, log.Sugar(), defaultTimeoutSecs}

	err := rt.Upload(ctx, srcFilePattern, targetRepoPath, artifactoryURL)
	assert.Equal(t, err, err.(*exec.ExitError))
}

func Test_artifactory_Upload_BasicAuthJsonParseErr(t *testing.T) {
	originalExecCommandCtx := execCommandWithContext
	defer func() { execCommandWithContext = originalExecCommandCtx }()
	execCommandWithContext = fakeExecCommandWithContext
	mockedExitStatus = 0
	mockedStdout = ""
	mockedStderr = ""

	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	usrName := "admin"
	password := "password"
	srcFilePattern := "/tmp/mock.jar"
	targetRepoPath := "tmp"
	artifactoryURL := "mock-url"
	log, _ := logs.GetObservedLogger(zap.ErrorLevel)
	rt := &artifactory{"jfrog", usrName, password, "", "", defaultNumRetries, log.Sugar(), defaultTimeoutSecs}

	err := rt.Upload(ctx, srcFilePattern, targetRepoPath, artifactoryURL)
	assert.Equal(t, err, err.(*json.SyntaxError))
}

func Test_artifactory_Upload_BasicAuthUploadFailErr(t *testing.T) {
	originalExecCommandCtx := execCommandWithContext
	defer func() { execCommandWithContext = originalExecCommandCtx }()
	execCommandWithContext = fakeExecCommandWithContext
	mockedExitStatus = 0
	mockedStdout = "{\"status\": \"failure\", \"totals\": {\"success\": 0, \"failure\": 1}}"
	mockedStderr = ""

	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	usrName := "admin"
	password := "password"
	srcFilePattern := "/tmp/mock.jar"
	targetRepoPath := "tmp"
	artifactoryURL := "mock-url"
	log, _ := logs.GetObservedLogger(zap.ErrorLevel)
	rt := &artifactory{"jfrog", usrName, password, "", "", defaultNumRetries, log.Sugar(), defaultTimeoutSecs}

	err := rt.Upload(ctx, srcFilePattern, targetRepoPath, artifactoryURL)
	assert.Equal(t, err.Error(), "failed to upload files to jfrog")
}

func Test_artifactory_Upload_BasicAuthUploadSuccess(t *testing.T) {
	originalExecCommandCtx := execCommandWithContext
	defer func() { execCommandWithContext = originalExecCommandCtx }()
	execCommandWithContext = fakeExecCommandWithContext
	mockedExitStatus = 0
	mockedStdout = "{\"status\": \"success\", \"totals\": {\"success\": 1, \"failure\": 0}}"
	mockedStderr = ""

	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	usrName := "admin"
	password := "password"
	srcFilePattern := "/tmp/mock.jar"
	targetRepoPath := "tmp"
	artifactoryURL := "mock-url"
	log, _ := logs.GetObservedLogger(zap.ErrorLevel)
	rt := &artifactory{"jfrog", usrName, password, "", "", defaultNumRetries, log.Sugar(), defaultTimeoutSecs}

	err := rt.Upload(ctx, srcFilePattern, targetRepoPath, artifactoryURL)
	assert.Equal(t, err, nil)
}

func Test_artifactory_Upload_ApiKeyUploadSuccess(t *testing.T) {
	originalExecCommandCtx := execCommandWithContext
	defer func() { execCommandWithContext = originalExecCommandCtx }()
	execCommandWithContext = fakeExecCommandWithContext
	mockedExitStatus = 0
	mockedStdout = "{\"status\": \"success\", \"totals\": {\"success\": 1, \"failure\": 0}}"
	mockedStderr = ""

	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	apiKey := "test-key"
	srcFilePattern := "/tmp/mock.jar"
	targetRepoPath := "tmp"
	artifactoryURL := "mock-url"
	log, _ := logs.GetObservedLogger(zap.ErrorLevel)
	rt := &artifactory{"jfrog", "", "", apiKey, "", defaultNumRetries, log.Sugar(), defaultTimeoutSecs}

	err := rt.Upload(ctx, srcFilePattern, targetRepoPath, artifactoryURL)
	assert.Equal(t, err, nil)
}

func Test_artifactory_Upload_AccessToeknUploadSuccess(t *testing.T) {
	originalExecCommandCtx := execCommandWithContext
	defer func() { execCommandWithContext = originalExecCommandCtx }()
	execCommandWithContext = fakeExecCommandWithContext
	mockedExitStatus = 0
	mockedStdout = "{\"status\": \"success\", \"totals\": {\"success\": 1, \"failure\": 0}}"
	mockedStderr = ""

	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	accessToken := "test-token"
	srcFilePattern := "/tmp/mock.jar"
	targetRepoPath := "tmp"
	artifactoryURL := "mock-url"
	log, _ := logs.GetObservedLogger(zap.ErrorLevel)
	rt := &artifactory{"jfrog", "", "", "", accessToken, defaultNumRetries, log.Sugar(), defaultTimeoutSecs}

	err := rt.Upload(ctx, srcFilePattern, targetRepoPath, artifactoryURL)
	assert.Equal(t, err, nil)
}
