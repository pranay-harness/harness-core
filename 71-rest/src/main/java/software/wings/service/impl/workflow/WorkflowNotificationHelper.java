package software.wings.service.impl.workflow;

import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.PAUSED;
import static io.harness.beans.ExecutionStatus.RESUMED;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.beans.OrchestrationWorkflowType.BUILD;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;
import static java.lang.String.format;
import static java.lang.String.join;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.ExecutionScope.WORKFLOW;
import static software.wings.beans.ExecutionScope.WORKFLOW_PHASE;
import static software.wings.beans.FailureNotification.Builder.aFailureNotification;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.WORKFLOW_NOTIFICATION;
import static software.wings.sm.StateType.PHASE;
import static software.wings.utils.Misc.getDurationString;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.OrchestrationWorkflowType;
import io.harness.context.ContextElementType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.wings.app.MainConfiguration;
import software.wings.beans.Application;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionScope;
import software.wings.beans.InformationNotification;
import software.wings.beans.Notification;
import software.wings.beans.NotificationGroup;
import software.wings.beans.NotificationRule;
import software.wings.beans.NotificationRule.NotificationRuleBuilder;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.security.UserGroup;
import software.wings.common.NotificationMessageResolver;
import software.wings.common.NotificationMessageResolver.NotificationMessageType;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateExecutionInstance.StateExecutionInstanceKeys;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.ApprovalState.ApprovalStateType;
import software.wings.sm.states.PhaseSubWorkflow;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * Created by anubhaw on 4/7/17.
 */
@Singleton
@Slf4j
public class WorkflowNotificationHelper {
  @Inject private WorkflowService workflowService;
  @Inject private NotificationService notificationService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private MainConfiguration configuration;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private Clock clock;
  @Inject private NotificationSetupService notificationSetupService;
  @Inject private UserGroupService userGroupService;
  @Inject private ArtifactStreamServiceBindingService artifactStreamServiceBindingService;

  private final DateFormat dateFormat = new SimpleDateFormat("MMM d");
  private final DateFormat timeFormat = new SimpleDateFormat("HH:mm z");

  public void sendWorkflowStatusChangeNotification(ExecutionContext context, ExecutionStatus status) {
    List<NotificationRule> notificationRules =
        obtainNotificationApplicableToScope((ExecutionContextImpl) context, WORKFLOW, status);
    if (isEmpty(notificationRules)) {
      return;
    }

    Application app = Objects.requireNonNull(((ExecutionContextImpl) context).getApp());
    Environment env = ((ExecutionContextImpl) context).getEnv();

    Map<String, String> placeHolderValues = getPlaceholderValues(context, app, env, status, null);

    Notification notification;
    if (status == SUCCESS || status == PAUSED || status == RESUMED) {
      notification = InformationNotification.builder()
                         .accountId(app.getAccountId())
                         .appId(app.getUuid())
                         .entityId(context.getWorkflowExecutionId())
                         .entityType(EntityType.ORCHESTRATED_DEPLOYMENT)
                         .notificationTemplateId(WORKFLOW_NOTIFICATION.name())
                         .notificationTemplateVariables(placeHolderValues)
                         .build();
    } else {
      notification = aFailureNotification()
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
    }

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    if (workflowStandardParams != null && workflowStandardParams.isNotifyTriggeredUserOnly()) {
      notificationService.sendNotificationToTriggeredByUserOnly(notification, workflowStandardParams.getCurrentUser());
    } else {
      notificationService.sendNotificationAsync(notification, notificationRules);
    }
  }

  public void sendWorkflowPhaseStatusChangeNotification(
      ExecutionContext context, ExecutionStatus status, PhaseSubWorkflow phaseSubWorkflow) {
    // TODO:: use phaseSubworkflow to send rollback notifications

    List<NotificationRule> notificationRules =
        obtainNotificationApplicableToScope((ExecutionContextImpl) context, WORKFLOW_PHASE, status);
    if (isEmpty(notificationRules)) {
      return;
    }

    Environment env = ((ExecutionContextImpl) context).getEnv();
    Application app = Objects.requireNonNull(((ExecutionContextImpl) context).getApp());

    Map<String, String> placeHolderValues = getPlaceholderValues(context, app, env, status, phaseSubWorkflow);

    Notification notification;
    if (status.equals(SUCCESS) || status.equals(PAUSED)) {
      notification = InformationNotification.builder()
                         .accountId(app.getAccountId())
                         .appId(app.getUuid())
                         .entityId(context.getWorkflowExecutionId())
                         .entityType(EntityType.ORCHESTRATED_DEPLOYMENT)
                         .notificationTemplateId(WORKFLOW_NOTIFICATION.name())
                         .notificationTemplateVariables(placeHolderValues)
                         .build();
    } else if (status.equals(FAILED)) {
      notification = aFailureNotification()
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
    } else {
      logger.info("No template found for workflow status " + status);
      return;
    }

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    if (workflowStandardParams != null && workflowStandardParams.isNotifyTriggeredUserOnly()) {
      notificationService.sendNotificationToTriggeredByUserOnly(notification, workflowStandardParams.getCurrentUser());
    } else {
      notificationService.sendNotificationAsync(notification, notificationRules);
    }
  }

