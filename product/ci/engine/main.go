package main

/*
	CI lite engine executes steps of stage provided as an input.
*/
import (
	"fmt"
	"os"
	"strconv"

	"github.com/alexflint/go-arg"
	"github.com/wings-software/portal/product/ci/engine/executor"
	"github.com/wings-software/portal/product/ci/engine/grpc"
	logger "github.com/wings-software/portal/product/ci/logger/util"
)

const (
	applicationName = "CI-lite-engine"
	deployable      = "ci-lite-engine"
)

var (
	executeStage     = executor.ExecuteStage
	executeStep      = executor.ExecuteStep
	liteEngineServer = grpc.NewLiteEngineServer
	newRemoteLogger  = logger.GetRemoteLogger
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

	// Build initial log
	// Lite engine logs that are not part of any step are logged with ID engine_state_logs-engineID
	engineID := "main"
	if args.Server != nil {
		engineID = strconv.Itoa(int(args.Server.Port))
	}
	key := fmt.Sprintf("engine_stage_logs-%s", engineID)
	remoteLogger, err := newRemoteLogger(key)
	if err != nil {
		// Could not create a logger
		panic(err)
	}
	log := remoteLogger.BaseLogger
	defer remoteLogger.Writer.Close() // upload the logs to object store and close the stream

	log.Infow("CI lite engine is starting")

	switch {
	case args.Stage != nil:
		err := executeStage(args.Stage.Input, args.Stage.LogPath, args.Stage.TmpFilePath, args.Stage.WorkerPorts, args.Stage.Debug, log)
		if err != nil {
			remoteLogger.Writer.Close()
			os.Exit(1) // Exit the lite engine with status code of 1
		}
	case args.Step != nil:
		err := executeStep(args.Step.Input, args.Step.LogPath, args.Step.TmpFilePath, log)
		if err != nil {
			remoteLogger.Writer.Close()
			os.Exit(1) // Exit the lite engine with status code of 1
		}
	case args.Server != nil:
		s, err := liteEngineServer(args.Server.Port, args.Server.LogPath, args.Server.TmpFilePath, log)
		if err != nil {
			remoteLogger.Writer.Close()
			os.Exit(1) // Exit the lite engine with status code of 1
		}

		// Wait for stop signal and shutdown the server upon receiving it in a separate goroutine
		go s.Stop()
		if err := s.Start(); err != nil {
			remoteLogger.Writer.Close()
			os.Exit(1) // Exit the lite engine with status code of 1
		}
	default:
		log.Errorw(
			"One of stage or step needs to be specified",
			"args", args,
		)
		remoteLogger.Writer.Close()
		os.Exit(1) // Exit the lite engine with status code of 1
	}
	log.Infow("CI lite engine completed execution, now exiting")
}
