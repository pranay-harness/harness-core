package tasks

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"regexp"
	"strings"
	"time"

	"github.com/wings-software/portal/commons/go/lib/exec"
	"github.com/wings-software/portal/commons/go/lib/filesystem"
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
	outDir                       = "%s/ti/callgraph/"    // path passed as outDir in the config.ini file
	cgDir                        = "%s/ti/callgraph/cg/" // path where callgraph files will be generated
	// TODO: (vistaar) move the java agent path to come as an env variable from CI manager,
	// as it is also used in init container.
	javaAgentArg = "-javaagent:/addon/bin/java-agent.jar=%s"
	tiConfigPath = ".ticonfig.yaml"
)

var (
	selectTestsFn        = selectTests
	collectCgFn          = collectCg
	collectTestReportsFn = collectTestReports
	runCmdFn             = runCmd
	isManualFn           = external.IsManualExecution
)

// RunTestsTask represents an interface to run tests intelligently
type RunTestsTask interface {
	Run(ctx context.Context) (int32, error)
}

type runTestsTask struct {
	id          string
	fs          filesystem.FileSystem
	displayName string
	reports     []*pb.Report
	// List of files which have been modified in the PR. This is marshalled form of types.File{}
	// This is done to avoid redefining the structs in code as well as proto.
	// Calls via lite engine use json encoded structs and can be decoded
	// on the TI service.
	diffFiles            string // JSON encoded string of a types.File{} object
	timeoutSecs          int64
	numRetries           int32
	tmpFilePath          string
	preCommand           string // command to run before the actual tests
	postCommand          string // command to run after the test execution
	args                 string // custom flags to run the tests
	language             string // language of codebase
	buildTool            string // buildTool used for codebase
	packages             string // Packages ti will generate callgraph for
	annotations          string // Annotations to identify tests for instrumentation
	runOnlySelectedTests bool   // Flag to be used for disabling testIntelligence and running all tests
	envVarOutputs        []string
	environment          map[string]string
	logMetrics           bool
	log                  *zap.SugaredLogger
	addonLogger          *zap.SugaredLogger
	procWriter           io.Writer
	cmdContextFactory    exec.CmdContextFactory
}

func NewRunTestsTask(step *pb.UnitStep, tmpFilePath string, log *zap.SugaredLogger,
	w io.Writer, logMetrics bool, addonLogger *zap.SugaredLogger) *runTestsTask {
	r := step.GetRunTests()
	fs := filesystem.NewOSFileSystem(log)
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
		fs:                   fs,
		displayName:          step.GetDisplayName(),
		timeoutSecs:          timeoutSecs,
		diffFiles:            r.GetDiffFiles(),
		tmpFilePath:          tmpFilePath,
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
		envVarOutputs:        r.GetEnvVarOutputs(),
		environment:          r.GetEnvironment(),
		cmdContextFactory:    exec.OsCommandContextGracefulWithLog(log),
		logMetrics:           logMetrics,
		log:                  log,
		procWriter:           w,
		addonLogger:          addonLogger,
	}
}

// Execute commands with timeout and retry handling
func (r *runTestsTask) Run(ctx context.Context) (map[string]string, int32, error) {
	var err, errCg error
	var o map[string]string
	cgDir := fmt.Sprintf(cgDir, r.tmpFilePath)
	testSt := time.Now()
	for i := int32(1); i <= r.numRetries; i++ {
		if o, err = r.execute(ctx, i); err == nil {
			cgSt := time.Now()
			// even if the collectCg fails, try to collect reports. Both are parallel features and one should
			// work even if the other one fails
			errCg = collectCgFn(ctx, r.id, cgDir, time.Since(testSt).Milliseconds(), r.log)
			cgTime := time.Since(cgSt)
			repoSt := time.Now()
			err = collectTestReportsFn(ctx, r.reports, r.id, r.log)
			repoTime := time.Since(repoSt)
			if errCg != nil {
				// If there's an error in collecting callgraph, we won't retry but
				// the step will be marked as an error
				r.log.Errorw(fmt.Sprintf("unable to collect callgraph. Time taken: %s", cgTime), zap.Error(errCg))
				if err != nil {
					r.log.Errorw(fmt.Sprintf("unable to collect tests reports. Time taken: %s", repoTime), zap.Error(err))
				}
				return nil, r.numRetries, errCg
			}
			if err != nil {
				// If there's an error in collecting reports, we won't retry but
				// the step will be marked as an error
				r.log.Errorw(fmt.Sprintf("unable to collect test reports. Time taken: %s", repoTime), zap.Error(err))
				return nil, r.numRetries, err
			}
			if len(r.reports) > 0 {
				r.log.Infow(fmt.Sprintf("successfully collected test reports in %s time", repoTime))
			}
			r.log.Infow(fmt.Sprintf("successfully uploaded partial callgraph in %s time", cgTime))
			return o, i, nil
		}
	}
	if err != nil {
		// Run step did not execute successfully
		// Try and collect callgraph and reports, ignore any errors during collection steps itself
		errCg = collectCgFn(ctx, r.id, cgDir, time.Since(testSt).Milliseconds(), r.log)
		errc := collectTestReportsFn(ctx, r.reports, r.id, r.log)
		if errc != nil {
			r.log.Errorw("error while collecting test reports", zap.Error(errc))
		}
		if errCg != nil {
			r.log.Errorw("error while collecting callgraph", zap.Error(errCg))
		}
		return nil, r.numRetries, err
	}
	return nil, r.numRetries, err
}

