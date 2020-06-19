package main

/*
	CI lite engine executes steps of stage provided as an input.
*/
import (
	"github.com/alexflint/go-arg"
	"github.com/wings-software/portal/commons/go/lib/logs"
	"github.com/wings-software/portal/product/ci/engine/executor"
	"go.uber.org/zap"
)

const (
	applicationName = "CI-lite-engine"
	deployable      = "ci-lite-engine"
)

var args struct {
	// CLI flags,
	Stage   string `arg:"--stage, required" help:"stage in base64 format to execute"`
	LogPath string `arg:"--logpath, required" help:"relative file path to store logs of steps"`
	Verbose bool   `arg:"--verbose" help:"enable verbose logging mode"`

	Deployment            string `arg:"env:DEPLOYMENT" help:"name of the deployment"`
	DeploymentEnvironment string `arg:"env:DEPLOYMENT_ENVIRONMENT" help:"environment of the deployment"`
}

func parseArgs() {
	// set defaults here
	args.DeploymentEnvironment = "prod"
	args.Verbose = false

	arg.MustParse(&args)
}

func init() {
	//TODO: perform any initialization
}

func main() {
	parseArgs()

	// build initial log
	logger := logs.NewBuilder().Verbose(args.Verbose).WithDeployment(args.Deployment).
		WithFields("deployable", deployable,
			"application_name", applicationName).
		MustBuild().Sugar()

	logger.Infow("CI lite engine is starting")
	executor := executor.NewStageExecutor(args.Stage, args.LogPath, logger)
	if err := executor.Run(); err != nil {
		logger.Fatalw(
			"error while executing steps in a stage",
			"embedded_stage", args.Stage,
			"log_path", args.LogPath,
			zap.Error(err),
		)
	}

	logger.Infow("CI lite engine completed execution, now exiting")
}
