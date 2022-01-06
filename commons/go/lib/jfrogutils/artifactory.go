// Copyright 2020 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package jfrogutils

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"os/exec"
	"strings"
	"time"

	"github.com/wings-software/portal/commons/go/lib/utils"
	"go.uber.org/zap"
)

//go:generate mockgen -source artifactory.go -package=jfrogutils -destination mocks/artifactory_mock.go Artifactory

const (
	defaultNumRetries   int64  = 3   // default number of retries
	defaultTimeoutSecs  int64  = 300 // 5 minutes
	failureUploadStatus string = "failure"
	successUploadStatus string = "success"
)

var execCommandWithContext = exec.CommandContext

// Artifactory represents an interface to upload files to Jfrog artifactory.
type Artifactory interface {
	Upload(ctx context.Context, srcFilePattern, targetRepositoryPath, artifactoryURL string) error
}

type artifactory struct {
	jfrogPath   string // jfrog cli path
	userName    string
	password    string
	apiKey      string
	accessToken string
	numRetries  int64
	log         *zap.SugaredLogger
	timeoutSecs int64
}

type uploadStatus struct {
	Status string
	Totals fileUploadStatus
}

type fileUploadStatus struct {
	Success int64
	Failure int64
}

// ArtifactoryClientOption is a type for providing arguments to NewArtifactoryClient in variadic format
type ArtifactoryClientOption func(*artifactory)

// WithArtifactoryClientRetries sets the number of retries for any operation performed using the Artifactory client.
// Number of retries provided should be a positive value.
func WithArtifactoryClientRetries(numRetries int64) ArtifactoryClientOption {
	return func(a *artifactory) {
		a.numRetries = numRetries
	}
}

// WithArtifactoryClientTimeout sets the timeout of any operation performed using the Artifactory client.
// Timeout provide should be a positive value.
func WithArtifactoryClientTimeout(timeoutSecs int64) ArtifactoryClientOption {
	return func(a *artifactory) {
		a.timeoutSecs = timeoutSecs
	}
}

// WithArtifactoryClientBasicAuth sets the username and password for interacting with jfrog repository.
func WithArtifactoryClientBasicAuth(userName, password string) ArtifactoryClientOption {
	return func(a *artifactory) {
		a.userName = userName
		a.password = password
	}
}

// WithArtifactoryClientAPIKeyAuth sets the api key for interacting with jfrog repository.
func WithArtifactoryClientAPIKeyAuth(apiKey string) ArtifactoryClientOption {
	return func(a *artifactory) {
		a.apiKey = apiKey
	}
}

// WithArtifactoryClientAccessTokenAuth sets the access token for interacting with jfrog repository.
func WithArtifactoryClientAccessTokenAuth(accessToken string) ArtifactoryClientOption {
	return func(a *artifactory) {
		a.accessToken = accessToken
	}
}

// NewArtifactoryClient creates a new Jfrog artifactory client and returns it.
func NewArtifactoryClient(jfrogPath string, log *zap.SugaredLogger, opts ...ArtifactoryClientOption) (Artifactory, error) {
	a := &artifactory{
		jfrogPath:   jfrogPath,
		log:         log,
		numRetries:  defaultNumRetries,
		timeoutSecs: defaultTimeoutSecs,
	}
	for _, opt := range opts {
		opt(a)
	}

	// Ensure that one of basic auth, api key or access token is provided.
	if a.userName == "" && a.apiKey == "" && a.accessToken == "" {
		return nil, fmt.Errorf("Provide one of basic auth, api key or access token for jfrog client creation")
	}

	return a, nil
}

// Uploads files to Jfrog artifactory using jfrog-cli
// Requires jfrog-cli to be installed on the system
func (a *artifactory) Upload(ctx context.Context, srcFilePattern, targetRepositoryPath, artifactoryURL string) error {
	// upload command format: jfrog rt u --url=<> --retries=<> --user=<> --password=<> srcFilePattern dstFilePattern
	start := time.Now()
	ctx, cancel := context.WithTimeout(ctx, time.Second*time.Duration(a.timeoutSecs))
	defer cancel()

	args := []string{"rt", "u", fmt.Sprintf("--url=%s", artifactoryURL), fmt.Sprintf("--retries=%d", a.numRetries)}
	printableArgs := strings.Join(args[:], " ")
	if a.userName != "" {
		args = append(args, fmt.Sprintf("--user=%s", a.userName))
		args = append(args, fmt.Sprintf("--password=%s", a.password))
	} else if a.apiKey != "" {
		args = append(args, fmt.Sprintf("--apikey=%s", a.apiKey))
	} else {
		args = append(args, fmt.Sprintf("--access-token=%s", a.accessToken))
	}
	args = append(args, srcFilePattern)
	args = append(args, targetRepositoryPath)
	printableArgs = fmt.Sprintf("%s %s <auth> %s %s", a.jfrogPath, printableArgs, srcFilePattern, targetRepositoryPath)

	var out bytes.Buffer
	var stderr bytes.Buffer
	cmd := execCommandWithContext(ctx, a.jfrogPath, args...)
	cmd.Stdout = &out
	cmd.Stderr = &stderr
	err := cmd.Run()
	if ctxErr := ctx.Err(); ctxErr == context.DeadlineExceeded {
		logCommandExecWarning(a.log, "time out while executing jfrog cli for upload", stderr.String(), printableArgs, start, ctxErr)
		return ctxErr
	}

	if err != nil {
		logCommandExecWarning(a.log, "error encountered while executing jfrog cli for upload", stderr.String(), printableArgs, start, err)
		return err
	}

	// Parse JSON output
	uploadStatus := &uploadStatus{}
	err = json.Unmarshal(out.Bytes(), uploadStatus)
	if err != nil {
		logJSONParseWarning(a.log, "error encountered while parsing jfrog upload status", out.String(), printableArgs, start, err)
		return err
	}

	if uploadStatus.Status == failureUploadStatus {
		err := fmt.Errorf("failed to upload files to jfrog")
		warnMsg := "failed to upload all the files to jfrog"
		logUploadFailureWarning(a.log, warnMsg, printableArgs, uploadStatus.Totals.Failure, uploadStatus.Totals.Success, start, err)
		return err
	}

	a.log.Infow(
		"Successfully uploaded files to jfrog",
		"failed_uploads", uploadStatus.Totals.Failure,
		"successful_uploads", uploadStatus.Totals.Success,
		"arguments", printableArgs,
		"elapsed_time_ms", utils.TimeSince(start),
	)
	return nil
}

func logCommandExecWarning(log *zap.SugaredLogger, warnMsg, stderr, args string, startTime time.Time, err error) {
	log.Warnw(
		warnMsg,
		"standard_error", stderr,
		"arguments", args,
		"elapsed_time_ms", utils.TimeSince(startTime),
		zap.Error(err),
	)
}

func logJSONParseWarning(log *zap.SugaredLogger, warnMsg, uploadStatus, args string, startTime time.Time, err error) {
	log.Warnw(
		warnMsg,
		"upload_status", uploadStatus,
		"arguments", args,
		"elapsed_time_ms", utils.TimeSince(startTime),
		zap.Error(err),
	)
}

func logUploadFailureWarning(log *zap.SugaredLogger, warnMsg, args string, failedUploads, successfulUploads int64, startTime time.Time, err error) {
	log.Warnw(
		warnMsg,
		"failed_uploads", failedUploads,
		"successful_uploads", successfulUploads,
		"arguments", args,
		"elapsed_time_ms", utils.TimeSince(startTime),
		zap.Error(err),
	)
}
