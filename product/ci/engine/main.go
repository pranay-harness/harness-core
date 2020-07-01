package main

/*
	CI lite engine executes steps of stage provided as an input.
*/
import (
	"github.com/alexflint/go-arg"
	"github.com/wings-software/portal/commons/go/lib/logs"
	"github.com/wings-software/portal/product/ci/engine/executor"
)

const (
	applicationName = "CI-lite-engine"
	deployable      = "ci-lite-engine"
)

type schema struct {
	Input       string `arg:"--input, required" help:"base64 format of stage/step to execute"`
	LogPath     string `arg:"--logpath, required" help:"relative file path to store logs of steps"`
	TmpFilePath string `arg:"--tmppath, required" help:"relative file path to store temporary files"`
}

var args struct {
	Stage *schema `arg:"subcommand:stage"`
	Step  *schema `arg:"subcommand:step"`

	Verbose               bool   `arg:"--verbose" help:"enable verbose logging mode"`
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
	log := logs.NewBuilder().Verbose(args.Verbose).WithDeployment(args.Deployment).
		WithFields("deployable", deployable,
			"application_name", applicationName).
		MustBuild().Sugar()

	log.Infow("CI lite engine is starting")
	switch {
	case args.Stage != nil:
		executor.ExecuteStage(args.Stage.Input, args.Stage.LogPath, args.Stage.TmpFilePath, log)
	case args.Step != nil:
		executor.ExecuteStep(args.Step.Input, args.Step.LogPath, args.Step.TmpFilePath, log)
	default:
		log.Fatalw(
			"One of stage or step needs to be specified",
			"args", args,
		)
	}
	log.Infow("CI lite engine completed execution, now exiting")
}
