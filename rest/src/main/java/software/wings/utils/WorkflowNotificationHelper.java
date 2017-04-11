package software.wings.utils;

import static software.wings.beans.FailureNotification.Builder.aFailureNotification;
import static software.wings.beans.InformationNotification.Builder.anInformationNotification;
import static software.wings.sm.ExecutionStatus.FAILED;
import static software.wings.sm.ExecutionStatus.PAUSED;
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
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.PhaseSubWorkflow;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
    List<NotificationRule> notificationRules = getNotificationApplicableToScope(context, ExecutionScope.WORKFLOW);
    if (notificationRules == null || notificationRules.size() == 0) {
      return;
    }

    WorkflowStandardParams stdParam = context.getContextElement(ContextElementType.STANDARD);
    Environment env = stdParam.getEnv();
    Application app = stdParam.getApp();

    WorkflowExecution executionDetails =
        workflowExecutionService.getExecutionDetails(context.getAppId(), context.getWorkflowExecutionId());
    Map<String, String> placeHolders = new HashMap<>();
    placeHolders.put("NAME", context.getWorkflowExecutionName());
    placeHolders.put("ENV_NAME", env.getName());
    placeHolders.put("DATE", getDateString(executionDetails.getEndTs()));

    if (status.equals(SUCCESS) || status.equals(PAUSED)) {
      String messageTemplate = status.equals(SUCCESS)
          ? NotificationMessageType.DEPLOYMENT_SUCCESSFUL_NOTIFICATION.name()
          : NotificationMessageType.DEPLOYMENT_PAUSED_NOTIFICATION.name();
      InformationNotification notification = anInformationNotification()
                                                 .withAccountId(app.getAccountId())
                                                 .withAppId(context.getAppId())
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
              .withNotificationTemplateId(NotificationMessageType.DEPLOYMENT_FAILED_NOTIFICATION.name())
              .withNotificationTemplateVariables(placeHolders)
              .withExecutionId(context.getWorkflowExecutionId())
              .build();
      notificationService.sendNotificationAsync(notification, notificationRules);
    } else {
      logger.info("No template found for workflow status " + status);
    }
  }

  private List<NotificationRule> getNotificationApplicableToScope(
      ExecutionContext context, ExecutionScope executionScope) {
    if (context instanceof ExecutionContextImpl) {
      OrchestrationWorkflow orchestrationWorkflow =
          ((ExecutionContextImpl) context).getStateMachine().getOrchestrationWorkflow();
      if (orchestrationWorkflow instanceof CanaryOrchestrationWorkflow) {
        List<NotificationRule> notificationRules =
            ((CanaryOrchestrationWorkflow) orchestrationWorkflow).getNotificationRules();
        return notificationRules.stream()
            .filter(notificationRule -> executionScope.equals(notificationRule.getExecutionScope()))
            .collect(Collectors.toList());
      }
    }
    return new ArrayList<>();
  }

  private String getDateString(Long startTs) {
    Date date = new Date(startTs);
    return date.toString(); // TODO:: format
  }

  public void sendWorkflowPhaseStatusChangeNotification(
      ExecutionContext context, ExecutionStatus status, PhaseSubWorkflow phaseSubWorkflow) {
    // TODO:: use phaseSubworkflow to send rollback notifications

    List<NotificationRule> notificationRules = getNotificationApplicableToScope(context, ExecutionScope.WORKFLOW_PHASE);
    if (notificationRules == null || notificationRules.size() == 0) {
      return;
    }

    WorkflowStandardParams stdParam = context.getContextElement(ContextElementType.STANDARD);
    Environment env = stdParam.getEnv();
    Application app = stdParam.getApp();
    WorkflowExecution executionDetails =
        workflowExecutionService.getExecutionDetails(context.getAppId(), context.getWorkflowExecutionId());

    Map<String, String> placeHolders = new HashMap<>();
    placeHolders.put("NAME", context.getWorkflowExecutionName());
    placeHolders.put("ENV_NAME", env.getName());
    placeHolders.put("DATE", getDateString(executionDetails.getEndTs()));

    if (status.equals(SUCCESS) || status.equals(PAUSED)) {
      String messageTemplate = status.equals(SUCCESS)
          ? NotificationMessageType.DEPLOYMENT_SUCCESSFUL_NOTIFICATION.name()
          : NotificationMessageType.DEPLOYMENT_PAUSED_NOTIFICATION.name();
      InformationNotification notification = anInformationNotification()
                                                 .withAccountId(app.getAccountId())
                                                 .withAppId(context.getAppId())
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
              .withNotificationTemplateId(NotificationMessageType.DEPLOYMENT_FAILED_NOTIFICATION.name())
              .withNotificationTemplateVariables(placeHolders)
              .withExecutionId(context.getWorkflowExecutionId())
              .build();
      notificationService.sendNotificationAsync(notification, notificationRules);
    } else {
      logger.info("No template found for workflow status " + status);
    }
  }
}