// createJavaAgentArg creates the ini file which is required as input to the java agent
// and returns back the arg to be added to the test command for generation of partial
// call graph.
func (r *runTestsTask) createJavaAgentArg() (string, error) {
	// Create config file
	dir := fmt.Sprintf(outDir, r.tmpFilePath)
	err := r.fs.MkdirAll(dir, os.ModePerm)
	if err != nil {
		r.log.Errorw(fmt.Sprintf("could not create nested directory %s", dir), zap.Error(err))
		return "", err
	}
	data := fmt.Sprintf(`outDir: %s
logLevel: 0
logConsole: false
writeTo: COVERAGE_JSON
instrPackages: %s`, dir, r.packages)
	// Add test annotations if they were provided
	if r.annotations != "" {
		data = data + "\n" + fmt.Sprintf("testAnnotations: %s", r.annotations)
	}
	iniFile := fmt.Sprintf("%s/config.ini", r.tmpFilePath)
	r.log.Infow(fmt.Sprintf("attempting to write %s to %s", data, iniFile))
	f, err := r.fs.Create(iniFile)
	if err != nil {
		r.log.Errorw(fmt.Sprintf("could not create file %s", iniFile), zap.Error(err))
		return "", err
	}
	_, err = f.Write([]byte(data))
	if err != nil {
		r.log.Errorw(fmt.Sprintf("could not write %s to file %s", data, iniFile), zap.Error(err))
		return "", err
	}
	// Return java agent arg
	return fmt.Sprintf(javaAgentArg, iniFile), nil
}

func (r *runTestsTask) getMavenCmd(tests []types.RunnableTest) (string, error) {
	instrArg, err := r.createJavaAgentArg()
	if err != nil {
		return "", err
	}
	re := regexp.MustCompile(`(-Duser\.\S*)`)
	s := re.FindAllString(r.args, -1)
	if s != nil {
		// If user args are present, move them to instrumentation
		r.args = re.ReplaceAllString(r.args, "")                            // Remove from arg
		instrArg = fmt.Sprintf("\"%s %s\"", strings.Join(s, " "), instrArg) // Add to instrumentation
	}
	if !r.runOnlySelectedTests {
		// Run all the tests
		// TODO -- Aman - check if instumentation is required here too.
		return strings.TrimSpace(fmt.Sprintf("%s -am -DargLine=%s %s", mvnCmd, instrArg, r.args)), nil
	}
	if len(tests) == 0 {
		return fmt.Sprintf("echo \"Skipping test run, received no tests to execute\""), nil
	}
	// Use only unique <package, class> tuples
	set := make(map[types.RunnableTest]interface{})
	ut := []string{}
	for _, t := range tests {
		w := types.RunnableTest{Pkg: t.Pkg, Class: t.Class}
		if _, ok := set[w]; ok {
			// The test has already been added
			continue
		}
		set[w] = struct{}{}
		if t.Pkg != "" {
			ut = append(ut, t.Pkg+"."+t.Class) // We should always have a package name. If not, use class to run
		} else {
			ut = append(ut, t.Class)
		}
	}
	testStr := strings.Join(ut, ",")
	return strings.TrimSpace(fmt.Sprintf("%s -Dtest=%s -am -DargLine=%s %s", mvnCmd, testStr, instrArg, r.args)), nil
}

