package steps

import (
	"bufio"
	"context"
	"fmt"
	"os/exec"
	"strings"
	"time"

	"github.com/wings-software/portal/commons/go/lib/filesystem"
	"github.com/wings-software/portal/commons/go/lib/utils"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

const (
	defaultTimeoutSecs int64  = 300 // 5 minutes
	defaultNumRetries  int32  = 1
	outputEnvSuffix    string = "output"
)

var execCmdCtx = exec.CommandContext

// RunStep represents interface to execute a run step
type RunStep interface {
	Run(ctx context.Context) error
}

type runStep struct {
	id            string
	displayName   string
	tmpFilePath   string
	commands      []string
	envVarOutputs []string
	timeoutSecs   int64
	numRetries    int32
	relLogPath    string
	log           *zap.SugaredLogger
	fs            filesystem.FileSystem
}

// NewRunStep creates a run step executor
func NewRunStep(step *pb.Step, relLogPath string, tmpFilePath string, fs filesystem.FileSystem, log *zap.SugaredLogger) RunStep {
	r := step.GetRun()
	timeoutSecs := r.GetContext().GetExecutionTimeoutSecs()
	if timeoutSecs == 0 {
		timeoutSecs = defaultTimeoutSecs
	}

	numRetries := r.GetContext().GetNumRetries()
	if numRetries == 0 {
		numRetries = defaultNumRetries
	}
	return &runStep{
		id:            step.GetId(),
		displayName:   step.GetDisplayName(),
		commands:      r.GetCommands(),
		tmpFilePath:   tmpFilePath,
		envVarOutputs: r.GetEnvVarOutputs(),
		timeoutSecs:   timeoutSecs,
		numRetries:    numRetries,
		relLogPath:    relLogPath,
		log:           log,
		fs:            fs,
	}
}

// Executes customer provided run step commands with retries and timeout handling
func (e *runStep) Run(ctx context.Context) error {
	var err error
	if err = e.validate(); err != nil {
		return err
	}
	for i := int32(1); i <= e.numRetries; i++ {
		if err = e.execute(ctx, i); err == nil {
			return nil
		}
	}
	return err
}

func (e *runStep) validate() error {
	if len(e.commands) == 0 {
		err := fmt.Errorf("commands in run step should have atleast one item")
		e.log.Warnw(
			"failed to validate run step",
			"step_id", e.id,
			zap.Error(err),
		)
		return err
	}
	return nil
}

// Fetches map of env variable and value from OutputFile. OutputFile stores all env variable and value
func (e *runStep) fetchOutputVariables(outputFile string) (map[string]string, error) {
	envVarMap := make(map[string]string)
	f, err := e.fs.Open(outputFile)
	if err != nil {
		e.log.Errorw("Failed to open output file", "error_msg", zap.Error(err))
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
		e.log.Errorw("Failed to create scanner from output file", "error_msg", zap.Error(err))
		return nil, err
	}
	return envVarMap, nil
}

func (e *runStep) execute(ctx context.Context, retryCount int32) error {
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
	logFilePath := fmt.Sprintf("%s/%s-%d.log", e.relLogPath, e.id, retryCount)
	logFile, err := e.fs.Create(logFilePath)
	if err != nil {
		logFileCreateWarning(e.log, "failed to create log file", logFilePath, e.id, commands, start, err)
		return err
	}
	defer logFile.Close()

	cmd := execCmdCtx(ctx, "sh", cmdArgs...)
	cmd.Stdout = logFile
	cmd.Stderr = logFile
	err = cmd.Run()
	if ctxErr := ctx.Err(); ctxErr == context.DeadlineExceeded {
		logCommandExecWarning(e.log, "time out while executing run step", e.id, commands, retryCount, start, ctxErr)
		return ctxErr
	}

	if err != nil {
		logCommandExecWarning(e.log, "error encountered while executing run step", e.id, commands, retryCount, start, err)
		return err
	}

	outputVars := make(map[string]string)
	if e.envVarOutputs != nil {
		var err error
		outputVars, err = e.fetchOutputVariables(outputFile)
		if err != nil {
			logCommandExecWarning(e.log, "error encountered while fetching output of run step", e.id, commands, retryCount, start, err)
			return err
		}
	}

	e.log.Infow(
		"Successfully executed step",
		"step_id", e.id,
		"arguments", commands,
		"output", outputVars,
		"elapsed_time_ms", utils.TimeSince(start),
	)
	return nil
}

func logFileCreateWarning(log *zap.SugaredLogger, warnMsg, stepID, filePath, args string, startTime time.Time, err error) {
	log.Warnw(
		warnMsg,
		"step_id", stepID,
		"file_path", filePath,
		"arguments", args,
		"elapsed_time_ms", utils.TimeSince(startTime),
		zap.Error(err),
	)
}

func logCommandExecWarning(log *zap.SugaredLogger, warnMsg, stepID, args string, retryCount int32, startTime time.Time, err error) {
	log.Warnw(
		warnMsg,
		"step_id", stepID,
		"arguments", args,
		"retry_count", retryCount,
		"elapsed_time_ms", utils.TimeSince(startTime),
		zap.Error(err),
	)
}
