package tasks

import (
	"context"
	"fmt"
	"io"
	"strings"
	"time"

	"github.com/pkg/errors"
	"github.com/wings-software/portal/commons/go/lib/exec"
	"github.com/wings-software/portal/commons/go/lib/utils"
	"github.com/wings-software/portal/product/ci/common/external"
	pb "github.com/wings-software/portal/product/ci/engine/proto"
	"github.com/wings-software/portal/product/ci/ti-service/types"
	"go.uber.org/zap"
)

const (
	defaultRunTestsTimeout int64 = 14400 // 4 hour
	defaultRunTestsRetries int32 = 1
	mvnCmd                       = "mvn"
	bazelCmd                     = "bazel"
	mvnAgentArg                  = "-DargLine=-javaagent:/step-exec/.harness/bin/java-agent.jar=%s/config.ini"
)

// RunTestsTask represents an interface to run tests intelligently
type RunTestsTask interface {
	Run(ctx context.Context) (int32, error)
}

type runTestsTask struct {
	id                   string
	displayName          string
	reports              []*pb.Report
	diffFiles            []string
	timeoutSecs          int64
	numRetries           int32
	preCommand           string // command to run before the actual tests
	postCommand          string // command to run after the test execution
	args                 string // custom flags to run the tests
	language             string // language of codebase
	buildTool            string // buildTool used for codebase
	packages             string // Packages ti will generate callgraph for
	annotations          string // Annotations to identify tests for instrumentation
	runOnlySelectedTests bool   // Flag to be used for disabling testIntelligence and running all tests
	logMetrics           bool
	log                  *zap.SugaredLogger
	addonLogger          *zap.SugaredLogger
	procWriter           io.Writer
	cmdContextFactory    exec.CmdContextFactory
}

func NewRunTestsTask(step *pb.UnitStep, log *zap.SugaredLogger,
	w io.Writer, logMetrics bool, addonLogger *zap.SugaredLogger) RunTestsTask {
	r := step.GetRunTests()
	timeoutSecs := r.GetContext().GetExecutionTimeoutSecs()
	if timeoutSecs == 0 {
		timeoutSecs = defaultRunTestsTimeout
	}

	numRetries := r.GetContext().GetNumRetries()
	if numRetries == 0 {
		numRetries = defaultRunTestsRetries
	}
	return &runTestsTask{
		id:                   step.GetId(),
		displayName:          step.GetDisplayName(),
		timeoutSecs:          timeoutSecs,
		diffFiles:            r.GetDiffFiles(),
		numRetries:           numRetries,
		reports:              r.GetReports(),
		preCommand:           r.GetPreTestCommand(),
		postCommand:          r.GetPostTestCommand(),
		args:                 r.GetArgs(),
		language:             r.GetLanguage(),
		buildTool:            r.GetBuildTool(),
		packages:             r.GetPackages(),
		annotations:          r.GetTestAnnotations(),
		runOnlySelectedTests: r.GetRunOnlySelectedTests(),
		cmdContextFactory:    exec.OsCommandContextGracefulWithLog(log),
		logMetrics:           logMetrics,
		log:                  log,
		procWriter:           w,
		addonLogger:          addonLogger,
	}
}

// Execute commands with timeout and retry handling
func (r *runTestsTask) Run(ctx context.Context) (int32, error) {
	var err error
	for i := int32(1); i <= r.numRetries; i++ {
		if err = r.execute(ctx, i); err == nil {
			st := time.Now()
			err = collectTestReports(ctx, r.reports, r.id, r.log)
			if err != nil {
				// If there's an error in collecting reports, we won't retry but
				// the step will be marked as an error
				r.log.Errorw("unable to collect test reports", zap.Error(err))
				return r.numRetries, err
			}
			if len(r.reports) > 0 {
				r.log.Infow(fmt.Sprintf("collected test reports in %s time", time.Since(st)))
			}
			return i, nil
		}
	}
	if err != nil {
		// Run step did not execute successfully
		// Try and collect reports, ignore any errors during report collection itself
		errc := collectTestReports(ctx, r.reports, r.id, r.log)
		if errc != nil {
			r.log.Errorw("error while collecting test reports", zap.Error(errc))
		}
		return r.numRetries, err
	}
	return r.numRetries, err
}

func (r *runTestsTask) getMavenCmd(tests []types.RunnableTest) (string, error) {
	if !r.runOnlySelectedTests {
		// Run all the tests
		// TODO -- Aman - check if instumentation is required here too.
		return fmt.Sprintf("%s %s -am", mvnCmd, r.args), nil
	}
	if len(tests) == 0 {
		return fmt.Sprintf("echo \"Skipping test run, received no tests to execute\""), nil
	}
	// Use only unique classes
	// TODO: Figure out how to incorporate package information in this
	set := make(map[string]interface{})
	ut := []string{}
	for _, t := range tests {
		if _, ok := set[t.Class]; ok {
			// The class has already been added
			continue
		}
		set[t.Class] = struct{}{}
		ut = append(ut, t.Class)
	}
	testStr := strings.Join(ut, ",")
	wrkspcPath, err := external.GetWrkspcPath()
	if err != nil {
		return "", errors.Wrap(err, "error while getting maven command")
	}
	fAgentArg := fmt.Sprintf(mvnAgentArg, wrkspcPath)
	return fmt.Sprintf("%s %s -Dtest=%s -am %s", mvnCmd, r.args, testStr, fAgentArg), nil
}

