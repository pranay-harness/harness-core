package software.wings.service.impl;

import static software.wings.beans.FailureNotification.Builder.aFailureNotification;
import static software.wings.beans.InformationNotification.Builder.anInformationNotification;
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

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionScope;
import software.wings.beans.FailureNotification;
import software.wings.beans.InformationNotification;
import software.wings.beans.NotificationRule;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.WorkflowExecution;
import software.wings.common.NotificationMessageResolver.NotificationMessageType;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.states.PhaseSubWorkflow;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Singleton;

/**
 * Created by anubhaw on 4/7/17.
 */
@Singleton
public class WorkflowNotificationHelper {
  @Inject private NotificationService notificationService;
  @Inject private WorkflowExecutionService workflowExecutionService;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  public void sendWorkflowStatusChangeNotification(ExecutionContext context, ExecutionStatus status) {
    List<NotificationRule> notificationRules =
        getNotificationApplicableToScope((ExecutionContextImpl) context, ExecutionScope.WORKFLOW, status);
    if (notificationRules == null || notificationRules.size() == 0) {
      return;
    }

    Environment env = ((ExecutionContextImpl) context).getEnv();
    Application app = ((ExecutionContextImpl) context).getApp();

    WorkflowExecution executionDetails =
        workflowExecutionService.getExecutionDetails(app.getUuid(), context.getWorkflowExecutionId());
    Map<String, String> placeHolders = new HashMap<>();
    placeHolders.put("WORKFLOW_NAME", context.getWorkflowExecutionName());
    placeHolders.put("ENV_NAME", env.getName());
    placeHolders.put("DATE", getDateString(executionDetails.getStartTs()));

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
                                                 .withNotificationTemplateVariables(placeHolders)
                                                 .build();
      notificationService.sendNotificationAsync(notification, notificationRules);
    } else if (status == FAILED || status == ABORTED) {
      FailureNotification notification = aFailureNotification()
                                             .withAccountId(app.getAccountId())
                                             .withAppId(app.getUuid())
                                             .withEnvironmentId(env.getUuid())
                                             .withEntityId(context.getWorkflowExecutionId())
                                             .withEntityType(EntityType.ORCHESTRATED_DEPLOYMENT)
                                             .withEntityName("Deployment")
                                             .withNotificationTemplateId(messageTemplate)
                                             .withNotificationTemplateVariables(placeHolders)
                                             .withExecutionId(context.getWorkflowExecutionId())
                                             .build();
      notificationService.sendNotificationAsync(notification, notificationRules);
    } else {
      logger.info("No template found for workflow status " + status);
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
      List<NotificationRule> notificationRules =
          ((CanaryOrchestrationWorkflow) orchestrationWorkflow).getNotificationRules();
      for (NotificationRule notificationRule : notificationRules) {
        if (executionScope.equals(notificationRule.getExecutionScope()) && notificationRule.getConditions() != null
            && notificationRule.getConditions().contains(status)) {
          filteredNotificationRules.add(notificationRule);
        }
      }
    }
    return filteredNotificationRules;
  }

  private String getDateString(Long startTs) {
    Date date = new Date(startTs);
    return date.toString(); // TODO:: format
  }

  public void sendWorkflowPhaseStatusChangeNotification(
      ExecutionContext context, ExecutionStatus status, PhaseSubWorkflow phaseSubWorkflow) {
    // TODO:: use phaseSubworkflow to send rollback notifications

    List<NotificationRule> notificationRules =
        getNotificationApplicableToScope((ExecutionContextImpl) context, ExecutionScope.WORKFLOW_PHASE, status);
    if (notificationRules == null || notificationRules.size() == 0) {
      return;
    }

    Environment env = ((ExecutionContextImpl) context).getEnv();
    Application app = ((ExecutionContextImpl) context).getApp();

    WorkflowExecution executionDetails =
        workflowExecutionService.getExecutionDetails(app.getUuid(), context.getWorkflowExecutionId());

    Map<String, String> placeHolders = new HashMap<>();
    placeHolders.put("WORKFLOW_NAME", context.getWorkflowExecutionName());
    placeHolders.put("PHASE_NAME", phaseSubWorkflow.getName());
    placeHolders.put("ENV_NAME", env.getName());
    placeHolders.put("DATE", getDateString(executionDetails.getStartTs()));

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
                                                 .withNotificationTemplateVariables(placeHolders)
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
              .withNotificationTemplateVariables(placeHolders)
              .withExecutionId(context.getWorkflowExecutionId())
              .build();
      notificationService.sendNotificationAsync(notification, notificationRules);
    } else {
      logger.info("No template found for workflow status " + status);
    }
  }
}
