package io.harness.ngtriggers.eventmapper.filters.impl;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus.NO_MATCHING_TRIGGER_FOR_FILEPATH_CONDITIONS;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTaskRequest;
import io.harness.connector.ConnectorResourceClient;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.task.scm.ScmPathFilterEvaluationTaskParams;
import io.harness.delegate.task.scm.ScmPathFilterEvaluationTaskParams.ScmPathFilterEvaluationTaskParamsBuilder;
import io.harness.delegate.task.scm.ScmPathFilterEvaluationTaskResponseData;
import io.harness.ngtriggers.beans.config.NGTriggerConfig;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventMappingResponse;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventMappingResponse.WebhookEventMappingResponseBuilder;
import io.harness.ngtriggers.beans.source.webhook.WebhookTriggerConfig;
import io.harness.ngtriggers.beans.source.webhook.WebhookTriggerSpec;
import io.harness.ngtriggers.conditionchecker.ConditionEvaluator;
import io.harness.ngtriggers.eventmapper.filters.TriggerFilter;
import io.harness.ngtriggers.eventmapper.filters.dto.FilterRequestData;
import io.harness.ngtriggers.expressions.TriggerExpressionEvaluator;
import io.harness.ngtriggers.helpers.WebhookEventResponseHelper;
import io.harness.ngtriggers.mapper.NGTriggerElementMapper;
import io.harness.ngtriggers.utils.WebhookTriggerFilterUtils;
import io.harness.product.ci.scm.proto.ParseWebhookResponse;
import io.harness.remote.client.NGRestUtils;
import io.harness.service.DelegateGrpcClientWrapper;

