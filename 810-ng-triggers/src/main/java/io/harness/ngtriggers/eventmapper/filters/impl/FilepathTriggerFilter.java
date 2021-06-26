
package io.harness.ngtriggers.eventmapper.filters.impl;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.ngtriggers.Constants.BITBUCKET_LOWER_CASE;
import static io.harness.ngtriggers.Constants.CHANGED_FILES;
import static io.harness.ngtriggers.Constants.COMMIT_FILE_ADDED;
import static io.harness.ngtriggers.Constants.COMMIT_FILE_MODIFIED;
import static io.harness.ngtriggers.Constants.COMMIT_FILE_REMOVED;
import static io.harness.ngtriggers.Constants.GITHUB_LOWER_CASE;
import static io.harness.ngtriggers.Constants.GITLAB_LOWER_CASE;
import static io.harness.ngtriggers.Constants.TRIGGER_PAYLOAD_COMMITS;
import static io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus.NO_MATCHING_TRIGGER_FOR_FILEPATH_CONDITIONS;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTaskRequest;
import io.harness.beans.IdentifierRef;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnectorDTO;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnectorDTO;
import io.harness.delegate.task.scm.ScmPathFilterEvaluationTaskParams;
import io.harness.delegate.task.scm.ScmPathFilterEvaluationTaskParams.ScmPathFilterEvaluationTaskParamsBuilder;
import io.harness.delegate.task.scm.ScmPathFilterEvaluationTaskResponse;
import io.harness.exception.TriggerException;
import io.harness.exception.WingsException;
import io.harness.ngtriggers.beans.config.NGTriggerConfigV2;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventMappingResponse;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventMappingResponse.WebhookEventMappingResponseBuilder;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.metadata.WebhookMetadata;
import io.harness.ngtriggers.beans.source.NGTriggerSpecV2;
import io.harness.ngtriggers.beans.source.WebhookTriggerType;
import io.harness.ngtriggers.beans.source.webhook.v2.TriggerEventDataCondition;
import io.harness.ngtriggers.beans.source.webhook.v2.WebhookTriggerConfigV2;
import io.harness.ngtriggers.conditionchecker.ConditionEvaluator;
import io.harness.ngtriggers.eventmapper.filters.TriggerFilter;
import io.harness.ngtriggers.eventmapper.filters.dto.FilterRequestData;
import io.harness.ngtriggers.expressions.TriggerExpressionEvaluator;
import io.harness.ngtriggers.helpers.WebhookEventResponseHelper;
import io.harness.ngtriggers.mapper.NGTriggerElementMapper;
import io.harness.ngtriggers.service.NGTriggerService;
import io.harness.ngtriggers.utils.TaskExecutionUtils;
import io.harness.ngtriggers.utils.WebhookTriggerFilterUtils;
import io.harness.product.ci.scm.proto.ParseWebhookResponse;
import io.harness.serializer.KryoSerializer;
import io.harness.tasks.BinaryResponseData;
import io.harness.tasks.ErrorResponseData;
import io.harness.tasks.ResponseData;
import io.harness.utils.ConnectorUtils;

import software.wings.beans.TaskType;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
@OwnedBy(PIPELINE)
public class FilepathTriggerFilter implements TriggerFilter {
  private TaskExecutionUtils taskExecutionUtils;
  private NGTriggerElementMapper ngTriggerElementMapper;
  private final NGTriggerService ngTriggerService;
  private KryoSerializer kryoSerializer;
  private ConnectorUtils connectorUtils;

