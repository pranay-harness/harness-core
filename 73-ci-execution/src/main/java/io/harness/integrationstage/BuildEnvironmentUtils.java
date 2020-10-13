package io.harness.integrationstage;

import static io.harness.common.BuildEnvironmentConstants.DRONE_BRANCH;
import static io.harness.common.BuildEnvironmentConstants.DRONE_BUILD_ACTION;
import static io.harness.common.BuildEnvironmentConstants.DRONE_BUILD_EVENT;
import static io.harness.common.BuildEnvironmentConstants.DRONE_BUILD_NUMBER;
import static io.harness.common.BuildEnvironmentConstants.DRONE_COMMIT;
import static io.harness.common.BuildEnvironmentConstants.DRONE_COMMIT_AFTER;
import static io.harness.common.BuildEnvironmentConstants.DRONE_COMMIT_AUTHOR;
import static io.harness.common.BuildEnvironmentConstants.DRONE_COMMIT_AUTHOR_AVATAR;
import static io.harness.common.BuildEnvironmentConstants.DRONE_COMMIT_AUTHOR_EMAIL;
import static io.harness.common.BuildEnvironmentConstants.DRONE_COMMIT_AUTHOR_NAME;
import static io.harness.common.BuildEnvironmentConstants.DRONE_COMMIT_BEFORE;
import static io.harness.common.BuildEnvironmentConstants.DRONE_COMMIT_BRANCH;
import static io.harness.common.BuildEnvironmentConstants.DRONE_COMMIT_LINK;
import static io.harness.common.BuildEnvironmentConstants.DRONE_COMMIT_MESSAGE;
import static io.harness.common.BuildEnvironmentConstants.DRONE_COMMIT_REF;
import static io.harness.common.BuildEnvironmentConstants.DRONE_COMMIT_SHA;
import static io.harness.common.BuildEnvironmentConstants.DRONE_GIT_HTTP_URL;
import static io.harness.common.BuildEnvironmentConstants.DRONE_GIT_SSH_URL;
import static io.harness.common.BuildEnvironmentConstants.DRONE_REMOTE_URL;
import static io.harness.common.BuildEnvironmentConstants.DRONE_REPO;
import static io.harness.common.BuildEnvironmentConstants.DRONE_REPO_BRANCH;
import static io.harness.common.BuildEnvironmentConstants.DRONE_REPO_LINK;
import static io.harness.common.BuildEnvironmentConstants.DRONE_REPO_NAME;
import static io.harness.common.BuildEnvironmentConstants.DRONE_REPO_NAMESPACE;
import static io.harness.common.BuildEnvironmentConstants.DRONE_REPO_OWNER;
import static io.harness.common.BuildEnvironmentConstants.DRONE_REPO_PRIVATE;
import static io.harness.common.BuildEnvironmentConstants.DRONE_REPO_SCM;
import static io.harness.common.BuildEnvironmentConstants.DRONE_SOURCE_BRANCH;
import static io.harness.common.BuildEnvironmentConstants.DRONE_TARGET_BRANCH;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.beans.execution.BranchWebhookEvent;
import io.harness.beans.execution.ExecutionSource;
import io.harness.beans.execution.PRWebhookEvent;
import io.harness.beans.execution.Repository;
import io.harness.beans.execution.WebhookBaseAttributes;
import io.harness.beans.execution.WebhookEvent;
import io.harness.beans.execution.WebhookExecutionSource;
import io.harness.beans.executionargs.CIExecutionArgs;
import lombok.experimental.UtilityClass;