  public void sendApprovalNotification(String accountId, NotificationMessageType notificationMessageType,
      Map<String, String> placeHolderValues, ExecutionContext context, ApprovalStateType approvalStateType) {
    List<NotificationRule> rules = new LinkedList<>();

    Objects.requireNonNull(context, "Context can't be null. accountId=" + accountId);
    Objects.requireNonNull(context.getWorkflowType(), "workflow type can't be null. accountId=" + accountId);

    switch (context.getWorkflowType()) {
      case ORCHESTRATION:
        rules = obtainNotificationApplicableToScope(context, WORKFLOW, PAUSED);
        break;

      case PIPELINE:
        if (approvalStateType.equals(ApprovalStateType.USER_GROUP)) {
          break;
        }
        UserGroup defaultUserGroup = userGroupService.getDefaultUserGroup(accountId);
        if (null == defaultUserGroup) {
          logger.error("There is no default user group. accountId={}", accountId);
        } else {
          NotificationRule rule = NotificationRuleBuilder.aNotificationRule()
                                      .withUserGroupIds(Collections.singletonList(defaultUserGroup.getUuid()))
                                      .build();

          rules.add(rule);
        }
        break;

      default:
        throw new IllegalArgumentException("Unknown workflow type: " + context.getWorkflowType());
    }

    InformationNotification notification = InformationNotification.builder()
                                               .appId(GLOBAL_APP_ID)
                                               .accountId(accountId)
                                               .notificationTemplateId(notificationMessageType.name())
                                               .notificationTemplateVariables(placeHolderValues)
                                               .build();

    notificationService.sendNotificationAsync(notification, rules);
  }

  List<NotificationRule> obtainNotificationApplicableToScope(
      ExecutionContext context, ExecutionScope executionScope, ExecutionStatus status) {
    if (ExecutionStatus.isNegativeStatus(status)) {
      status = FAILED;
    } else if (status == RESUMED) {
      status = PAUSED;
    }

    List<NotificationRule> filteredNotificationRules = new ArrayList<>();
    final Workflow workflow = workflowService.readWorkflow(context.getAppId(), context.getWorkflowId());

    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();

    if (orchestrationWorkflow instanceof CanaryOrchestrationWorkflow) {
      List<NotificationRule> notificationRules = orchestrationWorkflow.getNotificationRules();
      for (NotificationRule notificationRule : notificationRules) {
        boolean shouldNotify = executionScope.equals(notificationRule.getExecutionScope())
            && notificationRule.getConditions() != null && notificationRule.getConditions().contains(status);

        if (shouldNotify) {
          filteredNotificationRules.add(renderExpressions(context, notificationRule));
        }
      }
    }
    return filteredNotificationRules;
  }

  public NotificationRule renderExpressions(ExecutionContext context, NotificationRule notificationRule) {
    if (notificationRule.isNotificationGroupAsExpression()) {
      renderNotificationGroups(context, notificationRule);
    }

    if (notificationRule.isUserGroupAsExpression()) {
      renderUserGroups(context, notificationRule);
    }

    return notificationRule;
  }

  private void renderNotificationGroups(ExecutionContext context, NotificationRule notificationRule) {
    if (!notificationRule.isNotificationGroupAsExpression()) {
      return;
    }

    List<NotificationGroup> renderedNotificationGroups = new ArrayList<>();
    List<NotificationGroup> notificationGroups = notificationRule.getNotificationGroups();
    for (NotificationGroup notificationGroup : notificationGroups) {
      for (String notificationGroupName : context.renderExpression(notificationGroup.getName()).split(",")) {
        NotificationGroup renderedNotificationGroup = notificationSetupService.readNotificationGroupByName(
            context.getApp().getAccountId(), notificationGroupName.trim());
        if (renderedNotificationGroup != null) {
          renderedNotificationGroups.add(renderedNotificationGroup);
        }
      }
    }
    notificationRule.setNotificationGroups(renderedNotificationGroups);
  }

  private void renderUserGroups(ExecutionContext context, NotificationRule notificationRule) {
    notNullCheck("Invalid notificationRule", notificationRule);
    if (!notificationRule.isUserGroupAsExpression()) {
      return;
    }

    String accountId = context.getApp().getAccountId();
    if (StringUtils.isEmpty(accountId)) {
      logger.error("Could not find accountId in context. User Groups can't be rendered. Context: {}", context.asMap());
    }

    String expr = notificationRule.getUserGroupExpression();
    String renderedExpression = context.renderExpression(expr);

    if (StringUtils.isEmpty(renderedExpression)) {
      logger.error("[EMPTY_EXPRESSION] Rendered express is: {}. Original Expression: {}, Context: {}",
          renderedExpression, expr, context.asMap());
      return;
    }

    List<String> userGroupNames =
        Arrays.stream(renderedExpression.split(",")).map(String::trim).collect(Collectors.toList());

    List<UserGroup> userGroups = userGroupService.listByName(accountId, userGroupNames);
    if (isNotEmpty(userGroups)) {
      List<String> userGroupIds = userGroups.stream().map(UserGroup::getUuid).collect(Collectors.toList());
      notificationRule.setUserGroupIds(userGroupIds);
    }
  }

