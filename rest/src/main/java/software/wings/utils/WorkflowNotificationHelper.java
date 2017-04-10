package software.wings.utils;

import static software.wings.beans.FailureNotification.Builder.aFailureNotification;
import static software.wings.beans.InformationNotification.Builder.anInformationNotification;
import static software.wings.common.NotificationMessageResolver.DEPLOYMENT_PAUSED_NOTIFICATION;
import static software.wings.common.NotificationMessageResolver.DEPLOYMENT_SUCCESSFUL_NOTIFICATION;
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
import software.wings.beans.FailureNotification;
import software.wings.beans.InformationNotification;
import software.wings.beans.NotificationRule;
import software.wings.beans.WorkflowExecution;
import software.wings.common.NotificationMessageResolver;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.WorkflowStandardParams;

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
    WorkflowStandardParams stdParam = context.getContextElement(ContextElementType.STANDARD);
    Environment env = stdParam.getEnv();
    Application app = stdParam.getApp();
    WorkflowExecution executionDetails =
        workflowExecutionService.getExecutionDetails(context.getAppId(), context.getWorkflowExecutionId());
    List<NotificationRule> notificationRules;
    try {
      notificationRules =
          ((CanaryOrchestrationWorkflow) ((ExecutionContextImpl) context).getStateMachine().getOrchestrationWorkflow())
              .getNotificationRules(); // TODO:: get rid of try catch and find a better way to get notification rule
    } catch (ClassCastException ccex) {
      ccex.printStackTrace();
      return;
    }

    Map<String, String> placeHolders = new HashMap<>();
    placeHolders.put("NAME", context.getWorkflowExecutionName());
    placeHolders.put("ENV_NAME", env.getName());
    placeHolders.put("DATE", getDateString(executionDetails.getEndTs()));

    if (status.equals(SUCCESS) || status.equals(PAUSED)) {
      String messageTemplate =
          status.equals(SUCCESS) ? DEPLOYMENT_SUCCESSFUL_NOTIFICATION : DEPLOYMENT_PAUSED_NOTIFICATION;
      String decoratedNotificationMessage =
          NotificationMessageResolver.getDecoratedNotificationMessage(DEPLOYMENT_SUCCESSFUL_NOTIFICATION, placeHolders);
      InformationNotification notification = anInformationNotification()
                                                 .withAccountId(app.getAccountId())
                                                 .withAppId(context.getAppId())
                                                 .withDisplayText(decoratedNotificationMessage)
                                                 .build();
      notificationService.sendNotificationAsync(notification, notificationRules);
    } else if (status.equals(FAILED)) {
      FailureNotification notification = aFailureNotification()
                                             .withAccountId(app.getAccountId())
                                             .withAppId(app.getUuid())
                                             .withEnvironmentId(executionDetails.getEnvId())
                                             .withEntityId(executionDetails.getUuid())
                                             .withEntityType(EntityType.ORCHESTRATED_DEPLOYMENT)
                                             .withEntityName("Deployment")
                                             .withExecutionId(executionDetails.getUuid())
                                             .build();
      notificationService.sendNotificationAsync(notification, notificationRules);
    } else {
      logger.info("No template found for workflow status " + status);
    }
  }

  private String getDateString(Long startTs) {
    Date date = new Date(startTs);
    return date.toString(); // TODO:: format
  }
}