import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class BuildEnvironmentUtils {
  private static final String REPO_SCM = "git";

  public Map<String, String> getBuildEnvironmentVariables(CIExecutionArgs ciExecutionArgs) {
    Map<String, String> envVarMap = new HashMap<>();
    if (ciExecutionArgs == null) {
      return envVarMap;
    }

    envVarMap.put(DRONE_BUILD_NUMBER, ciExecutionArgs.getBuildNumber().getBuildNumber().toString());
    if (ciExecutionArgs.getExecutionSource() == null) {
      return envVarMap;
    }

    if (ciExecutionArgs.getExecutionSource().getType() == ExecutionSource.Type.Webhook) {
      WebhookExecutionSource webhookExecutionSource = (WebhookExecutionSource) ciExecutionArgs.getExecutionSource();
      if (webhookExecutionSource.getWebhookEvent().getType() == WebhookEvent.Type.BRANCH) {
        BranchWebhookEvent branchWebhookEvent = (BranchWebhookEvent) webhookExecutionSource.getWebhookEvent();
        envVarMap.putAll(getBaseEnvVars(branchWebhookEvent.getBaseAttributes()));
        envVarMap.putAll(getBuildRepoEnvvars(branchWebhookEvent.getRepository()));
        envVarMap.put(DRONE_BUILD_EVENT, "push");
      }
      if (webhookExecutionSource.getWebhookEvent().getType() == WebhookEvent.Type.PR) {
        PRWebhookEvent prWebhookEvent = (PRWebhookEvent) webhookExecutionSource.getWebhookEvent();
        envVarMap.putAll(getBaseEnvVars(prWebhookEvent.getBaseAttributes()));
        envVarMap.putAll(getBuildRepoEnvvars(prWebhookEvent.getRepository()));
        envVarMap.put(DRONE_BUILD_EVENT, "pull_request");
      }
    }
    return envVarMap;
  }

  private Map<String, String> getBuildRepoEnvvars(Repository repository) {
    Map<String, String> envVarMap = new HashMap<>();
    setEnvironmentVariable(envVarMap, DRONE_REPO, repository.getSlug());
    setEnvironmentVariable(envVarMap, DRONE_REPO_SCM, REPO_SCM);
    setEnvironmentVariable(envVarMap, DRONE_REPO_OWNER, repository.getNamespace());
    setEnvironmentVariable(envVarMap, DRONE_REPO_NAMESPACE, repository.getNamespace());
    setEnvironmentVariable(envVarMap, DRONE_REPO_NAME, repository.getName());
    setEnvironmentVariable(envVarMap, DRONE_REPO_LINK, repository.getLink());
    setEnvironmentVariable(envVarMap, DRONE_REPO_BRANCH, repository.getBranch());
    setEnvironmentVariable(envVarMap, DRONE_REMOTE_URL, repository.getHttpURL());
    setEnvironmentVariable(envVarMap, DRONE_GIT_HTTP_URL, repository.getHttpURL());
    setEnvironmentVariable(envVarMap, DRONE_GIT_SSH_URL, repository.getSshURL());
    setEnvironmentVariable(envVarMap, DRONE_REPO_PRIVATE, String.valueOf(repository.isPrivate()));
    return envVarMap;
  }

  private Map<String, String> getBaseEnvVars(WebhookBaseAttributes baseAttributes) {
    Map<String, String> envVarMap = new HashMap<>();
    setEnvironmentVariable(envVarMap, DRONE_BRANCH, baseAttributes.getTarget());
    setEnvironmentVariable(envVarMap, DRONE_SOURCE_BRANCH, baseAttributes.getSource());
    setEnvironmentVariable(envVarMap, DRONE_TARGET_BRANCH, baseAttributes.getTarget());
    setEnvironmentVariable(envVarMap, DRONE_COMMIT, baseAttributes.getAfter());
    setEnvironmentVariable(envVarMap, DRONE_COMMIT_SHA, baseAttributes.getAfter());
    setEnvironmentVariable(envVarMap, DRONE_COMMIT_BEFORE, baseAttributes.getBefore());
    setEnvironmentVariable(envVarMap, DRONE_COMMIT_AFTER, baseAttributes.getAfter());
    setEnvironmentVariable(envVarMap, DRONE_COMMIT_REF, baseAttributes.getRef());
    setEnvironmentVariable(envVarMap, DRONE_COMMIT_BRANCH, baseAttributes.getTarget());
    setEnvironmentVariable(envVarMap, DRONE_COMMIT_LINK, baseAttributes.getLink());
    setEnvironmentVariable(envVarMap, DRONE_COMMIT_MESSAGE, baseAttributes.getMessage());
    setEnvironmentVariable(envVarMap, DRONE_COMMIT_AUTHOR, baseAttributes.getAuthorLogin());
    setEnvironmentVariable(envVarMap, DRONE_COMMIT_AUTHOR_EMAIL, baseAttributes.getAuthorEmail());
    setEnvironmentVariable(envVarMap, DRONE_COMMIT_AUTHOR_AVATAR, baseAttributes.getAuthorAvatar());
    setEnvironmentVariable(envVarMap, DRONE_COMMIT_AUTHOR_NAME, baseAttributes.getAuthorName());
    if (isNotEmpty(baseAttributes.getAction())) {
      envVarMap.put(DRONE_BUILD_ACTION, baseAttributes.getAction());
    }
    return envVarMap;
  }

  private void setEnvironmentVariable(Map<String, String> envVarMap, String var, String value) {
    if (value == null) {
      return;
    }
    envVarMap.put(var, value);
  }
}