  @Override
  public WebhookEventMappingResponse applyFilter(FilterRequestData filterRequestData) {
    WebhookEventMappingResponseBuilder mappingResponseBuilder = initWebhookEventMappingResponse(filterRequestData);

    // If not push or PR, return list as is, as path filters does not apply
    if (pathFilterEvaluationNotNeeded(filterRequestData)) {
      return mappingResponseBuilder.failedToFindTrigger(false)
          .parseWebhookResponse(filterRequestData.getWebhookPayloadData().getParseWebhookResponse())
          .triggers(filterRequestData.getDetails())
          .build();
    }

    List<TriggerDetails> matchedTriggers = new ArrayList<>();
    for (TriggerDetails trigger : filterRequestData.getDetails()) {
      NGTriggerConfigV2 ngTriggerConfig = trigger.getNgTriggerConfigV2();
      if (ngTriggerConfig == null) {
        ngTriggerConfig = ngTriggerElementMapper.toTriggerConfigV2(trigger.getNgTriggerEntity());
      }

      TriggerDetails triggerDetails = TriggerDetails.builder()
                                          .ngTriggerConfigV2(ngTriggerConfig)
                                          .ngTriggerEntity(trigger.getNgTriggerEntity())
                                          .build();
      if (checkTriggerEligibility(filterRequestData, triggerDetails)) {
        matchedTriggers.add(triggerDetails);
      }
    }

    if (isEmpty(matchedTriggers)) {
      log.info("No trigger matched Path condition after filter evaluation:");
      mappingResponseBuilder.failedToFindTrigger(true)
          .webhookEventResponse(WebhookEventResponseHelper.toResponse(NO_MATCHING_TRIGGER_FOR_FILEPATH_CONDITIONS,
              filterRequestData.getWebhookPayloadData().getOriginalEvent(), null, null,
              "No trigger matched Path condition after filter evaluation for Account: "
                  + filterRequestData.getAccountId(),
              null))
          .build();
    } else {
      addDetails(mappingResponseBuilder, filterRequestData, matchedTriggers);
    }
    return mappingResponseBuilder.build();
  }

  @VisibleForTesting
  boolean pathFilterEvaluationNotNeeded(FilterRequestData filterRequestData) {
    ParseWebhookResponse parseWebhookResponse = filterRequestData.getWebhookPayloadData().getParseWebhookResponse();
    if (parseWebhookResponse == null) {
      return true;
    }

    if (filterRequestData.getWebhookPayloadData().getOriginalEvent().getSourceRepoType().equalsIgnoreCase(
            WebhookTriggerType.AWS_CODECOMMIT.getEntityMetadataName())) {
      return true;
    }

    if (!parseWebhookResponse.hasPr() && !parseWebhookResponse.hasPush() && !parseWebhookResponse.hasComment()) {
      return true;
    }
    return false;
  }

  @VisibleForTesting
  boolean checkTriggerEligibility(FilterRequestData filterRequestData, TriggerDetails triggerDetails) {
    try {
      NGTriggerSpecV2 spec = triggerDetails.getNgTriggerConfigV2().getSource().getSpec();
      if (!WebhookTriggerConfigV2.class.isAssignableFrom(spec.getClass())) {
        log.error("Trigger spec is not a WebhookTriggerConfig");
        return false;
      }

      WebhookTriggerConfigV2 webhookTriggerConfig = (WebhookTriggerConfigV2) spec;
      List<TriggerEventDataCondition> payloadConditions =
          webhookTriggerConfig.getSpec().fetchPayloadAware().fetchPayloadConditions();

      if (isEmpty(payloadConditions)) {
        return true;
      }

      TriggerEventDataCondition pathCondition =
          payloadConditions.stream()
              .filter(payloadCondition -> CHANGED_FILES.equalsIgnoreCase(payloadCondition.getKey()))
              .findFirst()
              .orElse(null);
      if (pathCondition == null) {
        return true;
      }

      if (shouldEvaluateOnDelegate(filterRequestData)) {
        return initiateDeleteTaskAndEvaluate(filterRequestData, triggerDetails, pathCondition);
      } else {
        return evaluateFromPushPayload(filterRequestData, pathCondition);
      }
    } catch (Exception e) {
      log.error(getTriggerSkipMessage(triggerDetails.getNgTriggerEntity()), e);
      return false;
    }
  }

