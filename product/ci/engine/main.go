package main

/*
	CI lite engine executes steps of stage provided as an input.
*/
import (
	"github.com/alexflint/go-arg"
	"github.com/wings-software/portal/commons/go/lib/logs"
	"github.com/wings-software/portal/product/ci/common/external"
	"github.com/wings-software/portal/product/ci/engine/consts"
	"github.com/wings-software/portal/product/ci/engine/executor"
	"github.com/wings-software/portal/product/ci/engine/grpc"
	"go.uber.org/zap"
	"os"
)

const (
	applicationName = "CI-lite-engine"
	deployable      = "ci-lite-engine"
)

var (
	executeStage        = executor.ExecuteStage
	newHTTPRemoteLogger = external.GetHTTPRemoteLogger
	engineServer        = grpc.NewEngineServer
)

// schema for executing a stage
type stageSchema struct {
	Input        string `arg:"--input, required" help:"base64 format of stage to execute"`
	TmpFilePath  string `arg:"--tmppath, required" help:"relative file path to store temporary files"`
	ServicePorts []uint `arg:"--svc_ports" help:"grpc service ports of integration service containers"`
	Debug        bool   `arg:"--debug" help:"Enables debug mode for checking run step logs by not exitting CI-addon"`
}

var args struct {
	Stage *stageSchema `arg:"subcommand:stage"`

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

	// Lite engine logs that are not part of any step are logged with ID engine_state_logs-main
	key := "engine_stage_logs-main"
	remoteLogger, err := newHTTPRemoteLogger(key)
	if err != nil {
		// Could not create a logger
		panic(err)
	}
	log := remoteLogger.BaseLogger
	defer remoteLogger.Writer.Close() // upload the logs to object store and close the stream

	startServer(remoteLogger)

	log.Infow("Starting stage execution")
	switch {
	case args.Stage != nil:
		err := executeStage(args.Stage.Input, args.Stage.TmpFilePath, args.Stage.ServicePorts, args.Stage.Debug, log)
		if err != nil {
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

// starts grpc server in background
func startServer(rl *logs.RemoteLogger) {
	log := rl.BaseLogger

	log.Infow("Starting CI engine server", "port", consts.LiteEnginePort)
	s, err := engineServer(consts.LiteEnginePort, log)
	if err != nil {
		log.Errorw("error on running CI engine server", "port", consts.LiteEnginePort, "error_msg", zap.Error(err))
		rl.Writer.Close()
		os.Exit(1) // Exit engine with exit code 1
	}

	// Start grpc server in separate goroutine. It will cater to pausing/resuming stage execution.
	go func() {
		if err := s.Start(); err != nil {
			log.Errorw("error in CI engine grpc server", "port", consts.LiteEnginePort, "error_msg", zap.Error(err))
		}
	}()
}
