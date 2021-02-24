package external

import (
	"context"
	"fmt"
	"os"
	"strings"

	"github.com/wings-software/portal/commons/go/lib/exec"
	"github.com/wings-software/portal/commons/go/lib/logs"
	ticlient "github.com/wings-software/portal/product/ci/ti-service/client"
	"github.com/wings-software/portal/product/log-service/client"
	"go.uber.org/zap"
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
	secretList    = "HARNESS_SECRETS_LIST"
	dSourceBranch = "DRONE_SOURCE_BRANCH"
	dRemoteUrl    = "DRONE_REMOTE_URL"
	dCommitSha    = "DRONE_COMMIT_SHA"
	wrkspcPath    = "HARNESS_WORKSPACE"
	gitBinPath    = "HARNESS_GIT_BINARY_PATH"
	diffFilesCmd  = "%s diff --name-only HEAD HEAD@{1} -1"
)

// GetChangedFiles executes a shell command and retuns list of files changed in PR
func GetChangedFiles(ctx context.Context, gitPath, workspace string, log *zap.SugaredLogger) ([]string, error) {
	cmdContextFactory := exec.OsCommandContextGracefulWithLog(log)
	cmd := cmdContextFactory.CmdContext(ctx, "sh", "-c", fmt.Sprintf(diffFilesCmd, gitPath)).WithDir(workspace)
	out, err := cmd.Output()

	if err != nil {
		return nil, err
	}
	return strings.Split(string(out), "\n"), nil
}

func GetSecrets() []logs.Secret {
	res := []logs.Secret{}
	secrets := os.Getenv(secretList)
	if secrets == "" {
		return res
	}
	secretList := strings.Split(secrets, ",")
	for _, skey := range secretList {
		sval := os.Getenv(skey)
		if sval == "" {
			fmt.Printf("could not find secret env variable for: %s\n", skey)
			continue
		}
		// Mask all the secrets for now
		res = append(res, logs.NewSecret(skey, sval, true))
	}
	return res
}

func GetHTTPRemoteLogger(stepID string) (*logs.RemoteLogger, error) {
	key, err := GetLogKey(stepID)
	if err != nil {
		return nil, err
	}
	client, err := GetRemoteHTTPClient()
	if err != nil {
		return nil, err
	}
	rw, err := logs.NewRemoteWriter(client, key)
	if err != nil {
		return nil, err
	}
	rws := logs.NewReplacer(rw, GetSecrets()) // Remote writer with secrets masked
	rl, err := logs.NewRemoteLogger(rws)
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

func GetSourceBranch() (string, error) {
	stage, ok := os.LookupEnv(dSourceBranch)
	if !ok {
		return "", fmt.Errorf("source branch variable not set %s", dSourceBranch)
	}
	return stage, nil
}

func GetRepo() (string, error) {
	stage, ok := os.LookupEnv(dRemoteUrl)
	if !ok {
		return "", fmt.Errorf("remote url variable not set %s", dRemoteUrl)
	}
	return stage, nil
}

func GetSha() (string, error) {
	stage, ok := os.LookupEnv(dCommitSha)
	if !ok {
		return "", fmt.Errorf("commit sha variable not set %s", dCommitSha)
	}
	return stage, nil
}

func GetWrkspcPath() (string, error) {
	path, ok := os.LookupEnv(wrkspcPath)
	if !ok {
		return "", fmt.Errorf("workspace path variable not set %s", wrkspcPath)
	}
	return path, nil
}

func GetGitBinPath() (string, error) {
	path, ok := os.LookupEnv(gitBinPath)
	if !ok {
		return "", fmt.Errorf("git binary path variable not set %s", gitBinPath)
	}
	return path, nil
}
