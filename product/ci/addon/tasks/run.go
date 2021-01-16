package tasks

import (
	"bufio"
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"strings"
	"time"

	grpc_retry "github.com/grpc-ecosystem/go-grpc-middleware/retry"
	"github.com/pkg/errors"
	"github.com/wings-software/portal/product/ci/addon/testreports"
	"github.com/wings-software/portal/product/ci/addon/testreports/junit"
	"github.com/wings-software/portal/product/ci/common/external"
	grpcclient "github.com/wings-software/portal/product/ci/engine/grpc/client"

	"github.com/wings-software/portal/commons/go/lib/exec"
	"github.com/wings-software/portal/commons/go/lib/filesystem"
	"github.com/wings-software/portal/commons/go/lib/utils"
	"github.com/wings-software/portal/product/ci/engine/consts"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"go.uber.org/zap"
	"mvdan.cc/sh/v3/syntax"
)

//go:generate mockgen -source run.go -package=tasks -destination mocks/run_mock.go RunTask

const (
	defaultTimeoutSecs int64         = 14400 // 4 hour
	defaultNumRetries  int32         = 1
	outputEnvSuffix    string        = ".out"
	cmdExitWaitTime    time.Duration = time.Duration(0)
	batchSize                        = 100
)

var (
	getTIClient = external.GetTiHTTPClient
	newJunit    = junit.New
)

// RunTask represents interface to execute a run step
type RunTask interface {
	Run(ctx context.Context) (map[string]string, int32, error)
}

type runTask struct {
	id                string
	displayName       string
	command           string
	envVarOutputs     []string
	timeoutSecs       int64
	numRetries        int32
	tmpFilePath       string
	reports           []*pb.Report
	log               *zap.SugaredLogger
	addonLogger       *zap.SugaredLogger
	procWriter        io.Writer
	fs                filesystem.FileSystem
	cmdContextFactory exec.CmdContextFactory
}

// NewRunTask creates a run step executor
func NewRunTask(step *pb.UnitStep, tmpFilePath string, log *zap.SugaredLogger, w io.Writer,
	addonLogger *zap.SugaredLogger) RunTask {
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
		tmpFilePath:       tmpFilePath,
		envVarOutputs:     r.GetEnvVarOutputs(),
		reports:           r.GetReports(),
		timeoutSecs:       timeoutSecs,
		numRetries:        numRetries,
		cmdContextFactory: exec.OsCommandContextGracefulWithLog(log),
		log:               log,
		fs:                fs,
		procWriter:        w,
		addonLogger:       addonLogger,
	}
}

