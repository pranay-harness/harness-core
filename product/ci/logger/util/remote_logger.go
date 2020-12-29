package logger

import (
	"fmt"
	"os"

	"github.com/wings-software/portal/commons/go/lib/logs"
	"github.com/wings-software/portal/product/log-service/client"
)

const (
	accountIDEnv       = "HARNESS_ACCOUNT_ID"
	orgIDEnv           = "HARNESS_ORG_ID"
	projectIDEnv       = "HARNESS_PROJECT_ID"
	buildIDEnv         = "HARNESS_BUILD_ID"
	stageIDEnv         = "HARNESS_STAGE_ID"
	logServiceEndpoint = "HARNESS_LOG_SERVICE_ENDPOINT"
	logServiceToken    = "HARNESS_LOG_SERVICE_TOKEN"
	logServicePort     = 8079
)

func GetHTTPRemoteLogger(stepID string) (*logs.RemoteLogger, error) {
	key, err := GetLogKey(stepID)
	if err != nil {
		return nil, err
	}
	client, err := GetRemoteHTTPClient()
	if err != nil {
		return nil, err
	}
	writer, err := logs.NewRemoteWriter(client, key)
	if err != nil {
		return nil, err
	}
	rl, err := logs.NewRemoteLogger(writer)
	if err != nil {
		return nil, err
	}
	return rl, nil
}

// GetRemoteClient returns a new HTTP client to talk to log service using information available in env.
func GetRemoteHTTPClient() (client.Client, error) {
	l, ok := os.LookupEnv(logServiceEndpoint)
	if !ok {
		return nil, fmt.Errorf(fmt.Sprintf("log service endpoint variable not set %s", logServiceEndpoint))
	}
	account, ok := os.LookupEnv(accountIDEnv)
	if !ok {
		return nil, fmt.Errorf(fmt.Sprintf("account ID endpoint variable not set %s", accountIDEnv))
	}
	token, ok := os.LookupEnv(logServiceToken)
	if !ok {
		return nil, fmt.Errorf(fmt.Sprintf("log service token not set %s", logServiceToken))
	}
	return client.NewHTTPClient(l, account, token, false), nil
}

func GetLogKey(stepID string) (string, error) {
	account, ok := os.LookupEnv(accountIDEnv)
	if !ok {
		return "", fmt.Errorf(fmt.Sprintf("account ID endpoint variable not set %s", accountIDEnv))
	}
	org, ok := os.LookupEnv(orgIDEnv)
	if !ok {
		return "", fmt.Errorf(fmt.Sprintf("project ID endpoint variable not set %s", orgIDEnv))
	}
	project, ok := os.LookupEnv(projectIDEnv)
	if !ok {
		return "", fmt.Errorf(fmt.Sprintf("project ID endpoint variable not set %s", projectIDEnv))
	}
	build, ok := os.LookupEnv(buildIDEnv)
	if !ok {
		return "", fmt.Errorf(fmt.Sprintf("build ID endpoint variable not set %s", buildIDEnv))
	}
	stage, ok := os.LookupEnv(stageIDEnv)
	if !ok {
		return "", fmt.Errorf(fmt.Sprintf("stage ID endpoint variable not set %s", stageIDEnv))
	}
	key := fmt.Sprintf("%s/%s/%s/%s/%s/%s", account, org, project, build, stage, stepID)
	return key, nil
}
