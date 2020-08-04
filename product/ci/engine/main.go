package main

/*
	CI lite engine executes steps of stage provided as an input.
*/
import (
	"github.com/alexflint/go-arg"
	"github.com/wings-software/portal/commons/go/lib/logs"
	"github.com/wings-software/portal/product/ci/engine/executor"
	"github.com/wings-software/portal/product/ci/engine/grpc"
	"go.uber.org/zap"
)

const (
	applicationName = "CI-lite-engine"
	deployable      = "ci-lite-engine"
)

var (
	executeStage     = executor.ExecuteStage
	liteEngineServer = grpc.NewLiteEngineServer
)

// schema for executing a stage
type stageSchema struct {
	Input       string `arg:"--input, required" help:"base64 format of stage to execute"`
	LogPath     string `arg:"--logpath, required" help:"relative file path to store logs of steps"`
	TmpFilePath string `arg:"--tmppath, required" help:"relative file path to store temporary files"`
	WorkerPorts []uint `arg:"--ports" help:"list of grpc server ports for worker lite engines"`
	Debug       bool   `arg:"--debug" help:"Enables debug mode for checking run step logs by not exitting CI-addon"`
}

// schema for executing a unit step
type stepSchema struct {
	Input       string `arg:"--input, required" help:"base64 format of unit step to execute"`
	LogPath     string `arg:"--logpath, required" help:"relative file path to store logs of steps"`
	TmpFilePath string `arg:"--tmppath, required" help:"relative file path to store temporary files"`
}

// schema for running GRPC server
type grpcSchema struct {
	Port        uint   `arg:"--port, required" help:"port to run grpc server"`
	LogPath     string `arg:"--logpath, required" help:"relative file path to store logs of steps"`
	TmpFilePath string `arg:"--tmppath, required" help:"relative file path to store temporary files"`
}

var args struct {
	Stage  *stageSchema `arg:"subcommand:stage"`
	Step   *stepSchema  `arg:"subcommand:step"`
	Server *grpcSchema  `arg:"subcommand:server"`

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
		executeStage(args.Stage.Input, args.Stage.LogPath, args.Stage.TmpFilePath, args.Stage.WorkerPorts, args.Stage.Debug, log)
	case args.Step != nil:
		executor.ExecuteStep(args.Step.Input, args.Step.LogPath, args.Step.TmpFilePath, log)
	case args.Server != nil:
		s, err := liteEngineServer(args.Server.Port, args.Server.LogPath, args.Server.TmpFilePath, log)
		if err != nil {
			log.Fatalw("error while running CI lite engine server", "port", args.Server.Port, zap.Error(err))
		}

		// Wait for stop signal and shutdown the server upon receiving it in a separate goroutine
		go s.Stop()
		s.Start()
	default:
		log.Fatalw(
			"One of stage or step needs to be specified",
			"args", args,
		)
	}
	log.Infow("CI lite engine completed execution, now exiting")
}
