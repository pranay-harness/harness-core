package main

/*
	CI-addon is an entrypoint for run step & plugin step container images. It executes a step on receiving GRPC.
*/
import (
	"fmt"
	"os"

	"github.com/alexflint/go-arg"
	"github.com/wings-software/portal/product/ci/addon/grpc"
	"github.com/wings-software/portal/product/ci/addon/services"
	logger "github.com/wings-software/portal/product/ci/logger/util"
	"go.uber.org/zap"
)

const (
	applicationName = "CI-addon"
	deployable      = "ci-addon"
)

var (
	addonServer       = grpc.NewAddonServer
	newRemoteLogger   = logger.GetRemoteLogger
	newIntegrationSvc = services.NewIntegrationSvc
)

// schema for running functional test service
type service struct {
	ID         string   `arg:"--id, required" help:"Service ID"`
	Image      string   `arg:"--image, required" help:"docker image name for the service"`
	Entrypoint []string `arg:"--entrypoint" help:"entrypoint for the service"`
	Args       []string `arg:"--args" help:"arguments for the service"`
}

var args struct {
	Service *service `arg:"subcommand:service" help:"integration service arguments"`

	Port                  uint   `arg:"--port, required" help:"port for running GRPC server"`
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

	// Addon logs not part of a step go to addon_stage_logs-<port>
	key := fmt.Sprintf("addon_stage_logs-%d", args.Port)
	remoteLogger, err := newRemoteLogger(key)
	if err != nil {
		// Could not create a logger
		panic(err)
	}
	log := remoteLogger.BaseLogger
	defer remoteLogger.Writer.Close() // upload the logs to object storage and close the log stream

	// Start integration test service in a separate goroutine
	if args.Service != nil {
		svc := args.Service

		// create logger for service logs
		rl, err := newRemoteLogger(svc.ID)
		if err != nil {
			panic(err) // Could not create a logger
		}
		defer rl.Writer.Close() // upload the service logs to object storage and close the log stream

		go func() {
			newIntegrationSvc(svc.ID, svc.Image, svc.Entrypoint, svc.Args, rl.BaseLogger, rl.Writer).Run()
		}()
	}

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
