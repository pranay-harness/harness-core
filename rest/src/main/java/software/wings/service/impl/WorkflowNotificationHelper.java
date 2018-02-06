package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
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
import static software.wings.sm.StateType.PHASE;
import static software.wings.utils.Switch.unhandled;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.MainConfiguration;
import software.wings.beans.Application;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionScope;
import software.wings.beans.FailureNotification;
import software.wings.beans.InformationNotification;
import software.wings.beans.NotificationRule;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.Service;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.common.NotificationMessageResolver.NotificationMessageType;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.states.PhaseSubWorkflow;

import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

/**
 * Created by anubhaw on 4/7/17.
 */
@Singleton
public class WorkflowNotificationHelper {
  private static final Logger logger = LoggerFactory.getLogger(WorkflowNotificationHelper.class);

  @Inject private NotificationService notificationService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private MainConfiguration configuration;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private Clock clock;

  private final DateFormat dateFormat = new SimpleDateFormat("MMM d");
  private final DateFormat timeFormat = new SimpleDateFormat("HH:mm z");

  public void sendWorkflowStatusChangeNotification(ExecutionContext context, ExecutionStatus status) {
    List<NotificationRule> notificationRules =
        getNotificationApplicableToScope((ExecutionContextImpl) context, WORKFLOW, status);
    if (isEmpty(notificationRules)) {
      return;
    }

    Application app = ((ExecutionContextImpl) context).getApp();
    Environment env = ((ExecutionContextImpl) context).getEnv();

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

    Map<String, String> placeHolderValues = getPlaceholderValues(context, app, env, null);

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
      FailureNotification notification =
          aFailureNotification()
              .withAccountId(app.getAccountId())
              .withAppId(app.getUuid())
              .withEnvironmentId(BUILD.equals(context.getOrchestrationWorkflowType()) ? null : env.getUuid())
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

    Map<String, String> placeHolderValues = getPlaceholderValues(context, app, env, phaseSubWorkflow);

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

  private List<NotificationRule> getNotificationApplicableToScope(
      ExecutionContextImpl context, ExecutionScope executionScope, ExecutionStatus status) {
    if (status == FAILED || status == ERROR || status == ABORTED) {
      status = FAILED;
    }
    if (status == RESUMED) {
      status = PAUSED;
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

  private Map<String, String> getPlaceholderValues(
      ExecutionContext context, Application app, Environment env, @Nullable PhaseSubWorkflow phaseSubWorkflow) {
    WorkflowExecution workflowExecution =
        workflowExecutionService.getExecutionDetails(app.getUuid(), context.getWorkflowExecutionId());
    String triggeredBy = workflowExecution.getTriggeredBy().getName();
    if (triggeredBy.equalsIgnoreCase("Deployment trigger")) {
      triggeredBy = triggeredBy.toLowerCase();
    }
    long startTs = Optional.ofNullable(workflowExecution.getStartTs()).orElse(workflowExecution.getCreatedAt());
    long endTs = Optional.ofNullable(workflowExecution.getEndTs()).orElse(workflowExecution.getLastUpdatedAt());

    if (phaseSubWorkflow != null) {
      StateExecutionInstance stateExecutionInstance = wingsPersistence.createQuery(StateExecutionInstance.class)
                                                          .field("executionUuid")
                                                          .equal(workflowExecution.getUuid())
                                                          .field("stateType")
                                                          .equal(PHASE.name())
                                                          .field("stateName")
                                                          .equal(phaseSubWorkflow.getName())
                                                          .get();
      if (stateExecutionInstance != null) {
        startTs =
            Optional.ofNullable(stateExecutionInstance.getStartTs()).orElse(stateExecutionInstance.getCreatedAt());
        endTs =
            Optional.ofNullable(stateExecutionInstance.getEndTs()).orElse(stateExecutionInstance.getLastUpdatedAt());
      }
    }

    if (endTs == startTs) {
      endTs = clock.millis();
    }

    String workflowUrl =
        buildAbsoluteUrl(String.format("/account/%s/app/%s/env/%s/executions/%s/details", app.getAccountId(),
            app.getUuid(), BUILD.equals(context.getOrchestrationWorkflowType()) ? "build" : env.getUuid(),
            context.getWorkflowExecutionId()));

    String pipelineMsg = "";
    if (workflowExecution.getPipelineExecutionId() != null) {
      String pipelineName = workflowExecution.getPipelineSummary().getPipelineName();
      if (isNotBlank(pipelineName)) {
        String pipelineUrl = buildAbsoluteUrl(String.format("/account/%s/app/%s/deployments/%s/details",
            app.getAccountId(), app.getUuid(), workflowExecution.getPipelineExecutionId()));
        pipelineMsg = String.format(" as part of <<<%s|-|%s>>> pipeline", pipelineUrl, pipelineName);
      }
    }

    String startTime =
        String.format("%s at %s", dateFormat.format(new Date(startTs)), timeFormat.format(new Date(startTs)));
    String endTime = timeFormat.format(new Date(endTs));

    Map<String, String> placeHolderValues = new HashMap<>();
    placeHolderValues.put("WORKFLOW_NAME", context.getWorkflowExecutionName());
    placeHolderValues.put("WORKFLOW_URL", workflowUrl);
    placeHolderValues.put("USER_NAME", triggeredBy);
    placeHolderValues.put("PIPELINE", pipelineMsg);
    placeHolderValues.put("APP_NAME", app.getName());
    placeHolderValues.put("START_TS_SECS", Long.toString(startTs / 1000L));
    placeHolderValues.put("END_TS_SECS", Long.toString(endTs / 1000L));
    placeHolderValues.put("START_DATE", startTime);
    placeHolderValues.put("END_DATE", endTime);
    placeHolderValues.put("DURATION", getDurationString(startTs, endTs));
    placeHolderValues.put(
        "ENV_NAME", BUILD.equals(context.getOrchestrationWorkflowType()) ? "no environment" : env.getName());
    if (phaseSubWorkflow != null) {
      placeHolderValues.put("PHASE_NAME", phaseSubWorkflow.getName());
      placeHolderValues.put(
          "ARTIFACTS", getArtifactsMessage(context, workflowExecution, WORKFLOW_PHASE, phaseSubWorkflow));
    } else {
      placeHolderValues.put("ARTIFACTS", getArtifactsMessage(context, workflowExecution, WORKFLOW, null));
    }
    return placeHolderValues;
  }

  private String buildAbsoluteUrl(String fragment) {
    String baseUrl = configuration.getPortal().getUrl().trim();
    if (!baseUrl.endsWith("/")) {
      baseUrl += "/";
    }
    try {
      URIBuilder uriBuilder = new URIBuilder(baseUrl);
      uriBuilder.setFragment(fragment);
      return uriBuilder.toString();
    } catch (URISyntaxException e) {
      logger.error("Bad URI syntax", e);
      return baseUrl;
    }
  }

  private String getArtifactsMessage(ExecutionContext context, WorkflowExecution workflowExecution,
      ExecutionScope scope, PhaseSubWorkflow phaseSubWorkflow) {
    List<String> serviceIds = new ArrayList<>();
    if (scope == WORKFLOW_PHASE) {
      serviceIds.add(phaseSubWorkflow.getServiceId());
    } else if (isNotEmpty(workflowExecution.getServiceIds())) {
      serviceIds.addAll(workflowExecution.getServiceIds());
    }

    Map<String, Artifact> serviceIdArtifacts = new HashMap<>();

    List<Artifact> artifacts = ((ExecutionContextImpl) context).getArtifacts();
    if (isNotEmpty(artifacts)) {
      for (Artifact artifact : artifacts) {
        for (String serviceId : artifact.getServiceIds()) {
          serviceIdArtifacts.put(serviceId, artifact);
        }
      }
    }

    List<String> serviceMsgs = new ArrayList<>();
    for (String serviceId : serviceIds) {
      StringBuilder serviceMsg = new StringBuilder();
      Service service = serviceResourceService.get(context.getAppId(), serviceId);
      serviceMsg.append(service.getName()).append(": ");
      if (serviceIdArtifacts.containsKey(serviceId)) {
        Artifact artifact = serviceIdArtifacts.get(serviceId);
        serviceMsg.append(artifact.getArtifactSourceName())
            .append(" (build# ")
            .append(artifact.getBuildNo())
            .append(')');
      } else {
        serviceMsg.append("no artifact");
      }
      serviceMsgs.add(serviceMsg.toString());
    }

    String artifactsMsg = "no services";
    if (isNotEmpty(serviceMsgs)) {
      artifactsMsg = Joiner.on(", ").join(serviceMsgs);
    }
    return artifactsMsg;
  }

  private String getDurationString(long start, long end) {
    long duration = end - start;
    long elapsedHours = duration / TimeUnit.HOURS.toMillis(1);
    duration = duration % TimeUnit.HOURS.toMillis(1);

    long elapsedMinutes = duration / TimeUnit.MINUTES.toMillis(1);
    duration = duration % TimeUnit.MINUTES.toMillis(1);

    long elapsedSeconds = duration / TimeUnit.SECONDS.toMillis(1);

    StringBuilder elapsed = new StringBuilder();

    if (elapsedHours > 0) {
      elapsed.append(elapsedHours).append('h');
    }
    if (elapsedMinutes > 0) {
      if (isNotEmpty(elapsed.toString())) {
        elapsed.append(' ');
      }
      elapsed.append(elapsedMinutes).append('m');
    }
    if (elapsedSeconds > 0) {
      if (isNotEmpty(elapsed.toString())) {
        elapsed.append(' ');
      }
      elapsed.append(elapsedSeconds).append('s');
    }

    if (isEmpty(elapsed.toString())) {
      elapsed.append("0s");
    }

    return elapsed.toString();
  }
}
