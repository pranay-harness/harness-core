package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.ExecutionScope.WORKFLOW;
import static software.wings.beans.ExecutionScope.WORKFLOW_PHASE;
import static software.wings.beans.FailureNotification.Builder.aFailureNotification;
import static software.wings.beans.InformationNotification.Builder.anInformationNotification;
import static software.wings.beans.OrchestrationWorkflowType.BUILD;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.WORKFLOW_ABORTED_NOTIFICATION;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.WORKFLOW_FAILED_NOTIFICATION;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.WORKFLOW_PAUSED_NOTIFICATION;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.WORKFLOW_RESUMED_NOTIFICATION;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.WORKFLOW_SUCCESSFUL_NOTIFICATION;
import static software.wings.sm.ExecutionStatus.ABORTED;
import static software.wings.sm.ExecutionStatus.ERROR;
import static software.wings.sm.ExecutionStatus.FAILED;
import static software.wings.sm.ExecutionStatus.PAUSED;
import static software.wings.sm.ExecutionStatus.RESUMED;
import static software.wings.sm.ExecutionStatus.SUCCESS;
import static software.wings.utils.Switch.unhandled;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.Base;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionScope;
import software.wings.beans.FailureNotification;
import software.wings.beans.InformationNotification;
import software.wings.beans.NotificationRule;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.common.NotificationMessageResolver.NotificationMessageType;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.states.PhaseSubWorkflow;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by anubhaw on 4/7/17.
 */
@Singleton
public class WorkflowNotificationHelper {
  private static final Logger logger = LoggerFactory.getLogger(WorkflowNotificationHelper.class);

  @Inject private NotificationService notificationService;
  @Inject private WorkflowService workflowService;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private Clock clock;

  private final DateFormat dateFormat = new SimpleDateFormat("MMM d HH:mm z");

  public void sendWorkflowStatusChangeNotification(ExecutionContext context, ExecutionStatus status) {
    List<NotificationRule> notificationRules =
        getNotificationApplicableToScope((ExecutionContextImpl) context, WORKFLOW, status);
    if (isEmpty(notificationRules)) {
      return;
    }

    Environment env = ((ExecutionContextImpl) context).getEnv();
    Application app = ((ExecutionContextImpl) context).getApp();

    WorkflowExecution workflowExecution =
        workflowExecutionService.getExecutionDetails(app.getUuid(), context.getWorkflowExecutionId());
    EmbeddedUser triggeredBy = workflowExecution.getTriggeredBy();
    String userName = triggeredBy != null ? triggeredBy.getName() : "deployment trigger";

    Map<String, String> placeHolderValues = new HashMap<>();
    placeHolderValues.put("WORKFLOW_NAME", context.getWorkflowExecutionName());
    placeHolderValues.put("ARTIFACTS", getArtifactsMessage(context, WORKFLOW, null));
    if (!BUILD.equals(context.getOrchestrationWorkflowType())) {
      placeHolderValues.put("ENV_NAME", env.getName());
    }
    placeHolderValues.put("USER_NAME", userName);
    placeHolderValues.put("DATE", getDateString());

    String messageTemplate = null;

    switch (status) {
      case SUCCESS:
        messageTemplate = WORKFLOW_SUCCESSFUL_NOTIFICATION.name();
        break;
      case FAILED:
        messageTemplate = WORKFLOW_FAILED_NOTIFICATION.name();
        break;
      case PAUSED:
        messageTemplate = WORKFLOW_PAUSED_NOTIFICATION.name();
        break;
      case RESUMED:
        messageTemplate = WORKFLOW_RESUMED_NOTIFICATION.name();
        break;
      case ABORTED:
        messageTemplate = WORKFLOW_ABORTED_NOTIFICATION.name();
        break;
      default:
        unhandled(status);
    }
    if (messageTemplate == null) {
      logger.error("No messageTemplate found for notification, status={}", status);
      return;
    }
    if (status == SUCCESS || status == PAUSED || status == RESUMED) {
      InformationNotification notification = anInformationNotification()
                                                 .withAccountId(app.getAccountId())
                                                 .withAppId(context.getAppId())
                                                 .withEntityId(context.getWorkflowExecutionId())
                                                 .withEntityType(EntityType.ORCHESTRATED_DEPLOYMENT)
                                                 .withNotificationTemplateId(messageTemplate)
                                                 .withNotificationTemplateVariables(placeHolderValues)
                                                 .build();
      notificationService.sendNotificationAsync(notification, notificationRules);
    } else {
      FailureNotification notification = aFailureNotification()
                                             .withAccountId(app.getAccountId())
                                             .withAppId(app.getUuid())
                                             .withEnvironmentId(env == null ? null : env.getUuid())
                                             .withEntityId(context.getWorkflowExecutionId())
                                             .withEntityType(EntityType.ORCHESTRATED_DEPLOYMENT)
                                             .withEntityName("Deployment")
                                             .withNotificationTemplateId(messageTemplate)
                                             .withNotificationTemplateVariables(placeHolderValues)
                                             .withExecutionId(context.getWorkflowExecutionId())
                                             .build();
      notificationService.sendNotificationAsync(notification, notificationRules);
    }
  }