  private Map<String, String> getPlaceholderValues(ExecutionContext context, Application app, Environment env,
      ExecutionStatus status, @Nullable PhaseSubWorkflow phaseSubWorkflow) {
    WorkflowExecution workflowExecution =
        workflowExecutionService.getExecutionDetails(app.getUuid(), context.getWorkflowExecutionId(), true);
    String triggeredBy = workflowExecution.getTriggeredBy().getName();
    if (triggeredBy.equalsIgnoreCase("Deployment trigger")) {
      triggeredBy = triggeredBy.toLowerCase();
    }
    long startTs = Optional.ofNullable(workflowExecution.getStartTs()).orElse(workflowExecution.getCreatedAt());
    long endTs = Optional.ofNullable(workflowExecution.getEndTs()).orElse(startTs);

    if (phaseSubWorkflow != null) {
      StateExecutionInstance stateExecutionInstance =
          wingsPersistence.createQuery(StateExecutionInstance.class)
              .filter(StateExecutionInstanceKeys.executionUuid, workflowExecution.getUuid())
              .filter(StateExecutionInstanceKeys.stateType, PHASE.name())
              .filter(StateExecutionInstanceKeys.displayName, phaseSubWorkflow.getName())
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
        String pipelineUrl = NotificationMessageResolver.buildAbsoluteUrl(configuration,
            format("/account/%s/app/%s/pipeline-execution/%s/workflow-execution/%s/details", app.getAccountId(),
                app.getUuid(), workflowExecution.getPipelineExecutionId(), context.getWorkflowExecutionId()));
        pipelineMsg = format(" as part of <<<%s|-|%s>>> pipeline", pipelineUrl, pipelineName);
      }
    }

    String startTime = format("%s at %s", dateFormat.format(new Date(startTs)), timeFormat.format(new Date(startTs)));
    String endTime = format("%s at %s", dateFormat.format(new Date(endTs)), timeFormat.format(new Date(endTs)));

    Map<String, String> placeHolderValues = new HashMap<>();
    placeHolderValues.put("WORKFLOW_NAME", context.getWorkflowExecutionName());
    placeHolderValues.put("WORKFLOW_URL", workflowUrl);
    placeHolderValues.put("VERB", NotificationMessageResolver.getStatusVerb(status));
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
    return NotificationMessageResolver.buildAbsoluteUrl(configuration,
        format("/account/%s/app/%s/env/%s/executions/%s/details", accountId, appId,
            BUILD.equals(type) ? "build" : environmentId, workflowExecutionId));
  }

  public String getArtifactsMessage(ExecutionContext context, WorkflowExecution workflowExecution, ExecutionScope scope,
      PhaseSubWorkflow phaseSubWorkflow) {
    List<String> serviceIds = new ArrayList<>();
    if (scope == WORKFLOW_PHASE) {
      serviceIds.add(phaseSubWorkflow.getServiceId());
    } else if (isNotEmpty(workflowExecution.getServiceIds())) {
      serviceIds.addAll(workflowExecution.getServiceIds());
    }

    Map<String, Artifact> artifactStreamIdArtifacts = new HashMap<>();
    List<Artifact> artifacts = ((ExecutionContextImpl) context).getArtifacts();
    if (isNotEmpty(artifacts)) {
      for (Artifact artifact : artifacts) {
        artifactStreamIdArtifacts.put(artifact.getArtifactStreamId(), artifact);
      }
    }

    List<String> serviceMsgs = new ArrayList<>();
    for (String serviceId : serviceIds) {
      StringBuilder serviceMsg = new StringBuilder();
      Service service = serviceResourceService.get(context.getAppId(), serviceId, false);
      notNullCheck("Service might have been deleted", service, USER);
      serviceMsg.append(service.getName()).append(": ");

      List<String> artifactStreamIds = artifactStreamServiceBindingService.listArtifactStreamIds(service);
      boolean found = false;
      if (isNotEmpty(artifactStreamIds)) {
        for (String artifactStreamId : artifactStreamIds) {
          if (artifactStreamIdArtifacts.containsKey(artifactStreamId)) {
            Artifact artifact = artifactStreamIdArtifacts.get(artifactStreamId);
            serviceMsg.append(artifact.getArtifactSourceName())
                .append(" (build# ")
                .append((artifact.getBuildNo() == null) ? "unknown" : artifact.getBuildNo().replaceAll("\\*", "٭"))
                .append(')');
            found = true;
            break;
          }
        }
      }

      if (!found) {
        serviceMsg.append("no artifact");
      }

      serviceMsgs.add(serviceMsg.toString());
    }

    String artifactsMsg = "no services";
    if (isNotEmpty(serviceMsgs)) {
      artifactsMsg = join(", ", serviceMsgs);
    }
    return artifactsMsg;
  }
}
