package io.harness.pms.instrumentaion;

import static io.harness.pms.instrumentaion.PipelineInstrumentationConstants.ERROR_MESSAGES;
import static io.harness.pms.instrumentaion.PipelineInstrumentationConstants.EVENT_TYPES;
import static io.harness.pms.instrumentaion.PipelineInstrumentationConstants.EXECUTION_TIME;
import static io.harness.pms.instrumentaion.PipelineInstrumentationConstants.FAILURE_TYPES;
import static io.harness.pms.instrumentaion.PipelineInstrumentationConstants.IS_RERUN;
import static io.harness.pms.instrumentaion.PipelineInstrumentationConstants.LEVEL;
import static io.harness.pms.instrumentaion.PipelineInstrumentationConstants.NOTIFICATION_METHODS;
import static io.harness.pms.instrumentaion.PipelineInstrumentationConstants.NOTIFICATION_RULES_COUNT;
import static io.harness.pms.instrumentaion.PipelineInstrumentationConstants.PIPELINE_EXECUTION;
import static io.harness.pms.instrumentaion.PipelineInstrumentationConstants.PIPELINE_NOTIFICATION;
import static io.harness.pms.instrumentaion.PipelineInstrumentationConstants.STAGE_COUNT;
import static io.harness.pms.instrumentaion.PipelineInstrumentationConstants.STAGE_TYPES;
import static io.harness.pms.instrumentaion.PipelineInstrumentationConstants.STATUS;
import static io.harness.pms.instrumentaion.PipelineInstrumentationConstants.TRIGGER_TYPE;
import static io.harness.telemetry.Destination.AMPLITUDE;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.observers.OrchestrationEndObserver;
import io.harness.notification.bean.NotificationRules;
import io.harness.notification.bean.PipelineEvent;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.notification.NotificationInstrumentationHelper;
import io.harness.pms.pipeline.observer.OrchestrationObserverUtils;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;
import io.harness.pms.plan.execution.service.PMSExecutionService;
import io.harness.telemetry.Category;
import io.harness.telemetry.TelemetryReporter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class InstrumentationPipelineEndEventHandler implements OrchestrationEndObserver {
  @Inject TelemetryReporter telemetryReporter;
  @Inject PMSExecutionService pmsExecutionService;
  @Inject NotificationInstrumentationHelper notificationInstrumentationHelper;

  @Override
  public void onEnd(Ambiance ambiance) {
    String planExecutionId = ambiance.getPlanExecutionId();
    String accountId = AmbianceUtils.getAccountId(ambiance);
    String projectId = AmbianceUtils.getProjectIdentifier(ambiance);
    String orgId = AmbianceUtils.getOrgIdentifier(ambiance);
    PipelineExecutionSummaryEntity pipelineExecutionSummaryEntity =
        pmsExecutionService.getPipelineExecutionSummaryEntity(accountId, orgId, projectId, planExecutionId, false);

    List<NotificationRules> notificationRulesList =
        notificationInstrumentationHelper.getNotificationRules(planExecutionId);

    Set<String> executedModules =
        OrchestrationObserverUtils.getExecutedModulesInPipeline(pipelineExecutionSummaryEntity);

<<<<<<< HEAD
    try (PmsSecurityContextEventGuard securityContextEventGuard = new PmsSecurityContextEventGuard(ambiance)) {
      HashMap<String, Object> propertiesMap = new HashMap<>();
      propertiesMap.put(STAGE_TYPES, executedModules);
      propertiesMap.put(TRIGGER_TYPE, pipelineExecutionSummaryEntity.getExecutionTriggerInfo().getTriggerType());
      propertiesMap.put(STATUS, pipelineExecutionSummaryEntity.getStatus());
      propertiesMap.put(LEVEL, StepCategory.PIPELINE);
      propertiesMap.put(IS_RERUN, pipelineExecutionSummaryEntity.getExecutionTriggerInfo().getIsRerun());
      propertiesMap.put(STAGE_COUNT, pipelineExecutionSummaryEntity.getLayoutNodeMap().size());
      propertiesMap.put(EXECUTION_TIME, getExecutionTimeInSeconds(pipelineExecutionSummaryEntity));
      propertiesMap.put(NOTIFICATION_RULES_COUNT, notificationRulesList.size());
      propertiesMap.put(FAILURE_TYPES,
          PipelineInstrumentationUtils.getFailureTypesFromPipelineExecutionSummary(pipelineExecutionSummaryEntity));
      propertiesMap.put(ERROR_MESSAGES,
          PipelineInstrumentationUtils.getErrorMessagesFromPipelineExecutionSummary(pipelineExecutionSummaryEntity));
      propertiesMap.put(
          NOTIFICATION_METHODS, notificationInstrumentationHelper.getNotificationMethodTypes(notificationRulesList));
      String identity = ambiance.getMetadata().getTriggerInfo().getTriggeredBy().getExtraInfoMap().get("email");
      telemetryReporter.sendTrackEvent(PIPELINE_EXECUTION, identity, accountId, propertiesMap,
          Collections.singletonMap(AMPLITUDE, true), Category.GLOBAL);
=======
    HashMap<String, Object> propertiesMap = new HashMap<>();
    propertiesMap.put(STAGE_TYPES, executedModules);
    propertiesMap.put(TRIGGER_TYPE, pipelineExecutionSummaryEntity.getExecutionTriggerInfo().getTriggerType());
    propertiesMap.put(STATUS, pipelineExecutionSummaryEntity.getStatus());
    propertiesMap.put(LEVEL, StepCategory.PIPELINE);
    propertiesMap.put(IS_RERUN, pipelineExecutionSummaryEntity.getExecutionTriggerInfo().getIsRerun());
    propertiesMap.put(STAGE_COUNT, pipelineExecutionSummaryEntity.getLayoutNodeMap().size());
    propertiesMap.put(EXECUTION_TIME, getExecutionTimeInSeconds(pipelineExecutionSummaryEntity));
    propertiesMap.put(NOTIFICATION_RULES_COUNT, notificationRulesList.size());
    propertiesMap.put(FAILURE_TYPES,
        PipelineInstrumentationUtils.getFailureTypesFromPipelineExecutionSummary(pipelineExecutionSummaryEntity));
    propertiesMap.put(ERROR_MESSAGES,
        PipelineInstrumentationUtils.getErrorMessagesFromPipelineExecutionSummary(pipelineExecutionSummaryEntity));
    propertiesMap.put(
        NOTIFICATION_METHODS, notificationInstrumentationHelper.getNotificationMethodTypes(notificationRulesList));
    String identity = ambiance.getMetadata().getTriggerInfo().getTriggeredBy().getExtraInfoMap().get("email");
    telemetryReporter.sendTrackEvent(PIPELINE_EXECUTION, identity, accountId, propertiesMap,
        Collections.singletonMap(AMPLITUDE, true), Category.GLOBAL);
>>>>>>> 98028592e376ea8866d9fdb08f4bc420bbd5ef60

    sendNotificationEvents(notificationRulesList, ambiance, accountId);
  }

  // TODO: Handle forStages case in PipelineEvents
  private void sendNotificationEvents(
      List<NotificationRules> notificationRulesList, Ambiance ambiance, String accountId) {
    for (NotificationRules notificationRules : notificationRulesList) {
      HashMap<String, Object> propertiesMap = new HashMap<>();
      propertiesMap.put(EVENT_TYPES,
          notificationRules.getPipelineEvents().stream().map(PipelineEvent::getType).collect(Collectors.toSet()));
      String email = PipelineInstrumentationUtils.getIdentityFromAmbiance(ambiance);
      telemetryReporter.sendTrackEvent(PIPELINE_NOTIFICATION, email, accountId, propertiesMap,
          Collections.singletonMap(AMPLITUDE, true), Category.GLOBAL);
    }
  }

  private Long getExecutionTimeInSeconds(PipelineExecutionSummaryEntity pipelineExecutionSummaryEntity) {
    return (pipelineExecutionSummaryEntity.getEndTs() - pipelineExecutionSummaryEntity.getStartTs()) / 1000;
  }
}
