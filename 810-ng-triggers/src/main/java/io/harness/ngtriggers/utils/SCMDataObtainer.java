package io.harness.ngtriggers.utils;

import static io.harness.annotations.dev.HarnessTeam.CI;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.beans.connector.ConnectorType.BITBUCKET;
import static io.harness.delegate.beans.connector.ConnectorType.GIT;
import static io.harness.delegate.beans.connector.ConnectorType.GITHUB;
import static io.harness.delegate.beans.connector.ConnectorType.GITLAB;

import static software.wings.beans.TaskType.SCM_GIT_REF_TASK;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTaskRequest;
import io.harness.beans.IdentifierRef;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnector;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfig;
import io.harness.delegate.beans.connector.scm.github.GithubConnector;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnector;
import io.harness.delegate.task.scm.GitRefType;
import io.harness.delegate.task.scm.ScmGitRefTaskParams;
import io.harness.delegate.task.scm.ScmGitRefTaskResponseData;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.TriggerException;
import io.harness.exception.WingsException;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.scm.WebhookPayloadData;
import io.harness.ngtriggers.beans.source.webhook.v2.WebhookTriggerConfigV2;
import io.harness.ngtriggers.beans.source.webhook.v2.git.GitAware;
import io.harness.ngtriggers.eventmapper.filters.dto.FilterRequestData;
import io.harness.ngtriggers.helpers.WebhookConfigHelper;
import io.harness.product.ci.scm.proto.Commit;
import io.harness.product.ci.scm.proto.ListCommitsInPRResponse;
import io.harness.product.ci.scm.proto.ParseWebhookResponse;
import io.harness.product.ci.scm.proto.PullRequest;
import io.harness.product.ci.scm.proto.PullRequestHook;
import io.harness.serializer.KryoSerializer;
import io.harness.tasks.BinaryResponseData;
import io.harness.tasks.ErrorResponseData;
import io.harness.tasks.ResponseData;
import io.harness.utils.ConnectorUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(CI)
public class SCMDataObtainer implements GitProviderBaseDataObtainer {
  private final TaskExecutionUtils taskExecutionUtils;
  private final ConnectorUtils connectorUtils;
  private final KryoSerializer kryoSerializer;
  public static final String GIT_URL_SUFFIX = ".git";
  public static final String PATH_SEPARATOR = "/";

  @Inject
  public SCMDataObtainer(
      TaskExecutionUtils taskExecutionUtils, ConnectorUtils connectorUtils, KryoSerializer kryoSerializer) {
    this.taskExecutionUtils = taskExecutionUtils;
    this.connectorUtils = connectorUtils;
    this.kryoSerializer = kryoSerializer;
  }

  @Override
  public void acquireProviderData(FilterRequestData filterRequestData, List<TriggerDetails> triggers) {
    WebhookPayloadData webhookPayloadData = filterRequestData.getWebhookPayloadData();
    ParseWebhookResponse parseWebhookResponse = webhookPayloadData.getParseWebhookResponse();
    if (parseWebhookResponse.hasPr()) {
      acquirePullRequestCommits(filterRequestData, triggers);
    }
  }

  private String getGitURL(GitConnectionType connectionType, String url, String repoName) {
    String gitUrl = retrieveGenericGitConnectorURL(repoName, connectionType, url);

    if (!url.endsWith(GIT_URL_SUFFIX) && !url.contains("dev.azure.com")) {
      gitUrl += GIT_URL_SUFFIX;
    }
    return gitUrl;
  }

  private String retrieveGenericGitConnectorURL(String repoName, GitConnectionType connectionType, String url) {
    String gitUrl;
    if (connectionType == GitConnectionType.REPO) {
      gitUrl = url;
    } else if (connectionType == GitConnectionType.ACCOUNT) {
      if (isEmpty(repoName)) {
        throw new IllegalArgumentException("Repo name is not set in trigger git connector spec");
      }

      if (url.endsWith(PATH_SEPARATOR)) {
        gitUrl = url + repoName;
      } else {
        gitUrl = url + PATH_SEPARATOR + repoName;
      }
    } else {
      throw new InvalidArgumentsException(
          format("Invalid connection type for git connector: %s", connectionType.toString()), WingsException.USER);
    }

    return gitUrl;
  }