// Executes customer provided run step command with retries and timeout handling
func (e *runTask) Run(ctx context.Context) (map[string]string, int32, error) {
	var err error
	var o map[string]string
	for i := int32(1); i <= e.numRetries; i++ {
		// Collect reports only if the step was completed successfully
		// TODO: (vistaar) Add a failure strategy later if needed
		if o, err = e.execute(ctx, i); err == nil {
			st := time.Now()
			err = e.collectTestReports(ctx)
			if err != nil {
				// If there's an error in collecting reports, we won't retry but
				// the step will be marked as an error
				e.log.Errorw("unable to collect test reports", zap.Error(err))
				break
			}
			if len(e.reports) > 0 {
				e.log.Infow(fmt.Sprintf("collected test reports in %s time", time.Since(st)))
			}
			return o, i, nil
		}
	}
	if err != nil {
		return nil, e.numRetries, err
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
	cmdToExecute := e.getScript(outputFile)
	cmdArgs := []string{"-c", cmdToExecute}

	cmd := e.cmdContextFactory.CmdContextWithSleep(ctx, cmdExitWaitTime, "sh", cmdArgs...).
		WithStdout(e.procWriter).WithStderr(e.procWriter).WithEnvVarsMap(nil)
	err := cmd.Run()
	if ctxErr := ctx.Err(); ctxErr == context.DeadlineExceeded {
		logCommandExecErr(e.log, "timeout while executing run step", e.id, cmdToExecute, retryCount, start, ctxErr)
		return nil, ctxErr
	}

	if err != nil {
		logCommandExecErr(e.log, "error encountered while executing run step", e.id, cmdToExecute, retryCount, start, err)
		return nil, err
	}

	stepOutput := make(map[string]string)
	if e.envVarOutputs != nil {
		var err error
		outputVars, err := e.fetchOutputVariables(outputFile)
		if err != nil {
			logCommandExecErr(e.log, "error encountered while fetching output of run step", e.id, cmdToExecute, retryCount, start, err)
			return nil, err
		}

		stepOutput = outputVars
	}

	e.addonLogger.Infow(
		"Successfully executed run step",
		"arguments", cmdToExecute,
		"output", stepOutput,
		"elapsed_time_ms", utils.TimeSince(start),
	)
	return stepOutput, nil
}

func (e *runTask) getScript(outputVarFile string) string {
	outputVarCmd := ""
	for _, o := range e.envVarOutputs {
		outputVarCmd += fmt.Sprintf("\necho %s $%s >> %s", o, o, outputVarFile)
	}

	command := fmt.Sprintf("set -e\n %s %s", e.command, outputVarCmd)
	logCmd, err := getLoggableCmd(command)
	if err != nil {
		e.addonLogger.Warn("failed to parse command using mvdan/sh. ", "command", command, zap.Error(err))
		return fmt.Sprintf("echo '---%s'\n%s", command, command)
	}
	return logCmd
}

func getLoggableCmd(cmd string) (string, error) {
	parser := syntax.NewParser(syntax.KeepComments(false))
	printer := syntax.NewPrinter(syntax.Minify(false))

	r := strings.NewReader(cmd)
	prog, err := parser.Parse(r, "")
	if err != nil {
		return "", errors.Wrap(err, "failed to parse command")
	}

	var stmts []*syntax.Stmt
	for _, stmt := range prog.Stmts {
		// convert the statement to a string and then encode special characters.
		var buf bytes.Buffer
		if printer.Print(&buf, stmt); err != nil {
			return "", errors.Wrap(err, "failed to parse statement")
		}

		// create a new statement that echos the
		// original shell statement.
		echo := &syntax.Stmt{
			Cmd: &syntax.CallExpr{
				Args: []*syntax.Word{
					{
						Parts: []syntax.WordPart{
							&syntax.Lit{
								Value: "echo",
							},
						},
					},
					{
						Parts: []syntax.WordPart{
							&syntax.SglQuoted{
								Dollar: false,
								Value:  "--- " + buf.String(),
							},
						},
					},
				},
			},
		}
		// append the echo statement and the statement
		stmts = append(stmts, echo)
		stmts = append(stmts, stmt)
	}
	// replace original statements with new statements
	prog.Stmts = stmts

	buf := new(bytes.Buffer)
	printer.Print(buf, prog)
	return buf.String(), nil
}

func (r *runTask) collectTestReports(ctx context.Context) error {
	// Test cases from reports are identified at a per-step level and won't cause overwriting/clashes
	// at the backend.
	for _, report := range r.reports {
		var rep testreports.TestReporter
		var err error

		x := report.GetType() // pass in report type in proto when other reports are reqd
		switch x {
		case pb.Report_UNKNOWN:
			return errors.New("report type is unknown")
		case pb.Report_JUNIT:
			rep = newJunit(report.GetPaths(), r.log)
		}

		var tests []string
		testc, _ := rep.GetTests(ctx)
		for t := range testc {
			jt, _ := json.Marshal(t)
			tests = append(tests, string(jt))
		}

		if len(tests) == 0 {
			return nil // We're not erroring even if we can't find any tests to report
		}

		// Create TI proxy client (lite engine)
		client, err := grpcclient.NewTiProxyClient(consts.LiteEnginePort, r.log)
		stream, err := client.Client().WriteTests(ctx, grpc_retry.Disable())
		var curr []string
		for _, t := range tests {
			curr = append(curr, t)
			if len(curr)%batchSize == 0 {
				in := &pb.WriteTestsRequest{StepId: r.id, Tests: curr}
				if serr := stream.Send(in); serr != nil {
					r.log.Errorw("write tests RPC failed", zap.Error(serr))
				}
				curr = []string{} // ignore RPC failures, try to write whatever you can
			}
		}
		if len(curr) > 0 {
			in := &pb.WriteTestsRequest{StepId: r.id, Tests: curr}
			if serr := stream.Send(in); serr != nil {
				r.log.Errorw("write tests RPC failed", zap.Error(serr))
			}
			curr = []string{}
		}

		// Close the stream and receive result
		_, err = stream.CloseAndRecv()
		if err != nil {
			return err
		}
	}
	return nil
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
