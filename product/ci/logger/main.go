// This package is used to wrap remote logging on top of commands to be executed.
// Currently, the use case for this is to get the logs of git clone command which
// runs inside an init container. When that is moved as a step to engine, this will not
// be needed.
package main

import (
	"context"
	"fmt"
	"os"
	"os/exec"

	"github.com/alexflint/go-arg"
	ciclient "github.com/wings-software/portal/product/log-service/client/ci"
	"github.com/wings-software/portal/commons/go/lib/logs"
	"go.uber.org/zap"
)

var args struct {
	AccountID string `arg:"env:HARNESS_ACCOUNT_ID, required" help:"Account ID"`
	OrgID     string `arg:"env:HARNESS_ORG_ID, required" help:"Org ID"`
	ProjectID string `arg:"env:HARNESS_PROJECT_ID, required" help:"Project ID"`
	BuildID   string `arg:"env:HARNESS_BUILD_ID, required" help:"Build ID"`
	StageID   string `arg:"env:HARNESS_STAGE_ID, required" help:"Stage ID"`
	StepID    string `arg:"env:HARNESS_STEP_ID, required" help:"Step ID"`
	Endpoint  string `arg:"env:LOG_SERVICE_ENDPOINT, required" help:"Log service endpoint"`
	Command   string `arg:"--command, required" help:"Command to execute"`
}

func parseArgs() {
	arg.MustParse(&args)
}

func init() {
	// TODO: perform any initialization
}

// Wrap command to be executed in logger
func main() {
	parseArgs()

	// Create remote logger for command execution
	key := fmt.Sprintf("%s/%s/%s/%s/%s/%s", args.AccountID, args.OrgID, args.ProjectID, args.BuildID, args.StageID, args.StepID)
	logClient := ciclient.NewHTTPClient(args.Endpoint, "", false)
	rl, err := logs.NewRemoteLogger(logClient, key)
	if err != nil {
		panic(err)
	}
	defer rl.Writer.Close()

	log := rl.BaseLogger

	// Execute the command
	cmd := exec.CommandContext(context.Background(), "sh", "-c", args.Command)
	cmd.Stdout = rl.Writer
	cmd.Stderr = rl.Writer
	if err := cmd.Start(); err != nil {
		log.Errorw("Could not start executing the command", "error_msg", zap.Error(err))
		rl.Writer.Close()
		os.Exit(1)
	}

	if err := cmd.Wait(); err != nil {
		log.Errorw("Errored out waiting for command to finish", "error_msg", zap.Error(err))
		rl.Writer.Close()
		os.Exit(1)
	}
}
