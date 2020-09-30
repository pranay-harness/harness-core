package tasks

import (
	"bufio"
	"context"
	"fmt"
	"io"
	"strings"
	"time"

	"github.com/wings-software/portal/commons/go/lib/exec"
	"github.com/wings-software/portal/commons/go/lib/filesystem"
	"github.com/wings-software/portal/commons/go/lib/utils"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

//go:generate mockgen -source run.go -package=steps -destination mocks/run_mock.go RunTask

const (
	defaultTimeoutSecs int64         = 14400 // 4 hour
	defaultNumRetries  int32         = 1
	outputEnvSuffix    string        = "output"
	cmdExitWaitTime    time.Duration = time.Duration(0)
)

// RunTask represents interface to execute a run step
type RunTask interface {
	Run(ctx context.Context) (map[string]string, int32, error)
}

type runTask struct {
	id                string
	displayName       string
	commands          []string
	envVarOutputs     []string
	timeoutSecs       int64
	numRetries        int32
	tmpFilePath       string
	log               *zap.SugaredLogger
	procWriter        io.Writer
	fs                filesystem.FileSystem
	cmdContextFactory exec.CmdContextFactory
}

// NewRunTask creates a run step executor
func NewRunTask(step *pb.UnitStep, tmpFilePath string, log *zap.SugaredLogger, w io.Writer) RunTask {
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
		commands:          r.GetCommands(),
		tmpFilePath:       tmpFilePath,
		envVarOutputs:     r.GetEnvVarOutputs(),
		timeoutSecs:       timeoutSecs,
		numRetries:        numRetries,
		cmdContextFactory: exec.OsCommandContextGracefulWithLog(log),
		log:               log,
		fs:                fs,
		procWriter:        w,
	}
}

// Executes customer provided run step commands with retries and timeout handling
func (e *runTask) Run(ctx context.Context) (map[string]string, int32, error) {
	var err error
	var o map[string]string
	for i := int32(1); i <= e.numRetries; i++ {
		if o, err = e.execute(ctx, i); err == nil {
			return o, i, nil
		}
	}
	return nil, e.numRetries, err
}

// Fetches map of env variable and value from OutputFile. OutputFile stores all env variable and value
func (e *runTask) fetchOutputVariables(outputFile string) (map[string]string, error) {
	envVarMap := make(map[string]string)
	f, err := e.fs.Open(outputFile)
	if err != nil {
		e.log.Errorw("Failed to open output file", zap.Error(err))
		return nil, err
	}
	defer f.Close()

	s := bufio.NewScanner(f)
	for s.Scan() {
		line := s.Text()
		sa := strings.Split(line, " ")
		if len(sa) != 2 {
			e.log.Warnw(
				"output variable does not exist",
				"variable", sa[0],
			)
		} else {
			envVarMap[sa[0]] = sa[1]
		}
	}
	if err := s.Err(); err != nil {
		e.log.Errorw("Failed to create scanner from output file", zap.Error(err))
		return nil, err
	}
	return envVarMap, nil
}

func (e *runTask) execute(ctx context.Context, retryCount int32) (map[string]string, error) {
	start := time.Now()
	ctx, cancel := context.WithTimeout(ctx, time.Second*time.Duration(e.timeoutSecs))
	defer cancel()

	outputFile := fmt.Sprintf("%s/%s%s", e.tmpFilePath, e.id, outputEnvSuffix)
	inputCommands := e.commands
	for _, o := range e.envVarOutputs {
		inputCommands = append(inputCommands, fmt.Sprintf("echo %s $%s >> %s", o, o, outputFile))
	}

	commands := fmt.Sprintf("set -e; %s", strings.Join(inputCommands[:], ";"))
	cmdArgs := []string{"-c", commands}

	e.log.Infow(fmt.Sprintf("Executing %s", commands))
	cmd := e.cmdContextFactory.CmdContextWithSleep(ctx, cmdExitWaitTime, "sh", cmdArgs...).
		WithStdout(e.procWriter).WithStderr(e.procWriter).WithEnvVarsMap(nil)
	err := cmd.Run()
	if ctxErr := ctx.Err(); ctxErr == context.DeadlineExceeded {
		logCommandExecErr(e.log, "timeout while executing run step", e.id, commands, retryCount, start, ctxErr)
		return nil, ctxErr
	}

	if err != nil {
		logCommandExecErr(e.log, "error encountered while executing run step", e.id, commands, retryCount, start, err)
		return nil, err
	}

	stepOutput := make(map[string]string)
	if e.envVarOutputs != nil {
		var err error
		outputVars, err := e.fetchOutputVariables(outputFile)
		if err != nil {
			logCommandExecErr(e.log, "error encountered while fetching output of run step", e.id, commands, retryCount, start, err)
			return nil, err
		}

		stepOutput = outputVars
	}

	e.log.Infow(
		"Successfully executed step",
		"arguments", commands,
		"output", stepOutput,
		"elapsed_time_ms", utils.TimeSince(start),
	)
	return stepOutput, nil
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
