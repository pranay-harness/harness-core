package tasks

import (
	"context"
	"fmt"
	"io"
	"os"
	"strings"
	"time"

	"github.com/wings-software/portal/commons/go/lib/exec"
	"github.com/wings-software/portal/commons/go/lib/expressions"
	"github.com/wings-software/portal/commons/go/lib/images"
	"github.com/wings-software/portal/commons/go/lib/utils"
	"github.com/wings-software/portal/product/ci/addon/remote"
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
)

var (
	getImgMetadata = remote.GetImageEntrypoint
	evaluateJEXL   = remote.EvaluateJEXL
)

// PluginTask represents interface to execute a plugin step
type PluginTask interface {
	Run(ctx context.Context) (int32, error)
}

type pluginTask struct {
	id                string
	displayName       string
	timeoutSecs       int64
	numRetries        int32
	image             string
	prevStepOutputs   map[string]*pb.StepOutput
	log               *zap.SugaredLogger
	procWriter        io.Writer
	cmdContextFactory exec.CmdContextFactory
}

// NewPluginTask creates a plugin step executor
func NewPluginTask(step *pb.UnitStep, prevStepOutputs map[string]*pb.StepOutput,
	log *zap.SugaredLogger, w io.Writer) PluginTask {
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
		timeoutSecs:       timeoutSecs,
		numRetries:        numRetries,
		prevStepOutputs:   prevStepOutputs,
		cmdContextFactory: exec.OsCommandContextGracefulWithLog(log),
		log:               log,
		procWriter:        w,
	}
}

// Executes customer provided plugin with retries and timeout handling
func (e *pluginTask) Run(ctx context.Context) (int32, error) {
	var err error
	for i := int32(1); i <= e.numRetries; i++ {
		if err = e.execute(ctx, i); err == nil {
			return i, nil
		}
	}
	return e.numRetries, err
}

// resolveEnvJEXL resolves JEXL expressions present in plugin settings environment variables
func (e *pluginTask) resolveEnvJEXL(ctx context.Context) (map[string]string, error) {
	envVarMap := getEnvVars()

	var exprsToResolve []string
	for k, v := range envVarMap {
		if strings.HasPrefix(k, settingEnvPrefix) && expressions.IsJEXL(v) {
			exprsToResolve = append(exprsToResolve, v)
		}
	}

	if len(exprsToResolve) == 0 {
		return envVarMap, nil
	}

	resolvedExprs, err := evaluateJEXL(ctx, e.id, exprsToResolve, e.prevStepOutputs, e.log)
	if err != nil {
		return nil, err
	}

	resolvedEnvVarMap := make(map[string]string)
	for k, v := range envVarMap {
		if v, ok := resolvedExprs[v]; ok {
			resolvedEnvVarMap[k] = v
		} else {
			resolvedEnvVarMap[k] = v
		}
	}
	return resolvedEnvVarMap, nil
}

func (e *pluginTask) execute(ctx context.Context, retryCount int32) error {
	start := time.Now()
	ctx, cancel := context.WithTimeout(ctx, time.Second*time.Duration(e.timeoutSecs))
	defer cancel()

	commands, err := e.getEntrypoint(ctx)
	if err != nil {
		logPluginErr(e.log, "failed to find entrypoint for plugin", e.id, commands, retryCount, start, err)
		return err
	}

	if len(commands) == 0 {
		err := fmt.Errorf("plugin entrypoint is empty")
		logPluginErr(e.log, "entrypoint fetched from remote for plugin is empty", e.id, commands, retryCount, start, err)
		return err
	}

	envVarsMap, err := e.resolveEnvJEXL(ctx)
	if err != nil {
		logPluginErr(e.log, "failed to evaluate JEXL expression for settings", e.id, commands, retryCount, start, err)
		return err
	}

	cmd := e.cmdContextFactory.CmdContextWithSleep(ctx, pluginCmdExitWaitTime, commands[0], commands[1:]...).
		WithStdout(e.procWriter).WithStderr(e.procWriter).WithEnvVarsMap(envVarsMap)
	err = cmd.Run()
	if ctxErr := ctx.Err(); ctxErr == context.DeadlineExceeded {
		logPluginErr(e.log, "timeout while executing plugin step", e.id, commands, retryCount, start, ctxErr)
		return ctxErr
	}

	if err != nil {
		logPluginErr(e.log, "error encountered while executing plugin step", e.id, commands, retryCount, start, err)
		return err
	}

	e.log.Infow(
		"Successfully executed plugin",
		"arguments", commands,
		"elapsed_time_ms", utils.TimeSince(start),
	)
	return nil
}

func (e *pluginTask) getEntrypoint(ctx context.Context) ([]string, error) {
	imageSecret, _ := os.LookupEnv(imageSecretEnv)
	return e.combinedEntrypoint(getImgMetadata(ctx, e.id, e.image, imageSecret, e.log))
}

func (e *pluginTask) combinedEntrypoint(ep, cmds []string, err error) ([]string, error) {
	if err != nil {
		return nil, err
	}
	return images.CombinedEntrypoint(ep, cmds), nil
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

// Returns environment variables as a map with key as environment variable name
// and value as environment variable value.
func getEnvVars() map[string]string {
	m := make(map[string]string)
	// os.Environ returns a copy of strings representing the environment in form
	// "key=value". Converting it into a map.
	for _, e := range os.Environ() {
		if i := strings.Index(e, "="); i >= 0 {
			m[e[:i]] = e[i+1:]
		}
	}
	return m
}
