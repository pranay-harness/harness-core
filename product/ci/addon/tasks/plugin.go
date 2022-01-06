// Copyright 2021 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package tasks

import (
	"context"
	"fmt"
	"io"
	"os"
	"time"

	"github.com/wings-software/portal/commons/go/lib/exec"
	"github.com/wings-software/portal/commons/go/lib/images"
	"github.com/wings-software/portal/commons/go/lib/utils"
	"github.com/wings-software/portal/product/ci/addon/artifact"
	"github.com/wings-software/portal/product/ci/addon/remote"
	"github.com/wings-software/portal/product/ci/addon/resolver"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

//go:generate mockgen -source plugin.go -package=tasks -destination mocks/plugin_mock.go PluginTask

const (
	defaultPluginTimeout    int64         = 14400 // 4 hour
	defaultPluginNumRetries int32         = 1
	pluginCmdExitWaitTime   time.Duration = time.Duration(0)
	imageSecretEnv                        = "HARNESS_IMAGE_SECRET" // Docker image secret for plugin image
	settingEnvPrefix                      = "PLUGIN_"
	pluginArtifactFileEnv                 = "PLUGIN_ARTIFACT_FILE"
)

var (
	getImgMetadata = remote.GetImageEntrypoint
	evaluateJEXL   = remote.EvaluateJEXL
)

// PluginTask represents interface to execute a plugin step
type PluginTask interface {
	Run(ctx context.Context) (*pb.Artifact, int32, error)
}

type pluginTask struct {
	id                string
	displayName       string
	timeoutSecs       int64
	numRetries        int32
	image             string
	entrypoint        []string
	environment       map[string]string
	prevStepOutputs   map[string]*pb.StepOutput
	logMetrics        bool
	log               *zap.SugaredLogger
	addonLogger       *zap.SugaredLogger
	procWriter        io.Writer
	cmdContextFactory exec.CmdContextFactory
	artifactFilePath  string
	reports           []*pb.Report
}

// NewPluginTask creates a plugin step executor
func NewPluginTask(step *pb.UnitStep, prevStepOutputs map[string]*pb.StepOutput,
	log *zap.SugaredLogger, w io.Writer, logMetrics bool, addonLogger *zap.SugaredLogger) PluginTask {
	r := step.GetPlugin()
	timeoutSecs := r.GetContext().GetExecutionTimeoutSecs()
	if timeoutSecs == 0 {
		timeoutSecs = defaultPluginTimeout
	}

	numRetries := r.GetContext().GetNumRetries()
	if numRetries == 0 {
		numRetries = defaultPluginNumRetries
	}
	return &pluginTask{
		id:                step.GetId(),
		displayName:       step.GetDisplayName(),
		image:             r.GetImage(),
		entrypoint:        r.GetEntrypoint(),
		environment:       r.GetEnvironment(),
		reports:           r.GetReports(),
		timeoutSecs:       timeoutSecs,
		numRetries:        numRetries,
		prevStepOutputs:   prevStepOutputs,
		cmdContextFactory: exec.OsCommandContextGracefulWithLog(log),
		logMetrics:        logMetrics,
		log:               log,
		procWriter:        w,
		addonLogger:       addonLogger,
		artifactFilePath:  r.GetArtifactFilePath(),
	}
}

// Executes customer provided plugin with retries and timeout handling
func (t *pluginTask) Run(ctx context.Context) (*pb.Artifact, int32, error) {
	var err error
	var o *pb.Artifact
	for i := int32(1); i <= t.numRetries; i++ {
		if o, err = t.execute(ctx, i); err == nil {
			st := time.Now()
			err = collectTestReports(ctx, t.reports, t.id, t.log)
			if err != nil {
				// If there's an error in collecting reports, we won't retry but
				// the step will be marked as an error
				t.log.Errorw("unable to collect test reports", zap.Error(err))
				return nil, t.numRetries, err
			}
			if len(t.reports) > 0 {
				t.log.Infow(fmt.Sprintf("collected test reports in %s time", time.Since(st)))
			}
			return o, i, nil
		}
	}
	if err != nil {
		// Run step did not execute successfully
		// Try and collect reports, ignore any errors during report collection itself
		errc := collectTestReports(ctx, t.reports, t.id, t.log)
		if errc != nil {
			t.log.Errorw("error while collecting test reports", zap.Error(errc))
		}
		return nil, t.numRetries, err
	}
	return nil, t.numRetries, err
}

// resolveExprInEnv resolves JEXL expressions & env var present in plugin settings environment variables
func (t *pluginTask) resolveExprInEnv(ctx context.Context) (map[string]string, error) {
	envVarMap := getEnvVars()
	for k, v := range t.environment {
		envVarMap[k] = v
	}

	// Resolves secret in environment variables e.g. foo-${ngSecretManager.obtain("secret", 1234)}
	resolvedSecretMap, err := resolver.ResolveSecretInMapValues(envVarMap)
	if err != nil {
		return nil, err
	}

	return resolvedSecretMap, nil
}

func (t *pluginTask) execute(ctx context.Context, retryCount int32) (*pb.Artifact, error) {
	start := time.Now()
	ctx, cancel := context.WithTimeout(ctx, time.Second*time.Duration(t.timeoutSecs))
	defer cancel()

	commands, err := t.getEntrypoint(ctx)
	if err != nil {
		logPluginErr(t.log, "failed to find entrypoint for plugin", t.id, commands, retryCount, start, err)
		return nil, err
	}

	if len(commands) == 0 {
		err := fmt.Errorf("plugin entrypoint is empty")
		logPluginErr(t.log, "entrypoint fetched from remote for plugin is empty", t.id, commands, retryCount, start, err)
		return nil, err
	}

	envVarsMap, err := t.resolveExprInEnv(ctx)
	if err != nil {
		logPluginErr(t.log, "failed to evaluate JEXL expression for settings", t.id, commands, retryCount, start, err)
		return nil, err
	}

	cmd := t.cmdContextFactory.CmdContextWithSleep(ctx, pluginCmdExitWaitTime, commands[0], commands[1:]...).
		WithStdout(t.procWriter).WithStderr(t.procWriter).WithEnvVarsMap(envVarsMap)
	err = runCmd(ctx, cmd, t.id, commands, retryCount, start, t.logMetrics, t.addonLogger)
	if err != nil {
		return nil, err
	}

	artifactFilePath := t.artifactFilePath
	if artifactFilePath == "" {
		artifactFilePath = envVarsMap[pluginArtifactFileEnv]
	}

	artifactProto, artifactErr := artifact.GetArtifactProtoFromFile(artifactFilePath)
	if artifactErr != nil {
		logPluginErr(t.addonLogger, "failed to retrieve artifacts from the plugin step", t.id, commands, retryCount, start, artifactErr)
	}

	t.addonLogger.Infow(
		"Successfully executed plugin",
		"arguments", commands,
		"output", artifactProto,
		"elapsed_time_ms", utils.TimeSince(start),
	)
	return artifactProto, err
}

func (t *pluginTask) getEntrypoint(ctx context.Context) ([]string, error) {
	if len(t.entrypoint) != 0 {
		return t.entrypoint, nil
	}

	imageSecret, _ := os.LookupEnv(imageSecretEnv)
	return t.combinedEntrypoint(getImgMetadata(ctx, t.id, t.image, imageSecret, t.log))
}

func (t *pluginTask) combinedEntrypoint(ep, cmds []string, err error) ([]string, error) {
	if err != nil {
		return nil, err
	}
	return images.CombinedEntrypoint(ep, cmds), nil
}

func (t *pluginTask) readPluginOutput() (map[string]string, error) {
	return make(map[string]string), nil
}

func logPluginErr(log *zap.SugaredLogger, errMsg, stepID string, cmds []string, retryCount int32, startTime time.Time, err error) {
	log.Errorw(
		errMsg,
		"retry_count", retryCount,
		"commands", cmds,
		"elapsed_time_ms", utils.TimeSince(startTime),
		zap.Error(err),
	)
}
