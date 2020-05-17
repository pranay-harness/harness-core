package main

/*
	CI-addon performs the following actions
		1) uploads the artifacts to customer desired location
		2) updates the configuration files to reflect the new version of the artifact the CI pipeline built.
		3) streams data, metrics through a stream-processing service(to be decided)
*/
import (
	"encoding/json"
	"github.com/alexflint/go-arg"
	"github.com/pkg/errors"
	"github.com/wings-software/portal/commons/go/lib/logs"
	"github.com/wings-software/portal/commons/go/lib/utils"
	"go.uber.org/zap"
)

const (
	applicationName = "CI-addon"
	deployable      = "ci-addon"
)

var args struct {
	// CLI flags,
	TaskID  string `arg:"--task_id, required" help:"ID of the task this addon is running"`
	Verbose bool   `arg:"--verbose" help:"enable verbose logging mode"`
	Linger  bool   `arg:"--linger" help:"don't exit ci-addon for debugging purpose"`

	OptionsJSON           string `arg:"--options_json" help:"JSON object full of arbitrary configuration values"`
	Deployment            string `arg:"env:DEPLOYMENT" help:"name of the deployment"`
	DeploymentEnvironment string `arg:"env:DEPLOYMENT_ENVIRONMENT" help:"environment of the deployment"`
}

func parseArgs() {
	// set defaults here
	args.DeploymentEnvironment = "prod"
	args.OptionsJSON = "{}"
	args.Linger = false

	arg.MustParse(&args)
}

func init() {
	//TODO: perform any initialization
}

func main() {
	parseArgs()

	// build initial log
	logger := logs.NewBuilder().WithDeployment(args.Deployment).
		WithFields("task_id", args.TaskID,
			"deployable", deployable,
			"application_name", applicationName).
		MustBuild().Sugar()

	logger.Infow("ci-addon is starting")
	options, err := initOptions()
	if err != nil {
		logger.Fatalw("Failed to initialize options from --options_json value", "data", args.OptionsJSON, zap.Error(err))
	}

	logger.Infow("options:", "key:timeout", options.GetString("timeout")) //TODO: remove this when we start using options

	if args.Linger {
		logger.Infow("Linger flag set, ci-addon will run forever, even though normally we would exit now")
		select {}
	}
	logger.Infow("ci-addon completed uploading, now exiting")
}

func initOptions() (utils.KVMap, error) {
	var options utils.KVMap

	err := json.Unmarshal([]byte(args.OptionsJSON), &options)
	if err != nil {
		return nil, errors.Wrap(err, "failed to unmarshall options string as JSON")
	}
	return options, nil
}
