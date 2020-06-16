package main

/*
	CI-addon performs the following actions
		1) uploads the artifacts to customer desired location
		2) updates the configuration files to reflect the new version of the artifact the CI pipeline built.
		3) streams data, metrics through a stream-processing service(to be decided)
*/
import (
	"github.com/alexflint/go-arg"
	"github.com/wings-software/portal/commons/go/lib/logs"
	"github.com/wings-software/portal/product/ci/addon/grpc"
	"go.uber.org/zap"
)

const (
	applicationName = "CI-addon"
	deployable      = "ci-addon"
	port            = 8001
)

var ciAddonServer = grpc.NewCIAddonServer

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

	// build initial log
	logBuilder := logs.NewBuilder().Verbose(args.Verbose).WithDeployment(args.Deployment).
		WithFields("deployable", deployable,
			"application_name", applicationName)
	logger := logBuilder.MustBuild().Sugar()

	logger.Infow("Starting CI addon server", "port", args.Port)
	s, err := ciAddonServer(args.Port, logger)
	if err != nil {
		logger.Fatalw("error while running CI addon server", "port", args.Port, zap.Error(err))
	}
	s.Start()
}
