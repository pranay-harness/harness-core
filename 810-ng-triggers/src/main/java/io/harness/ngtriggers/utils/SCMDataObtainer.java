package io.harness.ngtriggers.utils;

import static io.harness.annotations.dev.HarnessTeam.CI;

import static software.wings.beans.TaskType.SCM_GIT_REF_TASK;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTaskRequest;
import io.harness.beans.IdentifierRef;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.task.scm.GitRefType;
import io.harness.delegate.task.scm.ScmGitRefTaskParams;
import io.harness.delegate.task.scm.ScmGitRefTaskResponseData;
import io.harness.exception.TriggerException;
import io.harness.exception.WingsException;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.scm.WebhookPayloadData;
import io.harness.ngtriggers.eventmapper.filters.dto.FilterRequestData;
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

  List<Commit> getCommitsInPr(ConnectorDetails connectorDetails, TriggerDetails triggerDetails, long number) {
    if (ScmConnector.class.isAssignableFrom(connectorDetails.getConnectorConfig().getClass())) {
      ScmGitRefTaskParams scmGitRefTaskParams = ScmGitRefTaskParams.builder()
                                                    .prNumber(number)
                                                    .gitRefType(GitRefType.PULL_REQUEST_COMMITS)
                                                    .encryptedDataDetails(connectorDetails.getEncryptedDataDetails())
                                                    .scmConnector((ScmConnector) connectorDetails.getConnectorConfig())
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
              String.format("Failed to fetch connector details. Reason: %s", errorResponseData.getErrorMessage()),
              WingsException.SRE);
        }
      }
      throw new TriggerException("Failed to fetch connector details", WingsException.SRE);
    }
    return new ArrayList<>();
  }
}
