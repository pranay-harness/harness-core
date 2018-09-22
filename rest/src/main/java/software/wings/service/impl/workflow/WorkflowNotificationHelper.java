package software.wings.service.impl.workflow;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.govern.Switch.unhandled;
import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.ExecutionScope.WORKFLOW;
import static software.wings.beans.ExecutionScope.WORKFLOW_PHASE;
import static software.wings.beans.FailureNotification.Builder.aFailureNotification;
import static software.wings.beans.InformationNotification.Builder.anInformationNotification;
import static software.wings.beans.OrchestrationWorkflowType.BUILD;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.WORKFLOW_NOTIFICATION;
import static software.wings.sm.ExecutionStatus.FAILED;
import static software.wings.sm.ExecutionStatus.PAUSED;
import static software.wings.sm.ExecutionStatus.RESUMED;
import static software.wings.sm.ExecutionStatus.SUCCESS;
import static software.wings.sm.StateType.PHASE;
import static software.wings.utils.Misc.getDurationString;

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
import software.wings.beans.OrchestrationWorkflowType;
import software.wings.beans.Service;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
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

    Map<String, String> placeHolderValues = getPlaceholderValues(context, app, env, status, null);

    if (status == SUCCESS || status == PAUSED || status == RESUMED) {
      InformationNotification notification = anInformationNotification()
                                                 .withAccountId(app.getAccountId())
                                                 .withAppId(context.getAppId())
                                                 .withEntityId(context.getWorkflowExecutionId())
                                                 .withEntityType(EntityType.ORCHESTRATED_DEPLOYMENT)
                                                 .withNotificationTemplateId(WORKFLOW_NOTIFICATION.name())
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
              .withNotificationTemplateId(WORKFLOW_NOTIFICATION.name())
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

    Map<String, String> placeHolderValues = getPlaceholderValues(context, app, env, status, phaseSubWorkflow);

    if (status.equals(SUCCESS) || status.equals(PAUSED)) {
      InformationNotification notification = anInformationNotification()
                                                 .withAccountId(app.getAccountId())
                                                 .withAppId(context.getAppId())
                                                 .withEntityId(context.getWorkflowExecutionId())
                                                 .withEntityType(EntityType.ORCHESTRATED_DEPLOYMENT)
                                                 .withNotificationTemplateId(WORKFLOW_NOTIFICATION.name())
                                                 .withNotificationTemplateVariables(placeHolderValues)
                                                 .build();
      notificationService.sendNotificationAsync(notification, notificationRules);
    } else if (status.equals(FAILED)) {
      FailureNotification notification = aFailureNotification()
                                             .withAccountId(app.getAccountId())
                                             .withAppId(app.getUuid())
                                             .withEnvironmentId(env.getUuid())
                                             .withEntityId(context.getWorkflowExecutionId())
                                             .withEntityType(EntityType.ORCHESTRATED_DEPLOYMENT)
                                             .withEntityName("Deployment")
                                             .withNotificationTemplateId(WORKFLOW_NOTIFICATION.name())
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
    if (ExecutionStatus.isNegativeStatus(status)) {
      status = FAILED;
    } else if (status == RESUMED) {
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

  private Map<String, String> getPlaceholderValues(ExecutionContext context, Application app, Environment env,
      ExecutionStatus status, @Nullable PhaseSubWorkflow phaseSubWorkflow) {
    WorkflowExecution workflowExecution =
        workflowExecutionService.getExecutionDetails(app.getUuid(), context.getWorkflowExecutionId(), true, emptySet());
    String triggeredBy = workflowExecution.getTriggeredBy().getName();
    if (triggeredBy.equalsIgnoreCase("Deployment trigger")) {
      triggeredBy = triggeredBy.toLowerCase();
    }
    long startTs = Optional.ofNullable(workflowExecution.getStartTs()).orElse(workflowExecution.getCreatedAt());
    long endTs = Optional.ofNullable(workflowExecution.getEndTs()).orElse(workflowExecution.getLastUpdatedAt());

    if (phaseSubWorkflow != null) {
      StateExecutionInstance stateExecutionInstance = wingsPersistence.createQuery(StateExecutionInstance.class)
                                                          .filter("executionUuid", workflowExecution.getUuid())
                                                          .filter("stateType", PHASE.name())
                                                          .filter("displayName", phaseSubWorkflow.getName())
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

    String workflowUrl = calculateWorkflowUrl(context.getWorkflowExecutionId(), context.getOrchestrationWorkflowType(),
        app.getAccountId(), app.getUuid(), env == null ? null : env.getUuid());

    String pipelineMsg = "";
    if (workflowExecution.getPipelineExecutionId() != null) {
      String pipelineName = workflowExecution.getPipelineSummary().getPipelineName();
      if (isNotBlank(pipelineName)) {
        String pipelineUrl = buildAbsoluteUrl(
            format("/account/%s/app/%s/pipeline-execution/%s/workflow-execution/%s/details", app.getAccountId(),
                app.getUuid(), workflowExecution.getPipelineExecutionId(), context.getWorkflowExecutionId()));
        pipelineMsg = format(" as part of <<<%s|-|%s>>> pipeline", pipelineUrl, pipelineName);
      }
    }

    String startTime = format("%s at %s", dateFormat.format(new Date(startTs)), timeFormat.format(new Date(startTs)));
    String endTime = timeFormat.format(new Date(endTs));

    Map<String, String> placeHolderValues = new HashMap<>();
    placeHolderValues.put("WORKFLOW_NAME", context.getWorkflowExecutionName());
    placeHolderValues.put("WORKFLOW_URL", workflowUrl);
    placeHolderValues.put("VERB", getStatusVerb(status));
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
      placeHolderValues.put("PHASE_NAME", phaseSubWorkflow.getName() + " of ");
      placeHolderValues.put(
          "ARTIFACTS", getArtifactsMessage(context, workflowExecution, WORKFLOW_PHASE, phaseSubWorkflow));
    } else {
      placeHolderValues.put("PHASE_NAME", "");
      placeHolderValues.put("ARTIFACTS", getArtifactsMessage(context, workflowExecution, WORKFLOW, null));
    }
    return placeHolderValues;
  }

  public String calculateWorkflowUrl(String workflowExecutionId, OrchestrationWorkflowType type, String accountId,
      String appId, String environmentId) {
    return buildAbsoluteUrl(format("/account/%s/app/%s/env/%s/executions/%s/details", accountId, appId,
        BUILD.equals(type) ? "build" : environmentId, workflowExecutionId));
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

  private String getStatusVerb(ExecutionStatus status) {
    switch (status) {
      case SUCCESS:
        return "completed";
      case FAILED:
      case ERROR:
        return "failed";
      case PAUSED:
        return "paused";
      case RESUMED:
        return "resumed";
      case ABORTED:
        return "aborted";
      case REJECTED:
        return "rejected";
      case EXPIRED:
        return "expired";
      default:
        unhandled(status);
        return "failed";
    }
  }

  public String getArtifactsMessage(ExecutionContext context, WorkflowExecution workflowExecution, ExecutionScope scope,
      PhaseSubWorkflow phaseSubWorkflow) {
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
            .append(artifact.getBuildNo().replaceAll("\\*", "٭"))
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
}
