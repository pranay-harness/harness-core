package logger

import (
	"fmt"
	"os"

	ciclient "github.com/wings-software/log-service/client/ci"
	"github.com/wings-software/portal/commons/go/lib/logs"
)

const (
	accountIDEnv       = "HARNESS_ACCOUNT_ID"
	orgIDEnv           = "HARNESS_ORG_ID"
	projectIDEnv       = "HARNESS_PROJECT_ID"
	buildIDEnv         = "HARNESS_BUILD_ID"
	stageIDEnv         = "HARNESS_STAGE_ID"
	logServiceEndpoint = "LOG_SERVICE_ENDPOINT"
)

// GetRemoteLogger returns a logger which can talk to the log service with the corresponding step key.
func GetRemoteLogger(stepID string) (*logs.RemoteLogger, error) {
	l, ok := os.LookupEnv(logServiceEndpoint)
	if !ok {
		return nil, fmt.Errorf(fmt.Sprintf("log service endpoint variable not set %s", logServiceEndpoint))
	}
	logClient := ciclient.NewHTTPClient(l, "", false)
	account, ok := os.LookupEnv(accountIDEnv)
	if !ok {
		return nil, fmt.Errorf(fmt.Sprintf("account ID endpoint variable not set %s", accountIDEnv))
	}
	org, ok := os.LookupEnv(orgIDEnv)
	if !ok {
		return nil, fmt.Errorf(fmt.Sprintf("project ID endpoint variable not set %s", orgIDEnv))
	}
	project, ok := os.LookupEnv(projectIDEnv)
	if !ok {
		return nil, fmt.Errorf(fmt.Sprintf("project ID endpoint variable not set %s", projectIDEnv))
	}
	build, ok := os.LookupEnv(buildIDEnv)
	if !ok {
		return nil, fmt.Errorf(fmt.Sprintf("build ID endpoint variable not set %s", buildIDEnv))
	}
	stage, ok := os.LookupEnv(stageIDEnv)
	if !ok {
		return nil, fmt.Errorf(fmt.Sprintf("stage ID endpoint variable not set %s", stageIDEnv))
	}
	key := fmt.Sprintf("%s/%s/%s/%s/%s/%s", account, org, project, build, stage, stepID)
	rl, err := logs.NewRemoteLogger(logClient, key)
	if err != nil {
		return nil, err
	}
	return rl, nil
}
