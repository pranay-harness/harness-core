package external

import (
	"fmt"
	"os"

	"github.com/wings-software/portal/commons/go/lib/logs"
	ticlient "github.com/wings-software/portal/product/ci/ti-service/client"
	"github.com/wings-software/portal/product/log-service/client"
)

const (
	accountIDEnv  = "HARNESS_ACCOUNT_ID"
	orgIDEnv      = "HARNESS_ORG_ID"
	projectIDEnv  = "HARNESS_PROJECT_ID"
	buildIDEnv    = "HARNESS_BUILD_ID"
	stageIDEnv    = "HARNESS_STAGE_ID"
	pipelineIDEnv = "HARNESS_PIPELINE_ID"
	tiSvcEp       = "HARNESS_TI_SERVICE_ENDPOINT"
	tiSvcToken    = "HARNESS_TI_SERVICE_TOKEN"
	logSvcEp      = "HARNESS_LOG_SERVICE_ENDPOINT"
	logSvcToken   = "HARNESS_LOG_SERVICE_TOKEN"
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
	account, err := GetAccountId()
	if err != nil {
		return nil, err
	}
	token, ok := os.LookupEnv(logSvcToken)
	if !ok {
		return nil, fmt.Errorf("log service token not set %s", logSvcToken)
	}
	return client.NewHTTPClient(l, account, token, false), nil
}

// GetLogKey returns a stringified key for log service using various identifiers
func GetLogKey(stepID string) (string, error) {
	account, err := GetAccountId()
	if err != nil {
		return "", err
	}
	org, err := GetOrgId()
	if err != nil {
		return "", err
	}
	project, err := GetProjectId()
	if err != nil {
		return "", err
	}
	pipeline, err := GetPipelineId()
	if err != nil {
		return "", err
	}
	build, err := GetBuildId()
	if err != nil {
		return "", err
	}
	stage, err := GetStageId()
	if err != nil {
		return "", err
	}
	key := fmt.Sprintf("%s/%s/%s/%s/%s/%s/%s", account, org, project, pipeline, build, stage, stepID)
	return key, nil
}

// GetTiHTTPClient returns a client to talk to the TI service
func GetTiHTTPClient() (ticlient.Client, error) {
	l, ok := os.LookupEnv(tiSvcEp)
	if !ok {
		return nil, fmt.Errorf("ti service endpoint variable not set %s", tiSvcEp)
	}
	account, err := GetAccountId()
	if err != nil {
		return nil, err
	}
	token, ok := os.LookupEnv(tiSvcToken)
	if !ok {
		return nil, fmt.Errorf("TI service token not set %s", tiSvcToken)
	}
	return ticlient.NewHTTPClient(l, account, token, false), nil
}

func GetAccountId() (string, error) {
	account, ok := os.LookupEnv(accountIDEnv)
	if !ok {
		return "", fmt.Errorf("account ID environment variable not set %s", accountIDEnv)
	}
	return account, nil
}

func GetOrgId() (string, error) {
	org, ok := os.LookupEnv(orgIDEnv)
	if !ok {
		return "", fmt.Errorf("org ID environment variable not set %s", orgIDEnv)
	}
	return org, nil
}

func GetProjectId() (string, error) {
	project, ok := os.LookupEnv(projectIDEnv)
	if !ok {
		return "", fmt.Errorf("project ID environment variable not set %s", projectIDEnv)
	}
	return project, nil
}

func GetPipelineId() (string, error) {
	pipeline, ok := os.LookupEnv(pipelineIDEnv)
	if !ok {
		return "", fmt.Errorf("pipeline ID environment variable not set %s", pipelineIDEnv)
	}
	return pipeline, nil
}

func GetBuildId() (string, error) {
	build, ok := os.LookupEnv(buildIDEnv)
	if !ok {
		return "", fmt.Errorf("build ID environment variable not set %s", buildIDEnv)
	}
	return build, nil
}

func GetStageId() (string, error) {
	stage, ok := os.LookupEnv(stageIDEnv)
	if !ok {
		return "", fmt.Errorf("stage ID environment variable not set %s", stageIDEnv)
	}
	return stage, nil
}