func (r *runTestsTask) getBazelCmd(ctx context.Context, tests []types.RunnableTest) (string, error) {
	//tests = []types.RunnableTest{
	//	{Pkg: "io.harness.pms.pipeline.service", Class: "PMSPipelineServiceImplTest"},
	//	{Pkg: "software.wings.service.impl.trigger", Class: "TriggerServiceTest"},
	//	{Pkg: "software.wings.service.impl.workflow", Class: "WorkflowServiceTest"},
	//	{Pkg: "software.wings.service.impl.verification", Class: "CVConfigurationServiceImplTest"},
	//}
	instrArg, err := r.createJavaAgentArg()
	if err != nil {
		return "", err
	}
	bazelInstrArg := fmt.Sprintf("--define=HARNESS_ARGS=%s", instrArg)
	defaultCmd := fmt.Sprintf("%s %s %s //...", bazelCmd, r.args, bazelInstrArg) // run all the tests
	if !r.runOnlySelectedTests {
		// Run all the tests
		return defaultCmd, nil
	}
	if len(tests) == 0 {
		return fmt.Sprintf("echo \"Skipping test run, received no tests to execute\""), nil
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
	rulesM := make(map[string]struct{})
	rules := []string{} // List of unique bazel rules to be executed
	for i := 0; i < len(pkgs); i++ {
		c := fmt.Sprintf("%s query 'attr(name, %s.%s, //...)'", bazelCmd, pkgs[i], clss[i])
		cmdArgs := []string{"-c", c}
		resp, err := r.cmdContextFactory.CmdContextWithSleep(ctx, cmdExitWaitTime, "sh", cmdArgs...).Output()
		if err != nil || len(resp) == 0 {
			r.log.Errorw(fmt.Sprintf("could not find an appropriate rule for pkgs %s and class %s", pkgs[i], clss[i]),
				"index", i, "command", c, zap.Error(err))
			// Hack to get bazel rules for portal
			// TODO: figure out how to generically get rules to be executed from a package and a class
			// Example commands:
			//     find . -path "*pkg.class" -> can have multiple tests (eg helper/base tests)
			//     export fullname=$(bazelisk query path.java)
			//     bazelisk query "attr('srcs', $fullname, ${fullname//:*/}:*)" --output=label_kind | grep "java_test rule"

			// Get list of paths for the tests
			pathCmd := fmt.Sprintf(`find . -path '*%s/%s*' | sed -e "s/^\.\///g"`, strings.Replace(pkgs[i], ".", "/", -1), clss[i])
			cmdArgs = []string{"-c", pathCmd}
			pathResp, pathErr := r.cmdContextFactory.CmdContextWithSleep(ctx, cmdExitWaitTime, "sh", cmdArgs...).Output()
			if pathErr != nil {
				r.log.Errorw(fmt.Sprintf("could not find path for pkgs %s and class %s", pkgs[i], clss[i]), zap.Error(pathErr))
				continue
			}
			// Iterate over the paths and try to find the relevant rules
			for _, p := range strings.Split(string(pathResp), "\n") {
				p = strings.TrimSpace(p)
				if len(p) == 0 || !strings.Contains(p, "src/test") {
					continue
				}
				c = fmt.Sprintf("export fullname=$(%s query %s)\n"+
					"%s query \"attr('srcs', $fullname, ${fullname//:*/}:*)\" --output=label_kind | grep 'java_test rule'",
					bazelCmd, p, bazelCmd)
				cmdArgs = []string{"-c", c}
				resp2, err2 := r.cmdContextFactory.CmdContextWithSleep(ctx, cmdExitWaitTime, "sh", cmdArgs...).Output()
				if err2 != nil || len(resp2) == 0 {
					r.log.Errorw(fmt.Sprintf("could not find an appropriate rule in failback for path %s", p), zap.Error(err2))
					continue
				}
				t := strings.Fields(string(resp2))
				resp = []byte(t[2])
				r := strings.TrimSuffix(string(resp), "\n")
				if _, ok := rulesM[r]; !ok {
					rules = append(rules, r)
					rulesM[r] = struct{}{}
				}
			}
		} else {
			r := strings.TrimSuffix(string(resp), "\n")
			if _, ok := rulesM[r]; !ok {
				rules = append(rules, r)
				rulesM[r] = struct{}{}
			}
		}

	}
	if len(rules) == 0 {
		return fmt.Sprintf("echo \"Could not find any relevant test rules. Skipping the run\""), nil
	}
	testList := strings.Join(rules, " ")
	return fmt.Sprintf("%s %s %s %s", bazelCmd, r.args, bazelInstrArg, testList), nil
}

func valid(tests []types.RunnableTest) bool {
	for _, t := range tests {
		if t.Class == "" {
			return false
		}
	}
	return true
}

func (r *runTestsTask) getCmd(ctx context.Context, outputVarFile string) (string, error) {
	// Get the tests that need to be run if we are running selected tests
	var selection types.SelectTestsResp
	var files []types.File
	err := json.Unmarshal([]byte(r.diffFiles), &files)
	if err != nil {
		return "", err
	}
	isManual := isManualFn()
	if len(files) == 0 {
		r.log.Errorw("unable to get changed files list")
		r.runOnlySelectedTests = false // run all the tests if we could not find changed files list correctly
	}
	if isManual {
		r.log.Infow("detected manual execution - for intelligence to be configured, a PR must be raised. Running all the tests.")
		r.runOnlySelectedTests = false // run all the tests if it is a manual execution
	}
	selection, err = selectTestsFn(ctx, files, r.runOnlySelectedTests, r.id, r.log, r.fs)
	if err != nil {
		r.log.Errorw("there was some issue in trying to intelligently figure out tests to run. Running all the tests.", zap.Error(err))
		r.runOnlySelectedTests = false // run all the tests if an error was encountered
	} else if !valid(selection.Tests) { // This shouldn't happen
		r.log.Warnw("test intelligence did not return suitable tests")
		r.runOnlySelectedTests = false // TI did not return suitable tests
	} else if selection.SelectAll == true {
		r.log.Infow("intelligently determined to run all the tests")
		r.runOnlySelectedTests = false // TI selected all the tests to be run
	} else {
		r.log.Infow(fmt.Sprintf("intelligently running tests: %s", selection.Tests))
	}

	var testCmd string
	switch r.buildTool {
	case "maven":
		r.log.Infow("setting up maven as the build tool")
		testCmd, err = r.getMavenCmd(selection.Tests)
		if err != nil {
			return "", err
		}
	case "bazel":
		r.log.Infow("setting up bazel as the build tool")
		testCmd, err = r.getBazelCmd(ctx, selection.Tests)
		if err != nil {
			return "", err
		}
	default:
		return "", fmt.Errorf("build tool %s is not suported", r.buildTool)
	}

	outputVarCmd := ""
	for _, o := range r.envVarOutputs {
		outputVarCmd += fmt.Sprintf("\necho %s $%s >> %s", o, o, outputVarFile)
	}

	// TMPDIR needs to be set for some build tools like bazel
	command := fmt.Sprintf("set -xe\nexport TMPDIR=%s\n%s\n%s\n%s%s", r.tmpFilePath, r.preCommand, testCmd, r.postCommand, outputVarCmd)
	resolvedCmd, err := resolveExprInCmd(command)
	if err != nil {
		return "", err
	}

	return resolvedCmd, nil
}

func (r *runTestsTask) execute(ctx context.Context, retryCount int32) (map[string]string, error) {
	start := time.Now()
	ctx, cancel := context.WithTimeout(ctx, time.Second*time.Duration(r.timeoutSecs))
	defer cancel()

	outputFile := filepath.Join(r.tmpFilePath, fmt.Sprintf("%s%s", r.id, outputEnvSuffix))
	cmdToExecute, err := r.getCmd(ctx, outputFile)
	if err != nil {
		r.log.Errorw("could not create run command", zap.Error(err))
		return nil, err
	}

	envVars, err := resolveExprInEnv(r.environment)
	if err != nil {
		return nil, err
	}

	cmdArgs := []string{"-c", cmdToExecute}

	cmd := r.cmdContextFactory.CmdContextWithSleep(ctx, cmdExitWaitTime, "sh", cmdArgs...).
		WithStdout(r.procWriter).WithStderr(r.procWriter).WithEnvVarsMap(envVars)
	err = runCmdFn(ctx, cmd, r.id, cmdArgs, retryCount, start, r.logMetrics, r.addonLogger)
	if err != nil {
		return nil, err
	}

	stepOutput := make(map[string]string)
	if len(r.envVarOutputs) != 0 {
		var err error
		outputVars, err := fetchOutputVariables(outputFile, r.fs, r.log)
		if err != nil {
			logCommandExecErr(r.log, "error encountered while fetching output of runtest step", r.id, cmdToExecute, retryCount, start, err)
			return nil, err
		}

		stepOutput = outputVars
	}

	r.addonLogger.Infow(
		"successfully executed run tests step",
		"arguments", cmdToExecute,
		"output", stepOutput,
		"elapsed_time_ms", utils.TimeSince(start),
	)
	return stepOutput, nil
}
