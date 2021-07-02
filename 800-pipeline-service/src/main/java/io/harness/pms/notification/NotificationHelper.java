package io.harness.pms.notification;

import io.harness.PipelineServiceConfiguration;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.executions.plan.PlanExecutionMetadataService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.engine.utils.OrchestrationUtils;
import io.harness.execution.NodeExecution;
import io.harness.execution.PlanExecution;
import io.harness.execution.PlanExecutionMetadata;
import io.harness.notification.PipelineEventType;
import io.harness.notification.bean.NotificationChannelWrapper;
import io.harness.notification.bean.NotificationRules;
import io.harness.notification.bean.PipelineEvent;
import io.harness.notification.channeldetails.NotificationChannel;
import io.harness.notification.notificationclient.NotificationClient;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.pms.pipeline.yaml.BasicPipeline;
import io.harness.pms.plan.execution.service.PMSExecutionService;
import io.harness.pms.yaml.YamlUtils;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
public class NotificationHelper {
  public static final String DEFAULT_TIME_FORMAT = "MMM dd' 'hh:mm a z";

  @Inject private PMSExecutionService pmsExecutionService;
  @Inject NotificationClient notificationClient;
  @Inject PlanExecutionService planExecutionService;
  @Inject PipelineServiceConfiguration pipelineServiceConfiguration;
  @Inject PlanExecutionMetadataService planExecutionMetadataService;

  public Optional<PipelineEventType> getEventTypeForStage(NodeExecution nodeExecution) {
    if (!OrchestrationUtils.isStageNode(nodeExecution)) {
      return Optional.empty();
    }
    if (nodeExecution.getStatus() == Status.SUCCEEDED) {
      return Optional.of(io.harness.notification.PipelineEventType.STAGE_SUCCESS);
    }
    if (StatusUtils.brokeStatuses().contains(nodeExecution.getStatus())) {
      return Optional.of(io.harness.notification.PipelineEventType.STAGE_FAILED);
    }
    return Optional.empty();
  }

  public void sendNotification(
      Ambiance ambiance, PipelineEventType pipelineEventType, NodeExecution nodeExecution, Long updatedAt) {
    String identifier = nodeExecution != null ? nodeExecution.getNode().getIdentifier() : "";
    String accountId = AmbianceUtils.getAccountId(ambiance);
    String orgIdentifier = AmbianceUtils.getOrgIdentifier(ambiance);
    String projectIdentifier = AmbianceUtils.getProjectIdentifier(ambiance);

    String yaml = obtainYaml(ambiance.getPlanExecutionId());
    if (EmptyPredicate.isEmpty(yaml)) {
      log.error("Empty yaml found in executionMetaData");
      return;
    }
    List<NotificationRules> notificationRules = null;
    try {
      notificationRules = getNotificationRulesFromYaml(yaml);
    } catch (IOException exception) {
      // Todo: throw Execution exception over here.
      log.error("", exception);
    }
    if (EmptyPredicate.isEmpty(notificationRules)) {
      return;
    }

    sendNotificationInternal(notificationRules, pipelineEventType, identifier, accountId,
        constructDummyTemplateData(ambiance, pipelineEventType, nodeExecution, identifier, updatedAt), orgIdentifier,
        projectIdentifier);
  }

  private void sendNotificationInternal(List<NotificationRules> notificationRulesList,
      PipelineEventType pipelineEventType, String identifier, String accountIdentifier, String notificationContent,
      String orgIdentifier, String projectIdentifier) {
    for (NotificationRules notificationRules : notificationRulesList) {
      if (!notificationRules.isEnabled()) {
        continue;
      }
      List<PipelineEvent> pipelineEvents = notificationRules.getPipelineEvents();
      boolean shouldSendNotification = shouldSendNotification(pipelineEvents, pipelineEventType, identifier);
      if (shouldSendNotification) {
        NotificationChannelWrapper wrapper = notificationRules.getNotificationChannelWrapper();
        String templateId = getNotificationTemplate(pipelineEventType.getLevel(), wrapper.getType());
        NotificationChannel channel = wrapper.getNotificationChannel().toNotificationChannel(accountIdentifier,
            orgIdentifier, projectIdentifier, templateId,
            ImmutableMap.of("message", notificationContent, "event_name", pipelineEventType.toString()));
        notificationClient.sendNotificationAsync(channel);
      }
    }
  }

