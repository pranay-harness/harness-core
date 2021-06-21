package tasks

import (
	"bufio"
	"context"
	"fmt"
	"io"
	"path/filepath"
	"strings"
	"time"

	"github.com/wings-software/portal/commons/go/lib/exec"
	"github.com/wings-software/portal/commons/go/lib/filesystem"
	"github.com/wings-software/portal/commons/go/lib/utils"
	"github.com/wings-software/portal/product/ci/addon/resolver"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

//go:generate mockgen -source run.go -package=tasks -destination mocks/run_mock.go RunTask

const (
	defaultTimeoutSecs int64         = 14400 // 4 hour
	defaultNumRetries  int32         = 1
	outputEnvSuffix    string        = ".out"
	cmdExitWaitTime    time.Duration = time.Duration(0)
	batchSize                        = 100
)

// RunTask represents interface to execute a run step
type RunTask interface {
	Run(ctx context.Context) (map[string]string, int32, error)
}

type runTask struct {
	id                string
	displayName       string
	command           string
	shellType         pb.ShellType
	envVarOutputs     []string
	environment       map[string]string
	timeoutSecs       int64
	numRetries        int32
	tmpFilePath       string
	prevStepOutputs   map[string]*pb.StepOutput
	reports           []*pb.Report
	logMetrics        bool
	log               *zap.SugaredLogger
	addonLogger       *zap.SugaredLogger
	procWriter        io.Writer
	fs                filesystem.FileSystem
	cmdContextFactory exec.CmdContextFactory
}

// NewRunTask creates a run step executor
func NewRunTask(step *pb.UnitStep, prevStepOutputs map[string]*pb.StepOutput, tmpFilePath string,
	log *zap.SugaredLogger, w io.Writer, logMetrics bool, addonLogger *zap.SugaredLogger) RunTask {
	r := step.GetRun()
	fs := filesystem.NewOSFileSystem(log)

	timeoutSecs := r.GetContext().GetExecutionTimeoutSecs()
	if timeoutSecs == 0 {
		timeoutSecs = defaultTimeoutSecs
	}

	numRetries := r.GetContext().GetNumRetries()
	if numRetries == 0 {
		numRetries = defaultNumRetries
	}
	return &runTask{
		id:                step.GetId(),
		displayName:       step.GetDisplayName(),
		command:           r.GetCommand(),
		shellType:         r.GetShellType(),
		tmpFilePath:       tmpFilePath,
		envVarOutputs:     r.GetEnvVarOutputs(),
		environment:       r.GetEnvironment(),
		reports:           r.GetReports(),
		timeoutSecs:       timeoutSecs,
		numRetries:        numRetries,
		cmdContextFactory: exec.OsCommandContextGracefulWithLog(log),
		prevStepOutputs:   prevStepOutputs,
		logMetrics:        logMetrics,
		log:               log,
		fs:                fs,
		procWriter:        w,
		addonLogger:       addonLogger,
	}
}

// Executes customer provided run step command with retries and timeout handling
func (r *runTask) Run(ctx context.Context) (map[string]string, int32, error) {
	var err error
	var o map[string]string
	for i := int32(1); i <= r.numRetries; i++ {
		if o, err = r.execute(ctx, i); err == nil {
			st := time.Now()
			err = collectTestReports(ctx, r.reports, r.id, r.log)
			if err != nil {
				// If there's an error in collecting reports, we won't retry but
				// the step will be marked as an error
				r.log.Errorw("unable to collect test reports", zap.Error(err))
				return nil, r.numRetries, err
			}
			if len(r.reports) > 0 {
				r.log.Infow(fmt.Sprintf("collected test reports in %s time", time.Since(st)))
			}
			return o, i, nil
		}
	}
	if err != nil {
		// Run step did not execute successfully
		// Try and collect reports, ignore any errors during report collection itself
		errc := collectTestReports(ctx, r.reports, r.id, r.log)
		if errc != nil {
			r.log.Errorw("error while collecting test reports", zap.Error(errc))
		}
		return nil, r.numRetries, err
	}
	return nil, r.numRetries, err
}

// Fetches map of env variable and value from OutputFile. OutputFile stores all env variable and value
func (r *runTask) fetchOutputVariables(outputFile string) (map[string]string, error) {
	envVarMap := make(map[string]string)
	f, err := r.fs.Open(outputFile)
	if err != nil {
		r.log.Errorw("Failed to open output file", zap.Error(err))
		return nil, err
	}
	defer f.Close()

	s := bufio.NewScanner(f)
	for s.Scan() {
		line := s.Text()
		sa := strings.Split(line, " ")
		if len(sa) < 2 {
			r.log.Warnw(
				"output variable does not exist",
				"variable", sa[0],
			)
		} else {
			envVarMap[sa[0]] = line[len(sa[0])+1:]
		}
	}
	if err := s.Err(); err != nil {
		r.log.Errorw("Failed to create scanner from output file", zap.Error(err))
		return nil, err
	}
	return envVarMap, nil
}

func (r *runTask) execute(ctx context.Context, retryCount int32) (map[string]string, error) {
	start := time.Now()
	ctx, cancel := context.WithTimeout(ctx, time.Second*time.Duration(r.timeoutSecs))
	defer cancel()

	outputFile := filepath.Join(r.tmpFilePath, fmt.Sprintf("%s%s", r.id, outputEnvSuffix))
	cmdToExecute, err := r.getScript(ctx, outputFile)
	if err != nil {
		return nil, err
	}

	envVars, err := r.resolveExprInEnv(ctx)
	if err != nil {
		return nil, err
	}

	cmdArgs := []string{"-c", cmdToExecute}

	cmd := r.cmdContextFactory.CmdContextWithSleep(ctx, cmdExitWaitTime, r.getShell(), cmdArgs...).
		WithStdout(r.procWriter).WithStderr(r.procWriter).WithEnvVarsMap(envVars)
	err = runCmd(ctx, cmd, r.id, cmdArgs, retryCount, start, r.logMetrics, r.addonLogger)
	if err != nil {
		return nil, err
	}

	stepOutput := make(map[string]string)
	if len(r.envVarOutputs) != 0 {
		var err error
		outputVars, err := r.fetchOutputVariables(outputFile)
		if err != nil {
			logCommandExecErr(r.log, "error encountered while fetching output of run step", r.id, cmdToExecute, retryCount, start, err)
			return nil, err
		}

		stepOutput = outputVars
	}

	r.addonLogger.Infow(
		"Successfully executed run step",
		"arguments", cmdToExecute,
		"output", stepOutput,
		"elapsed_time_ms", utils.TimeSince(start),
	)
	return stepOutput, nil
}

func (r *runTask) getScript(ctx context.Context, outputVarFile string) (string, error) {
	outputVarCmd := ""
	for _, o := range r.envVarOutputs {
		outputVarCmd += fmt.Sprintf("\necho %s $%s >> %s", o, o, outputVarFile)
	}

	resolvedCmd, err := r.resolveExprInCmd(ctx)
	if err != nil {
		return "", err
	}

	// Using set -xe instead of printing command via utils.GetLoggableCmd(command) since if ' is present in a command,
	// echo on the command fails with an error.
	command := fmt.Sprintf("set -xe\n%s %s", resolvedCmd, outputVarCmd)
	return command, nil
}

func (r *runTask) getShell() string {
	if r.shellType == pb.ShellType_BASH {
		return "bash"
	}
	return "sh"
}

// resolveExprInEnv resolves JEXL expressions & env var present in plugin settings environment variables
func (r *runTask) resolveExprInEnv(ctx context.Context) (map[string]string, error) {
	envVarMap := getEnvVars()
	for k, v := range r.environment {
		envVarMap[k] = v
	}
	m, err := resolver.ResolveJEXLInMapValues(ctx, envVarMap, r.id, r.prevStepOutputs, r.log)
	if err != nil {
		return nil, err
	}

	resolvedSecretMap, err := resolver.ResolveSecretInMapValues(m)
	if err != nil {
		return nil, err
	}

	return resolver.ResolveEnvInMapValues(resolvedSecretMap), nil
}

// resolveExprInCmd resolves JEXL expressions & secret present in command
func (r *runTask) resolveExprInCmd(ctx context.Context) (string, error) {
	c, err := resolver.ResolveJEXLInString(ctx, r.command, r.id, r.prevStepOutputs, r.log)
	if err != nil {
		return "", err
	}

	resolvedCmd, err := resolver.ResolveSecretInString(c)
	if err != nil {
		return "", err
	}

	return resolvedCmd, nil
}

func logCommandExecErr(log *zap.SugaredLogger, errMsg, stepID, args string, retryCount int32, startTime time.Time, err error) {
	log.Errorw(
		errMsg,
		"arguments", args,
		"retry_count", retryCount,
		"elapsed_time_ms", utils.TimeSince(startTime),
		zap.Error(err),
	)
}