  private boolean evaluateFromPushPayload(
      FilterRequestData filterRequestData, TriggerEventDataCondition pathCondition) {
    Set<String> payloadFiles = getFilesFromPushPayload(filterRequestData);

    boolean eligible = true;
    for (String pathFetched : payloadFiles) {
      if (ConditionEvaluator.evaluate(pathFetched, pathCondition.getValue(), pathCondition.getOperator().getValue())) {
        eligible = true;
        break;
      }
    }

    return eligible;
  }

  private boolean initiateDeleteTaskAndEvaluate(
      FilterRequestData filterRequestData, TriggerDetails triggerDetails, TriggerEventDataCondition pathCondition) {
    ScmPathFilterEvaluationTaskResponse scmPathFilterEvaluationTaskResponse =
        performScmPathFilterEvaluation(triggerDetails.getNgTriggerEntity(), filterRequestData, pathCondition);
    if (scmPathFilterEvaluationTaskResponse == null) {
      log.error(new StringBuilder(128)
                    .append(getTriggerSkipMessage(triggerDetails.getNgTriggerEntity()))
                    .append(", Null response from Delegate Task: ")
                    .toString());
      return false;
    } else {
      if (isNotEmpty(scmPathFilterEvaluationTaskResponse.getErrorMessage())) {
        log.error(new StringBuilder(128)
                      .append(getTriggerSkipMessage(triggerDetails.getNgTriggerEntity()))
                      .append(", Error Message from Delegate Task: ")
                      .append(scmPathFilterEvaluationTaskResponse.getErrorMessage())
                      .toString());
      }
      return scmPathFilterEvaluationTaskResponse.isMatched();
    }
  }

  private ScmPathFilterEvaluationTaskResponse performScmPathFilterEvaluation(
      NGTriggerEntity ngTriggerEntity, FilterRequestData filterRequestData, TriggerEventDataCondition pathCondition) {
    try {
      WebhookMetadata webhook = ngTriggerEntity.getMetadata().getWebhook();
      ConnectorDetails connectorDetails =
          connectorUtils.getConnectorDetails(IdentifierRef.builder()
                                                 .accountIdentifier(ngTriggerEntity.getAccountId())
                                                 .orgIdentifier(ngTriggerEntity.getOrgIdentifier())
                                                 .projectIdentifier(ngTriggerEntity.getProjectIdentifier())
                                                 .build(),
              webhook.getGit().getConnectorIdentifier());

      ScmConnector connector = null;
      ConnectorConfigDTO connectorConfigDTO = connectorDetails.getConnectorConfig();
      if (connectorConfigDTO.getClass().isAssignableFrom(GithubConnectorDTO.class)) {
        connector = (GithubConnectorDTO) connectorConfigDTO;
      } else if (connectorConfigDTO.getClass().isAssignableFrom(GitlabConnectorDTO.class)) {
        connector = (GitlabConnectorDTO) connectorConfigDTO;
      } else if (connectorConfigDTO.getClass().isAssignableFrom(BitbucketConnectorDTO.class)) {
        connector = (BitbucketConnectorDTO) connectorConfigDTO;
      }

      if (connector == null) {
        return null;
      }

      ScmPathFilterEvaluationTaskParamsBuilder paramsBuilder =
          ScmPathFilterEvaluationTaskParams.builder()
              .encryptedDataDetails(connectorDetails.getEncryptedDataDetails())
              .scmConnector(connector)
              .operator(pathCondition.getOperator().getValue())
              .standard(pathCondition.getValue());
      ParseWebhookResponse parseWebhookResponse = filterRequestData.getWebhookPayloadData().getParseWebhookResponse();

      switch (parseWebhookResponse.getHookCase()) {
        case PR:
          paramsBuilder.prNumber((int) parseWebhookResponse.getPr().getPr().getNumber());
          break;
        default:
          paramsBuilder.branch(parseWebhookResponse.getPush().getRef())
              .latestCommit(parseWebhookResponse.getPush().getAfter())
              .previousCommit(parseWebhookResponse.getPush().getBefore());
      }
      ScmPathFilterEvaluationTaskParams params = paramsBuilder.build();
      DelegateTaskRequest delegateTaskRequest = DelegateTaskRequest.builder()
                                                    .accountId(filterRequestData.getAccountId())
                                                    .taskType(TaskType.SCM_PATH_FILTER_EVALUATION_TASK.toString())
                                                    .taskParameters(params)
                                                    .executionTimeout(Duration.ofMinutes(1l))
                                                    .build();
      ResponseData responseData = taskExecutionUtils.executeSyncTask(delegateTaskRequest);

      if (BinaryResponseData.class.isAssignableFrom(responseData.getClass())) {
        BinaryResponseData binaryResponseData = (BinaryResponseData) responseData;
        Object object = kryoSerializer.asInflatedObject(binaryResponseData.getData());
        if (object instanceof ScmPathFilterEvaluationTaskResponse) {
          return (ScmPathFilterEvaluationTaskResponse) object;
        } else if (object instanceof ErrorResponseData) {
          ErrorResponseData errorResponseData = (ErrorResponseData) object;
          throw new TriggerException(
              String.format("Failed to fetch PR Details. Reason: {}", errorResponseData.getErrorMessage()),
              WingsException.SRE);
        }
      }
    } catch (Exception e) {
      log.error(getTriggerSkipMessage(ngTriggerEntity) + ". Filed in executing delegate task");
      return null;
    }

    return null;
  }