  private String getNotificationTemplate(String level, String channelType) {
    return String.format("pms_%s_%s_plain", level.toLowerCase(), channelType.toLowerCase());
  }

  private boolean shouldSendNotification(List<PipelineEvent> pipelineEvents,
      io.harness.notification.PipelineEventType pipelineEventType, String identifier) {
    String pipelineEventTypeLevel = pipelineEventType.getLevel();
    for (PipelineEvent pipelineEvent : pipelineEvents) {
      io.harness.notification.PipelineEventType thisEventType = pipelineEvent.getType();
      if (thisEventType == io.harness.notification.PipelineEventType.ALL_EVENTS) {
        return true;
      } else if (thisEventType == pipelineEventType && pipelineEventTypeLevel.equals("Stage")) {
        List<String> stages = pipelineEvent.getForStages();
        if (stages.contains(identifier) || stages.contains("AllStages")) {
          return true;
        }
      } else if (thisEventType == pipelineEventType && !pipelineEventTypeLevel.equals("Stage")) {
        return true;
      }
    }
    return false;
  }

  private List<NotificationRules> getNotificationRulesFromYaml(String yaml) throws IOException {
    BasicPipeline basicPipeline = YamlUtils.read(yaml, BasicPipeline.class);
    return basicPipeline.getNotificationRules();
  }

  private String constructDummyTemplateData(Ambiance ambiance, PipelineEventType pipelineEventType,
      NodeExecution nodeExecution, String identifier, Long updatedAt) {
    PlanExecution planExecution = planExecutionService.get(ambiance.getPlanExecutionId());
    String projectId = AmbianceUtils.getProjectIdentifier(ambiance);
    String pipelineId = ambiance.getMetadata().getPipelineIdentifier();
    StringBuilder sb =
        new StringBuilder(128)
            .append("Pipeline Status Update\nEventType: ")
            .append(pipelineEventType.getDisplayName())
            .append("\nProject Id: ")
            .append(projectId)
            .append("\nPipeline: ")
            .append(pipelineId)
            .append("\nStarted At: ")
            .append(new SimpleDateFormat(DEFAULT_TIME_FORMAT).format(new Date(planExecution.getStartTs())));

    if (pipelineEventType.getLevel().equals("Stage") && nodeExecution != null) {
      sb.append("\nStage: ").append(identifier);
      sb.append("\nStatus: ").append(ExecutionStatus.getExecutionStatus(nodeExecution.getStatus()));
    } else if (pipelineEventType.getLevel().equals("Step") && nodeExecution != null) {
      sb.append("\nStep: ").append(identifier);
      sb.append("\nStatus: ").append(ExecutionStatus.getExecutionStatus(nodeExecution.getStatus()));
    } else {
      sb.append("\nStatus: ").append(ExecutionStatus.getExecutionStatus(planExecution.getStatus()));
    }

    if (StatusUtils.isFinalStatus(planExecution.getStatus()) && updatedAt != null) {
      sb.append("\nEnded At: ").append(new SimpleDateFormat(DEFAULT_TIME_FORMAT).format(new Date(updatedAt)));
    }
    sb.append("\n Link to Execution: ").append(generateUrl(ambiance));
    return sb.toString();
  }

  public String generateUrl(Ambiance ambiance) {
    String module = "cd";
    return String.format("%s/account/%s/%s/orgs/%s/projects/%s/pipelines/%s/executions/%s/pipeline",
        pipelineServiceConfiguration.getPipelineServiceBaseUrl(), AmbianceUtils.getAccountId(ambiance),
        StringUtils.defaultString(ambiance.getMetadata().getModuleType(), module),
        AmbianceUtils.getOrgIdentifier(ambiance), AmbianceUtils.getProjectIdentifier(ambiance),
        ambiance.getMetadata().getPipelineIdentifier(), ambiance.getPlanExecutionId());
  }

  private String obtainYaml(String planExecutionId) {
    Optional<PlanExecutionMetadata> optional = planExecutionMetadataService.findByPlanExecutionId(planExecutionId);
    return optional.map(PlanExecutionMetadata::getYaml).orElse(null);
  }
}
