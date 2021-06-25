package tasks

import (
	"context"
	"fmt"
	"io"
	"os"
	"strings"
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
			return o, i, nil
		}
	}
	return nil, t.numRetries, err
}

// resolveExprInEnv resolves JEXL expressions & env var present in plugin settings environment variables
func (t *pluginTask) resolveExprInEnv(ctx context.Context) (map[string]string, error) {
	envVarMap := getEnvVars()
	for k, v := range t.environment {
		envVarMap[k] = v
	}
	m, err := resolver.ResolveJEXLInMapValues(ctx, envVarMap, t.id, t.prevStepOutputs, t.log)
	if err != nil {
		return nil, err
	}

	resolvedSecretMap, err := resolver.ResolveSecretInMapValues(m)
	if err != nil {
		return nil, err
	}

	return resolver.ResolveEnvInMapValues(resolvedSecretMap), nil
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

	err = runCmd(ctx, t.id, commands[0], commands[1:], t.procWriter, t.procWriter, envVarsMap, retryCount, start,
		t.logMetrics, t.addonLogger)
	if err != nil {
		return nil, err
	}

	artifactProto, artifactErr := artifact.GetArtifactProtoFromFile(t.artifactFilePath)
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
