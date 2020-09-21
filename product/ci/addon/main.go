package main

/*
	CI-addon performs the following actions
		1) uploads the artifacts to customer desired location
		2) updates the configuration files to reflect the new version of the artifact the CI pipeline built.
		3) streams data, metrics through a stream-processing service(to be decided)
*/
import (
	"os"

	"github.com/alexflint/go-arg"
	"github.com/wings-software/portal/product/ci/addon/grpc"
	logger "github.com/wings-software/portal/product/ci/logger/util"
	"go.uber.org/zap"
)

const (
	applicationName = "CI-addon"
	deployable      = "ci-addon"
	port            = 8001
)

var (
	addonServer     = grpc.NewAddonServer
	newRemoteLogger = logger.GetRemoteLogger
)

var args struct {
	Verbose bool `arg:"--verbose" help:"enable verbose logging mode"`
	Port    uint `arg:"--port" help:"port for running GRPC server"`

	Deployment            string `arg:"env:DEPLOYMENT" help:"name of the deployment"`
	DeploymentEnvironment string `arg:"env:DEPLOYMENT_ENVIRONMENT" help:"environment of the deployment"`
}

func parseArgs() {
	// set defaults here
	args.DeploymentEnvironment = "prod"
	args.Port = port
	args.Verbose = false

	arg.MustParse(&args)
}

func init() {
	//TODO: perform any initialization
}

func main() {
	parseArgs()

	// Build initial log
	// Addon logs not part of a step go to addon_stage_logs
	key := "addon_stage_logs"
	remoteLogger, err := newRemoteLogger(key)
	if err != nil {
		// Could not create a logger
		panic(err)
	}
	log := remoteLogger.BaseLogger
	defer remoteLogger.Writer.Close() // upload the logs to object storage and close the log stream

	log.Infow("Starting CI addon server", "port", args.Port)
	s, err := addonServer(args.Port, log)
	if err != nil {
		log.Errorw("error while running CI addon server", "port", args.Port, "error_msg", zap.Error(err))
		remoteLogger.Writer.Close()
		os.Exit(1) // Exit addon with exit code 1
	}

	// Wait for stop signal and shutdown the server upon receiving it in a separate goroutine
	go s.Stop()
	if err := s.Start(); err != nil {
		remoteLogger.Writer.Close()
		os.Exit(1) // Exit addon with exit code 1
	}
}