  private List<NotificationRule> getNotificationApplicableToScope(
      ExecutionContextImpl context, ExecutionScope executionScope, ExecutionStatus status) {
    if (status == FAILED || status == ERROR || status == ABORTED) {
      status = FAILED;
    }

    List<NotificationRule> filteredNotificationRules = new ArrayList<>();
    OrchestrationWorkflow orchestrationWorkflow = context.getStateMachine().getOrchestrationWorkflow();
    if (orchestrationWorkflow instanceof CanaryOrchestrationWorkflow) {
      List<NotificationRule> notificationRules = orchestrationWorkflow.getNotificationRules();
      for (NotificationRule notificationRule : notificationRules) {
        if (executionScope.equals(notificationRule.getExecutionScope()) && notificationRule.getConditions() != null
            && notificationRule.getConditions().contains(status)) {
          filteredNotificationRules.add(notificationRule);
        }
      }
    }
    return filteredNotificationRules;
  }

  private String getDateString() {
    return dateFormat.format(Date.from(clock.instant()));
  }

  public void sendWorkflowPhaseStatusChangeNotification(
      ExecutionContext context, ExecutionStatus status, PhaseSubWorkflow phaseSubWorkflow) {
    // TODO:: use phaseSubworkflow to send rollback notifications

    List<NotificationRule> notificationRules =
        getNotificationApplicableToScope((ExecutionContextImpl) context, WORKFLOW_PHASE, status);
    if (isEmpty(notificationRules)) {
      return;
    }

    Environment env = ((ExecutionContextImpl) context).getEnv();
    Application app = ((ExecutionContextImpl) context).getApp();

    WorkflowExecution workflowExecution =
        workflowExecutionService.getExecutionDetails(app.getUuid(), context.getWorkflowExecutionId());
    EmbeddedUser triggeredBy = workflowExecution.getTriggeredBy();
    String userName = triggeredBy != null ? triggeredBy.getName() : "deployment trigger";

    Map<String, String> placeHolderValues = new HashMap<>();
    placeHolderValues.put("WORKFLOW_NAME", context.getWorkflowExecutionName());
    placeHolderValues.put("PHASE_NAME", phaseSubWorkflow.getName());
    placeHolderValues.put("ARTIFACTS", getArtifactsMessage(context, WORKFLOW_PHASE, phaseSubWorkflow));
    placeHolderValues.put("ENV_NAME", env.getName());
    placeHolderValues.put("USER_NAME", userName);
    placeHolderValues.put("DATE", getDateString());

    if (status.equals(SUCCESS) || status.equals(PAUSED)) {
      String messageTemplate = status.equals(SUCCESS)
          ? NotificationMessageType.WORKFLOW_PHASE_SUCCESSFUL_NOTIFICATION.name()
          : NotificationMessageType.WORKFLOW_PHASE_PAUSED_NOTIFICATION.name();
      InformationNotification notification = anInformationNotification()
                                                 .withAccountId(app.getAccountId())
                                                 .withAppId(context.getAppId())
                                                 .withEntityId(context.getWorkflowExecutionId())
                                                 .withEntityType(EntityType.ORCHESTRATED_DEPLOYMENT)
                                                 .withNotificationTemplateId(messageTemplate)
                                                 .withNotificationTemplateVariables(placeHolderValues)
                                                 .build();
      notificationService.sendNotificationAsync(notification, notificationRules);
    } else if (status.equals(FAILED)) {
      FailureNotification notification =
          aFailureNotification()
              .withAccountId(app.getAccountId())
              .withAppId(app.getUuid())
              .withEnvironmentId(env.getUuid())
              .withEntityId(context.getWorkflowExecutionId())
              .withEntityType(EntityType.ORCHESTRATED_DEPLOYMENT)
              .withEntityName("Deployment")
              .withNotificationTemplateId(NotificationMessageType.WORKFLOW_PHASE_FAILED_NOTIFICATION.name())
              .withNotificationTemplateVariables(placeHolderValues)
              .withExecutionId(context.getWorkflowExecutionId())
              .build();
      notificationService.sendNotificationAsync(notification, notificationRules);
    } else {
      logger.info("No template found for workflow status " + status);
    }
  }

  private String getArtifactsMessage(
      ExecutionContext context, ExecutionScope scope, PhaseSubWorkflow phaseSubWorkflow) {
    List<String> serviceIds = scope == WORKFLOW_PHASE
        ? singletonList(phaseSubWorkflow.getServiceId())
        : workflowService.readWorkflow(context.getAppId(), context.getWorkflowId())
              .getServices()
              .stream()
              .map(Base::getUuid)
              .collect(toList());
    List<Artifact> artifacts = ((ExecutionContextImpl) context).getArtifacts();
    String artifactsMsg = "no artifacts";
    if (isNotEmpty(artifacts)) {
      List<Artifact> relatedArtifacts =
          artifacts.stream()
              .filter(artifact -> artifact.getServiceIds().stream().anyMatch(serviceIds::contains))
              .collect(toList());
      artifactsMsg = Joiner.on(", ").join(
          relatedArtifacts.stream()
              .map(artifact -> artifact.getArtifactSourceName() + " (build# " + artifact.getBuildNo() + ")")
              .collect(toList()));
    }
    return artifactsMsg;
  }
}