import software.wings.beans.TaskType;

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
  private NGTriggerElementMapper ngTriggerElementMapper;
  private DelegateGrpcClientWrapper delegateGrpcClientWrapper;
  private ConnectorResourceClient connectorService;

  @Override
  public WebhookEventMappingResponse applyFilter(FilterRequestData filterRequestData) {
    WebhookEventMappingResponseBuilder mappingResponseBuilder = initWebhookEventMappingResponse(filterRequestData);
    List<TriggerDetails> matchedTriggers = new ArrayList<>();

    for (TriggerDetails trigger : filterRequestData.getDetails()) {
      NGTriggerConfig ngTriggerConfig = trigger.getNgTriggerConfig();
      if (ngTriggerConfig == null) {
        ngTriggerConfig = ngTriggerElementMapper.toTriggerConfig(trigger.getNgTriggerEntity().getYaml());
      }

      TriggerDetails triggerDetails = TriggerDetails.builder()
                                          .ngTriggerConfig(ngTriggerConfig)
                                          .ngTriggerEntity(trigger.getNgTriggerEntity())
                                          .build();
      if (checkTriggerEligibility(filterRequestData, triggerDetails)) {
        matchedTriggers.add(triggerDetails);
      }
    }

    if (isEmpty(matchedTriggers)) {
      log.info("No trigger matched payload after condition evaluation:");
      mappingResponseBuilder.failedToFindTrigger(true)
          .webhookEventResponse(WebhookEventResponseHelper.toResponse(NO_MATCHING_TRIGGER_FOR_FILEPATH_CONDITIONS,
              filterRequestData.getWebhookPayloadData().getOriginalEvent(), null, null,
              "No Trigger matched conditions for payload event for Account: " + filterRequestData.getAccountId(), null))
          .build();
    } else {
      addDetails(mappingResponseBuilder, filterRequestData, matchedTriggers);
    }
    return mappingResponseBuilder.build();
  }

  boolean checkTriggerEligibility(FilterRequestData filterRequestData, TriggerDetails triggerDetails) {
    WebhookTriggerConfig webhookTriggerSpec =
        (WebhookTriggerConfig) (triggerDetails.getNgTriggerConfig().getSource().getSpec());
    ScmConnector connector;
    try {
      connector = (ScmConnector) NGRestUtils
                      .getResponse(connectorService.get(
                          ((WebhookTriggerSpec) (triggerDetails.getNgTriggerConfig().getSource().getSpec()))
                              .getRepoSpec()
                              .getIdentifier(),
                          triggerDetails.getNgTriggerEntity().getAccountId(),
                          triggerDetails.getNgTriggerEntity().getOrgIdentifier(),
                          triggerDetails.getNgTriggerEntity().getProjectIdentifier()))
                      .get();
    } catch (Exception e) {
      return false;
    }
    if (shouldEvaluateOnDelegate(filterRequestData)) {
      ScmPathFilterEvaluationTaskParamsBuilder paramsBuilder =
          ScmPathFilterEvaluationTaskParams.builder()
              .scmConnector(connector)
              .operator(webhookTriggerSpec.getSpec().getPathFilters().getOperator())
              .standard(webhookTriggerSpec.getSpec().getPathFilters().getValue());
      ParseWebhookResponse parseWebhookResponse = filterRequestData.getWebhookPayloadData().getParseWebhookResponse();
      switch (parseWebhookResponse.getHookCase()) {
        case PR:
          paramsBuilder.prNumber((int) parseWebhookResponse.getPr().getPr().getNumber());
          break;
        default:
          paramsBuilder.branch(parseWebhookResponse.getPush().getRepo().getBranch())
              .latestCommit(parseWebhookResponse.getPush().getAfter())
              .previousCommit(parseWebhookResponse.getPush().getBefore());
      }
      ScmPathFilterEvaluationTaskParams params = paramsBuilder.build();
      DelegateTaskRequest delegateTaskRequest = DelegateTaskRequest.builder()
                                                    .accountId(filterRequestData.getAccountId())
                                                    .taskType(TaskType.SCM_PATH_FILTER_EVALUATION_TASK.toString())
                                                    .taskParameters(params)
                                                    .executionTimeout(Duration.ofMinutes(1L))
                                                    .build();
      ScmPathFilterEvaluationTaskResponseData delegateResponseData =
          (ScmPathFilterEvaluationTaskResponseData) delegateGrpcClientWrapper.executeSyncTask(delegateTaskRequest);
      return delegateResponseData.isMatched();
    } else {
      Set<String> payloadFiles = getFilesFromPushPayload(filterRequestData);

      for (String filepath : payloadFiles) {
        if (ConditionEvaluator.evaluate(filepath, webhookTriggerSpec.getSpec().getPathFilters().getOperator(),
                webhookTriggerSpec.getSpec().getPathFilters().getValue())) {
          return true;
        }
      }
      return false;
    }
  }

  boolean shouldEvaluateOnDelegate(FilterRequestData filterRequestData) {
    switch (filterRequestData.getWebhookPayloadData().getParseWebhookResponse().getHookCase()) {
      case PR:
        return true;
      default: // continue
    }
    TriggerExpressionEvaluator triggerExpressionEvaluator =
        WebhookTriggerFilterUtils.generatorPMSExpressionEvaluator(filterRequestData.getWebhookPayloadData());
    switch (filterRequestData.getWebhookPayloadData().getOriginalEvent().getSourceRepoType()) {
      case "Github":
        // while we are only guaranteed 20 per the spec, experiments have shown that we can go over the limit
        return false;
      case "Gitlab":
        int payloadSize =
            (Integer) triggerExpressionEvaluator.evaluateExpression("<+trigger.payload.total_commits_count>");
        return payloadSize > 20;
      case "Bitbucket":
      default:
        return true;
    }
  }

  Set<String> getFilesFromPushPayload(FilterRequestData filterRequestData) {
    Set<String> pushPayloadFiles = new HashSet<>();
    TriggerExpressionEvaluator triggerExpressionEvaluator =
        WebhookTriggerFilterUtils.generatorPMSExpressionEvaluator(filterRequestData.getWebhookPayloadData());
    switch (filterRequestData.getWebhookPayloadData().getOriginalEvent().getSourceRepoType()) {
      case "Github":
      case "Gitlab":
        for (Object commitObject : (List) triggerExpressionEvaluator.evaluateExpression("<+trigger.payload.commits>")) {
          Map<String, Object> commitJson = (Map) commitObject;
          for (Object added : (List) commitJson.get("added")) {
            pushPayloadFiles.add((String) added);
          }
          for (Object modified : (List) commitJson.get("modified")) {
            pushPayloadFiles.add((String) modified);
          }
          for (Object removed : (List) commitJson.get("removed")) {
            pushPayloadFiles.add((String) removed);
          }
        }
        return pushPayloadFiles;
      case "Bitbucket":
      default:
        return pushPayloadFiles;
    }
  }
}