  // Github, gitlab docs say, payload would contains details about 20 commits.
  // So if there more than or equal to 20 commits, there is a chance, few commits were truncated.
  // So, we go to delegate task.
  boolean shouldEvaluateOnDelegate(FilterRequestData filterRequestData) {
    if (filterRequestData.getWebhookPayloadData().getParseWebhookResponse().hasPr()) {
      return true;
    } else if (filterRequestData.getWebhookPayloadData().getParseWebhookResponse().hasPush()) {
      String sourceRepoType =
          filterRequestData.getWebhookPayloadData().getOriginalEvent().getSourceRepoType().toLowerCase();
      switch (sourceRepoType) {
        case GITHUB_LOWER_CASE:
        case GITLAB_LOWER_CASE:
          int commitsCount =
              filterRequestData.getWebhookPayloadData().getParseWebhookResponse().getPush().getCommitsCount();
          return commitsCount >= 20;
        case BITBUCKET_LOWER_CASE:
        default:
          return true;
      }
    } else {
      // No Path filter evaluation needed.
      return true;
    }
  }

  Set<String> getFilesFromPushPayload(FilterRequestData filterRequestData) {
    Set<String> pushPayloadFiles = new HashSet<>();
    TriggerExpressionEvaluator triggerExpressionEvaluator =
        WebhookTriggerFilterUtils.generatorPMSExpressionEvaluator(filterRequestData.getWebhookPayloadData());
    switch (filterRequestData.getWebhookPayloadData().getOriginalEvent().getSourceRepoType().toLowerCase()) {
      case GITHUB_LOWER_CASE:
      case GITLAB_LOWER_CASE:
        for (Object commitObject : (List) triggerExpressionEvaluator.evaluateExpression(TRIGGER_PAYLOAD_COMMITS)) {
          Map<String, Object> commitJson = (Map) commitObject;
          for (Object added : (List) commitJson.get(COMMIT_FILE_ADDED)) {
            pushPayloadFiles.add((String) added);
          }
          for (Object modified : (List) commitJson.get(COMMIT_FILE_MODIFIED)) {
            pushPayloadFiles.add((String) modified);
          }
          for (Object removed : (List) commitJson.get(COMMIT_FILE_REMOVED)) {
            pushPayloadFiles.add((String) removed);
          }
        }
        return pushPayloadFiles;
      case BITBUCKET_LOWER_CASE:
      default:
        return pushPayloadFiles;
    }
  }
}