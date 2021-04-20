package main

/*
	scm service performs the following actions
*/
import (
	"github.com/alexflint/go-arg"
	"github.com/wings-software/portal/commons/go/lib/logs"
	"github.com/wings-software/portal/product/ci/scm/grpc"
	"go.uber.org/zap"
)

const (
	applicationName = "CI-scm"
	deployable      = "ci-scm"
	port            = 8091
)

var scmServer = grpc.NewSCMServer

var args struct {
	Verbose    bool   `arg:"--verbose" help:"enable verbose logging mode"`
	Port       uint   `arg:"--port" help:"port for running GRPC server"`
	UnixSocket string `arg:"--unix" help:"the unix socket to run on"`

	Deployment            string `arg:"env:DEPLOYMENT" help:"name of the deployment"`
	DeploymentEnvironment string `arg:"env:DEPLOYMENT_ENVIRONMENT" help:"environment of the deployment"`
}

func parseArgs() {
	// set defaults here
	args.DeploymentEnvironment = "prod"
	args.Port = port
	args.Verbose = false
	args.UnixSocket = ""

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

	logger.Infow("Starting CI GRPC scm server", "port", args.Port, "unixSocket", args.UnixSocket)
	s, err := scmServer(args.Port, args.UnixSocket, logger)
	if err != nil {
		logger.Fatalw("error while running CI GRPC scm server", "port", args.Port, "unixSocket", args.UnixSocket, zap.Error(err))
	}
	// Wait for stop signal and shutdown the server upon receiving it in a separate goroutine
	go s.Stop()
	s.Start()

}