func (r *runTestsTask) getBazelCmd(ctx context.Context, tests []types.RunnableTest) string {
	defaultCmd := fmt.Sprintf("%s %s //...", bazelCmd, r.args) // run all the tests
	if !r.runOnlySelectedTests {
		// Run all the tests
		return defaultCmd
	}
	if len(tests) == 0 {
		return fmt.Sprintf("echo \"Skipping test run, received no tests to execute\"")
	}
	// Use only unique classes
	pkgs := []string{}
	clss := []string{}
	set := make(map[string]interface{})
	ut := []string{}
	for _, t := range tests {
		if _, ok := set[t.Class]; ok {
			// The class has already been added
			continue
		}
		set[t.Class] = struct{}{}
		ut = append(ut, t.Class)
		pkgs = append(pkgs, t.Pkg)
		clss = append(clss, t.Class)
	}
	rules := []string{} // List of bazel rules to execute
	for i := 0; i < len(pkgs); i++ {
		c := fmt.Sprintf("%s query 'attr(name, %s.%s, //...)'", bazelCmd, pkgs[i], clss[i])
		cmdArgs := []string{"-c", c}
		resp, err := r.cmdContextFactory.CmdContextWithSleep(ctx, cmdExitWaitTime, "sh", cmdArgs...).Output()
		if err != nil {
			r.log.Errorw(fmt.Sprintf("could not find an appropriate rule for pkgs %s and class %s", pkgs[i], clss[i]),
				"index", i, "command", c, zap.Error(err))
			// Run all the tests
			return defaultCmd
		}
		rules = append(rules, strings.TrimSuffix(string(resp), "\n"))
	}
	testList := strings.Join(rules, " ")
	return fmt.Sprintf("%s %s %s", bazelCmd, r.args, testList)
}

func valid(tests []types.RunnableTest) bool {
	for _, t := range tests {
		if t.Class == "" {
			return false
		}
	}
	return true
}

func (r *runTestsTask) getCmd(ctx context.Context) (string, error) {
	// Get the tests that need to be run if we are running selected tests
	var err error
	tests := []types.RunnableTest{}
	if r.runOnlySelectedTests {
		tests, err = selectTests(ctx, r.diffFiles, r.id, r.log)
		if err != nil {
			r.log.Errorw("there was some issue in trying to figure out tests to run. Running all the tests", zap.Error(err))
			// Set run only selected tests to false if there was some issue in the response
			r.runOnlySelectedTests = false
		}
		if !valid(tests) {
			r.log.Warnw("did not receive accurate test list from TI service. This may be because TI service wants to run all the tests to be sure. Running all the tests")
			r.runOnlySelectedTests = false
		}
	}

	var testCmd string
	switch r.buildTool {
	case "maven":
		r.log.Infow("setting up maven as the build tool")
		testCmd, err = r.getMavenCmd(tests)
		if err != nil {
			return "", err
		}
	case "bazel":
		r.log.Infow("setting up bazel as the build tool")
		testCmd = r.getBazelCmd(ctx, tests)
	default:
		return "", fmt.Errorf("build tool %s is not suported", r.buildTool)
	}

	command := fmt.Sprintf("set -e\n%s\n%s\n%s", r.preCommand, testCmd, r.postCommand)
	logCmd, err := utils.GetLoggableCmd(command)
	if err != nil {
		r.addonLogger.Warn("failed to parse command using mvdan/sh. ", "command", command, zap.Error(err))
		return fmt.Sprintf("echo '---%s'\n%s", command, command), nil
	}
	return logCmd, nil
}

func (r *runTestsTask) execute(ctx context.Context, retryCount int32) error {
	start := time.Now()
	ctx, cancel := context.WithTimeout(ctx, time.Second*time.Duration(r.timeoutSecs))
	defer cancel()

	cmdToExecute, err := r.getCmd(ctx)
	if err != nil {
		r.log.Errorw("could not create run command", zap.Error(err))
		return err
	}
	cmdArgs := []string{"-c", cmdToExecute}

	cmd := r.cmdContextFactory.CmdContextWithSleep(ctx, cmdExitWaitTime, "sh", cmdArgs...).
		WithStdout(r.procWriter).WithStderr(r.procWriter).WithEnvVarsMap(nil)
	err = runCmd(ctx, cmd, r.id, cmdArgs, retryCount, start, r.logMetrics, r.log, r.addonLogger)
	if err != nil {
		return err
	}

	r.addonLogger.Infow(
		"successfully executed run tests step",
		"arguments", cmdToExecute,
		"elapsed_time_ms", utils.TimeSince(start),
	)
	return nil
}