  private void acquirePullRequestCommits(FilterRequestData filterRequestData, List<TriggerDetails> triggers) {
    WebhookPayloadData webhookPayloadData = filterRequestData.getWebhookPayloadData();
    ParseWebhookResponse parseWebhookResponse = webhookPayloadData.getParseWebhookResponse();
    PullRequestHook pullRequestHook = parseWebhookResponse.getPr();
    PullRequest pullRequest = pullRequestHook.getPr();
    List<Commit> commitsInPr = new ArrayList<>();
    for (TriggerDetails triggerDetails : triggers) {
      try {
        String connectorIdentifier =
            triggerDetails.getNgTriggerEntity().getMetadata().getWebhook().getGit().getConnectorIdentifier();

        ConnectorDetails connectorDetails = connectorUtils.getConnectorDetails(
            IdentifierRef.builder()
                .accountIdentifier(triggerDetails.getNgTriggerEntity().getAccountId())
                .orgIdentifier(triggerDetails.getNgTriggerEntity().getOrgIdentifier())
                .projectIdentifier(triggerDetails.getNgTriggerEntity().getProjectIdentifier())
                .build(),
            connectorIdentifier);

        commitsInPr.addAll(getCommitsInPr(connectorDetails, triggerDetails, pullRequest.getNumber()));
        break;

      } catch (Exception e) {
        log.error("Failed while fetching additional information from git provider for branch webhook event"
                + "Project : " + filterRequestData.getAccountId() + ", with Exception" + e.getMessage(),
            e);
      }
    }
    PullRequest updatedPullRequest = pullRequest.toBuilder().addAllCommits(commitsInPr).build();
    PullRequestHook updatedPullRequestHook = pullRequestHook.toBuilder().setPr(updatedPullRequest).build();
    ParseWebhookResponse updatedParseWebhookResponse =
        parseWebhookResponse.toBuilder().setPr(updatedPullRequestHook).build();
    WebhookPayloadData updatedWebhookPayloadData =
        webhookPayloadData.toBuilder().parseWebhookResponse(updatedParseWebhookResponse).build();
    filterRequestData.setWebhookPayloadData(updatedWebhookPayloadData);
  }

  private GitConnectionType retrieveGitConnectionType(ConnectorDetails gitConnector) {
    if (gitConnector.getConnectorType() == GITHUB) {
      GithubConnector gitConfigDTO = (GithubConnector) gitConnector.getConnectorConfig();
      return gitConfigDTO.getConnectionType();
    } else if (gitConnector.getConnectorType() == BITBUCKET) {
      BitbucketConnector gitConfigDTO = (BitbucketConnector) gitConnector.getConnectorConfig();
      return gitConfigDTO.getConnectionType();
    } else if (gitConnector.getConnectorType() == GITLAB) {
      GitlabConnector gitConfigDTO = (GitlabConnector) gitConnector.getConnectorConfig();
      return gitConfigDTO.getConnectionType();
    } else if (gitConnector.getConnectorType() == GIT) {
      GitConfig gitConfig = (GitConfig) gitConnector.getConnectorConfig();
      return gitConfig.getGitConnectionType();
    } else {
      throw new CIStageExecutionException("scmType " + gitConnector.getConnectorType() + "is not supported");
    }
  }

  List<Commit> getCommitsInPr(ConnectorDetails connectorDetails, TriggerDetails triggerDetails, long number) {
    ScmConnector scmConnector = (ScmConnector) connectorDetails.getConnectorConfig();
    try {
      GitAware gitAware = WebhookConfigHelper.retrieveGitAware(
          (WebhookTriggerConfigV2) triggerDetails.getNgTriggerConfigV2().getSource().getSpec());

      String repoName = gitAware.fetchRepoName();

      scmConnector.setUrl(getGitURL(retrieveGitConnectionType(connectorDetails), scmConnector.getUrl(), repoName));
    } catch (Exception ex) {
      log.error("Failed to update url");
    }
    if (ScmConnector.class.isAssignableFrom(connectorDetails.getConnectorConfig().getClass())) {
      ScmGitRefTaskParams scmGitRefTaskParams = ScmGitRefTaskParams.builder()
                                                    .prNumber(number)
                                                    .gitRefType(GitRefType.PULL_REQUEST_COMMITS)
                                                    .encryptedDataDetails(connectorDetails.getEncryptedDataDetails())
                                                    .scmConnector(scmConnector)
                                                    .build();
      ResponseData responseData =
          taskExecutionUtils.executeSyncTask(DelegateTaskRequest.builder()
                                                 .accountId(triggerDetails.getNgTriggerEntity().getAccountId())
                                                 .executionTimeout(Duration.ofSeconds(30))
                                                 .taskType(SCM_GIT_REF_TASK.name())
                                                 .taskParameters(scmGitRefTaskParams)
                                                 .build());

      if (BinaryResponseData.class.isAssignableFrom(responseData.getClass())) {
        BinaryResponseData binaryResponseData = (BinaryResponseData) responseData;
        Object object = kryoSerializer.asInflatedObject(binaryResponseData.getData());
        if (ScmGitRefTaskResponseData.class.isAssignableFrom(object.getClass())) {
          ScmGitRefTaskResponseData scmGitRefTaskResponseData = (ScmGitRefTaskResponseData) object;
          try {
            return ListCommitsInPRResponse.parseFrom(scmGitRefTaskResponseData.getListCommitsInPRResponse())
                .getCommitsList();
          } catch (InvalidProtocolBufferException e) {
            throw new TriggerException("Unexpected error occurred while doing scm operation", WingsException.SRE);
          }
        } else if (object instanceof ErrorResponseData) {
          ErrorResponseData errorResponseData = (ErrorResponseData) object;
          throw new TriggerException(
              String.format("Failed to fetch commit details. Reason: %s", errorResponseData.getErrorMessage()),
              WingsException.SRE);
        }
      }
      throw new TriggerException("Failed to fetch commit details", WingsException.SRE);
    }
    return new ArrayList<>();
  }
}
