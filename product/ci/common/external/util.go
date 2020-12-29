package external

import (
	"fmt"
	"os"

	"github.com/wings-software/portal/commons/go/lib/logs"
	ticlient "github.com/wings-software/portal/product/ci/ti-service/client"
	"github.com/wings-software/portal/product/log-service/client"
)

const (
	accountIDEnv = "HARNESS_ACCOUNT_ID"
	orgIDEnv     = "HARNESS_ORG_ID"
	projectIDEnv = "HARNESS_PROJECT_ID"
	buildIDEnv   = "HARNESS_BUILD_ID"
	stageIDEnv   = "HARNESS_STAGE_ID"
	tiSvcEp      = "HARNESS_TI_SERVICE_ENDPOINT"
	logSvcEp     = "HARNESS_LOG_SERVICE_ENDPOINT"
	logSvcToken  = "HARNESS_LOG_SERVICE_TOKEN"
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

// GetRemoteHttpClient returns a new HTTP client to talk to log service using information available in env.
func GetRemoteHTTPClient() (client.Client, error) {
	l, ok := os.LookupEnv(logSvcEp)
	if !ok {
		return nil, fmt.Errorf("log service endpoint variable not set %s", logSvcEp)
	}
	account, ok := os.LookupEnv(accountIDEnv)
	if !ok {
		return nil, fmt.Errorf("account ID endpoint variable not set %s", accountIDEnv)
	}
	token, ok := os.LookupEnv(logSvcToken)
	if !ok {
		return nil, fmt.Errorf("log service token not set %s", logSvcToken)
	}
	return client.NewHTTPClient(l, account, token, false), nil
}

// GetLogKey returns a stringified key for log service using various identifiers
func GetLogKey(stepID string) (string, error) {
	account, ok := os.LookupEnv(accountIDEnv)
	if !ok {
		return "", fmt.Errorf("account ID endpoint variable not set %s", accountIDEnv)
	}
	org, ok := os.LookupEnv(orgIDEnv)
	if !ok {
		return "", fmt.Errorf("project ID endpoint variable not set %s", orgIDEnv)
	}
	project, ok := os.LookupEnv(projectIDEnv)
	if !ok {
		return "", fmt.Errorf("project ID endpoint variable not set %s", projectIDEnv)
	}
	build, ok := os.LookupEnv(buildIDEnv)
	if !ok {
		return "", fmt.Errorf("build ID endpoint variable not set %s", buildIDEnv)
	}
	stage, ok := os.LookupEnv(stageIDEnv)
	if !ok {
		return "", fmt.Errorf("stage ID endpoint variable not set %s", stageIDEnv)
	}
	key := fmt.Sprintf("%s/%s/%s/%s/%s/%s", account, org, project, build, stage, stepID)
	return key, nil
}

// GetTiHTTPClient returns a client to talk to the TI service
func GetTiHTTPClient() (ticlient.Client, error) {
	l, ok := os.LookupEnv(tiSvcEp)
	if !ok {
		return nil, fmt.Errorf("ti service endpoint variable not set %s", tiSvcEp)
	}
	account, ok := os.LookupEnv(accountIDEnv)
	if !ok {
		return nil, fmt.Errorf("account ID endpoint variable not set %s", accountIDEnv)
	}
	return ticlient.NewHTTPClient(l, account, false), nil
}

func GetAccountId() (string, error) {
	account, ok := os.LookupEnv(accountIDEnv)
	if !ok {
		return "", fmt.Errorf("account ID endpoint variable not set %s", accountIDEnv)
	}
	return account, nil
}

func GetOrgId() (string, error) {
	org, ok := os.LookupEnv(orgIDEnv)
	if !ok {
		return "", fmt.Errorf("project ID endpoint variable not set %s", orgIDEnv)
	}
	return org, nil
}

func GetProjectId() (string, error) {
	project, ok := os.LookupEnv(projectIDEnv)
	if !ok {
		return "", fmt.Errorf("project ID endpoint variable not set %s", projectIDEnv)
	}
	return project, nil
}

func GetBuildId() (string, error) {
	build, ok := os.LookupEnv(buildIDEnv)
	if !ok {
		return "", fmt.Errorf("build ID endpoint variable not set %s", buildIDEnv)
	}
	return build, nil
}

func GetStageId() (string, error) {
	stage, ok := os.LookupEnv(stageIDEnv)
	if !ok {
		return "", fmt.Errorf("stage ID endpoint variable not set %s", stageIDEnv)
	}
	return stage, nil
}
