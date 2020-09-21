package steps

import (
	"bufio"
	"context"
	"fmt"
	"io"
	"os/exec"
	"strings"
	"time"

	"github.com/wings-software/portal/commons/go/lib/filesystem"
	"github.com/wings-software/portal/commons/go/lib/utils"
	"github.com/wings-software/portal/product/ci/engine/jexl"
	"github.com/wings-software/portal/product/ci/engine/output"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
)

//go:generate mockgen -source run.go -package=steps -destination mocks/run_mock.go RunStep

const (
	defaultTimeoutSecs int64  = 300 // 5 minutes
	defaultNumRetries  int32  = 1
	outputEnvSuffix    string = "output"
)

var (
	startTailFn  = StartTail
	stopTailFn   = StopTail
	evaluateJEXL = jexl.EvaluateJEXL
	execCmdCtx   = exec.CommandContext
)

// RunStep represents interface to execute a run step
type RunStep interface {
	Run(ctx context.Context) (*output.StepOutput, int32, error)
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
	stageOutput   output.StageOutput
	log           *zap.SugaredLogger
	procWriter    io.Writer
	fs            filesystem.FileSystem
}

// NewRunStep creates a run step executor
func NewRunStep(step *pb.UnitStep, relLogPath string, tmpFilePath string,
	so output.StageOutput, fs filesystem.FileSystem, log *zap.SugaredLogger, w io.Writer) RunStep {
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
		stageOutput:   so,
		log:           log,
		fs:            fs,
		procWriter:    w,
	}
}

// Executes customer provided run step commands with retries and timeout handling
func (e *runStep) Run(ctx context.Context) (*output.StepOutput, int32, error) {
	var err error
	var o *output.StepOutput
	if err = e.validate(); err != nil {
		return nil, int32(1), err
	}
	if err = e.resolveJEXL(ctx); err != nil {
		return nil, int32(1), err
	}
	for i := int32(1); i <= e.numRetries; i++ {
		if o, err = e.execute(ctx, i); err == nil {
			return o, i, nil
		}
	}
	return nil, e.numRetries, err
}

func (e *runStep) validate() error {
	if len(e.commands) == 0 {
		err := fmt.Errorf("commands in run step should have atleast one item")
		e.log.Warnw(
			"failed to validate run step",
			zap.Error(err),
		)
		return err
	}
	return nil
}

// resolveJEXL resolves JEXL expressions present in run step input
func (e *runStep) resolveJEXL(ctx context.Context) error {
	// JEXL expressions are only present in run step commands
	s := e.commands
	resolvedExprs, err := evaluateJEXL(ctx, s, e.stageOutput, e.log)
	if err != nil {
		return err
	}

	// Updating step commands with the resolved value of JEXL expressions
	var resolvedCmds []string
	for _, cmd := range e.commands {
		if val, ok := resolvedExprs[cmd]; ok {
			resolvedCmds = append(resolvedCmds, val)
		} else {
			resolvedCmds = append(resolvedCmds, cmd)
		}
	}
	e.commands = resolvedCmds
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

func (e *runStep) execute(ctx context.Context, retryCount int32) (*output.StepOutput, error) {
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
	// TODO: (vistaar) Remove file path from run step
	logFilePath := fmt.Sprintf("%s/%s-%d.log", e.relLogPath, e.id, retryCount)
	logFile, err := e.fs.Create(logFilePath)
	if err != nil {
		logFileCreateWarning(e.log, "failed to create log file", logFilePath, e.id, commands, start, err)
		return nil, err
	}
	defer logFile.Close()

	cmd := execCmdCtx(ctx, "sh", cmdArgs...)
	cmd.Stdout = e.procWriter
	cmd.Stderr = e.procWriter
	e.log.Infow(fmt.Sprintf("Executing %s", commands), "owner", "user")
	err = cmd.Run()
	if ctxErr := ctx.Err(); ctxErr == context.DeadlineExceeded {
		logCommandExecWarning(e.log, "time out while executing run step", e.id, commands, retryCount, start, ctxErr)
		return nil, ctxErr
	}

	if err != nil {
		logCommandExecWarning(e.log, "error encountered while executing run step", e.id, commands, retryCount, start, err)
		return nil, err
	}

	var stepOutput *output.StepOutput
	if e.envVarOutputs != nil {
		var err error
		outputVars, err := e.fetchOutputVariables(outputFile)
		if err != nil {
			logCommandExecWarning(e.log, "error encountered while fetching output of run step", e.id, commands, retryCount, start, err)
			return nil, err
		}

		stepOutput = &output.StepOutput{
			Output: outputVars,
		}
	}

	e.log.Infow(
		"Successfully executed step",
		"arguments", commands,
		"output", stepOutput,
		"elapsed_time_ms", utils.TimeSince(start),
		"owner", "user",
	)
	return stepOutput, nil
}

func logFileCreateWarning(log *zap.SugaredLogger, warnMsg, stepID, filePath, args string, startTime time.Time, err error) {
	log.Warnw(
		warnMsg,
		"file_path", filePath,
		"arguments", args,
		"elapsed_time_ms", utils.TimeSince(startTime),
		zap.Error(err),
	)
}

func logCommandExecWarning(log *zap.SugaredLogger, warnMsg, stepID, args string, retryCount int32, startTime time.Time, err error) {
	log.Warnw(
		warnMsg,
		"arguments", args,
		"retry_count", retryCount,
		"elapsed_time_ms", utils.TimeSince(startTime),
		zap.Error(err),
	)
}
