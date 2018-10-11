package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.govern.Switch.unhandled;
import static java.util.Arrays.asList;
import static software.wings.beans.NotificationRule.NotificationRuleBuilder.aNotificationRule;
import static software.wings.common.Constants.DEFAULT_ASYNC_CALL_TIMEOUT;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.RESOURCE_CONSTRAINT_BLOCKED_NOTIFICATION;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.RESOURCE_CONSTRAINT_UNBLOCKED_NOTIFICATION;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.states.ResourceConstraintState.HoldingScope.WORKFLOW;

import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.distribution.constraint.Constraint;
import io.harness.distribution.constraint.ConstraintException;
import io.harness.distribution.constraint.Consumer;
import io.harness.distribution.constraint.ConsumerId;
import io.harness.exception.InvalidRequestException;
import io.harness.task.protocol.ResponseData;
import lombok.Getter;
import lombok.Setter;
import software.wings.api.ResourceConstraintExecutionData;
import software.wings.beans.Application;
import software.wings.beans.NotificationGroup;
import software.wings.beans.NotificationRule;
import software.wings.beans.ResourceConstraint;
import software.wings.beans.ResourceConstraintInstance;
import software.wings.beans.ResourceConstraintNotification;
import software.wings.beans.ResourceConstraintUsage;
import software.wings.beans.ResourceConstraintUsage.ActiveScope;
import software.wings.beans.WorkflowExecution;
import software.wings.common.NotificationMessageResolver.NotificationMessageType;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.workflow.WorkflowNotificationHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.ResourceConstraintService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionResponse.Builder;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.Min;

/*
 */

public class ResourceConstraintState extends State {
  @Inject private transient AppService applicationService;
  @Inject private transient ResourceConstraintService resourceConstraintService;
  @Inject private transient NotificationSetupService notificationSetupService;
  @Inject private transient NotificationService notificationService;
  @Inject private transient WingsPersistence wingsPersistence;
  @Inject private transient WorkflowNotificationHelper workflowNotificationHelper;

  @Getter @Setter private String resourceConstraintId;

  @Getter @Setter @Min(value = 1) private int permits;

  public enum HoldingScope { PIPELINE, WORKFLOW, PHASE, PHASE_SECTION, NEXT_STEP }

  @Getter @Setter private String holdingScope;

  public enum NotificationEvent { BLOCKED, UNBLOCKED }

  @Getter @Setter private List<NotificationEvent> notificationEvents;

  @Getter @Setter private List<String> notificationGroups;

  public ResourceConstraintState(String name) {
    super(name, StateType.RESOURCE_CONSTRAINT.name());
  }

  @Attributes(title = "Timeout (ms)")
  @DefaultValue("" + DEFAULT_ASYNC_CALL_TIMEOUT)
  @Override
  public Integer getTimeoutMillis() {
    return super.getTimeoutMillis();
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    return executeInternal(context);
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {
    // TODO: force the resource constraint to final. Not critical the backup job handles this so far.
  }

  @Override
  @SchemaIgnore
  public List<String> getPatternsForRequiredContextElementType() {
    // TODO: support for different repeaters
    return null;
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    String accountId = applicationService.getAccountIdByAppId(context.getAppId());
    final ResourceConstraint resourceConstraint = resourceConstraintService.get(accountId, resourceConstraintId);

    if (notificationEvents.contains(NotificationEvent.UNBLOCKED)) {
      sendNotification(accountId, context, resourceConstraint, RESOURCE_CONSTRAINT_UNBLOCKED_NOTIFICATION);
    }

    final Builder executionResponseBuilder = executionResponseBuilder(resourceConstraint);
    return executionResponseBuilder.build();
  }

  private ExecutionResponse executeInternal(ExecutionContext context) {
    String accountId = applicationService.getAccountIdByAppId(context.getAppId());
    final ResourceConstraint resourceConstraint = resourceConstraintService.get(accountId, resourceConstraintId);
    final Constraint constraint = resourceConstraintService.createAbstraction(resourceConstraint);

    final Builder executionResponseBuilder = executionResponseBuilder(resourceConstraint);

    String releaseEntityId = null;
    switch (HoldingScope.valueOf(holdingScope)) {
      case WORKFLOW:
        releaseEntityId = context.getWorkflowExecutionId();
        break;
      default:
        throw new InvalidRequestException(String.format("Unhandled holding scope %s", holdingScope));
    }

    Map<String, Object> constraintContext = new HashMap();
    constraintContext.put(ResourceConstraintInstance.APP_ID_KEY, context.getAppId());
    constraintContext.put(ResourceConstraintInstance.RELEASE_ENTITY_TYPE_KEY, holdingScope);
    constraintContext.put(ResourceConstraintInstance.RELEASE_ENTITY_ID_KEY, releaseEntityId);
    constraintContext.put(
        ResourceConstraintInstance.ORDER_KEY, resourceConstraintService.getMaxOrder(resourceConstraintId) + 1);

    String consumerId = generateUuid();
    try {
      final Consumer.State state = constraint.registerConsumer(
          new ConsumerId(consumerId), permits, constraintContext, resourceConstraintService.getRegistry());

      if (state == Consumer.State.ACTIVE) {
        return executionResponseBuilder.withExecutionStatus(ExecutionStatus.SUCCESS).build();
      }
    } catch (ConstraintException exception) {
      return executionResponseBuilder.withExecutionStatus(ExecutionStatus.FAILED)
          .withErrorMessage(exception.getMessage())
          .build();
    }

    if (isNotEmpty(notificationEvents) && notificationEvents.contains(NotificationEvent.BLOCKED)) {
      sendNotification(accountId, context, resourceConstraint, RESOURCE_CONSTRAINT_BLOCKED_NOTIFICATION);
    }

    return executionResponseBuilder.withAsync(true).withCorrelationIds(asList(consumerId)).build();
  }

  private void sendNotification(
      String accountId, ExecutionContext context, ResourceConstraint resourceConstraint, NotificationMessageType type) {
    if (isEmpty(notificationGroups)) {
      return;
    }

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);

    String envId = workflowStandardParams.getEnv() == null ? null : workflowStandardParams.getEnv().getUuid();
    final String workflowUrl = workflowNotificationHelper.calculateWorkflowUrl(
        context.getWorkflowExecutionId(), context.getOrchestrationWorkflowType(), accountId, context.getAppId(), envId);

    final ResourceConstraintNotification notification = new ResourceConstraintNotification();
    notification.setNotificationTemplateId(type.name());

    final Application application = applicationService.get(context.getAppId());
    Map<String, String> variables = new HashMap();
    variables.put("RESOURCE_CONSTRAINT_NAME", resourceConstraint.getName());
    variables.put("WORKFLOW_NAME", context.getWorkflowExecutionName());
    variables.put("WORKFLOW_URL", workflowUrl);
    variables.put("APP_NAME", application.getName());

    if (RESOURCE_CONSTRAINT_BLOCKED_NOTIFICATION.equals(type)) {
      final List<ResourceConstraintUsage> usages =
          resourceConstraintService.usage(accountId, asList(resourceConstraint.getUuid()));

      // Note that there is a race between determining to block the state and the time we obtain the blocking items
      // At least in theory the usages being empty is possible.
      if (isNotEmpty(usages)) {
        final ResourceConstraintUsage resourceConstraintUsage = usages.iterator().next();
        final ActiveScope activeScope = resourceConstraintUsage.getActiveScopes().iterator().next();

        if (WORKFLOW.name().equals(activeScope.getReleaseEntityType())) {
          final WorkflowExecution workflowExecution =
              wingsPersistence.get(WorkflowExecution.class, activeScope.getReleaseEntityId());

          if (workflowExecution != null) {
            final String blockingWorkflowUrl = workflowNotificationHelper.calculateWorkflowUrl(
                workflowExecution.getUuid(), workflowExecution.getOrchestrationType(), accountId,
                workflowExecution.getAppId(), workflowExecution.getEnvId());

            variables.put("BLOCKING_WORKFLOW_NAME", activeScope.getReleaseEntityName());
            variables.put("BLOCKING_WORKFLOW_URL", blockingWorkflowUrl);
          }
        } else {
          unhandled(activeScope.getReleaseEntityType());
        }
      }
    }

    notification.setAccountId(accountId);
    notification.setAppId(context.getAppId());
    notification.setNotificationTemplateVariables(variables);

    final List<NotificationGroup> notificationGroupObjects =
        notificationSetupService.readNotificationGroups(accountId, notificationGroups);

    final NotificationRule notificationRule =
        aNotificationRule().withNotificationGroups(notificationGroupObjects).build();

    notificationService.sendNotificationAsync(notification, asList(notificationRule));
  }

  private Builder executionResponseBuilder(ResourceConstraint resourceConstraint) {
    ResourceConstraintExecutionData stateExecutionData = new ResourceConstraintExecutionData();
    stateExecutionData.setResourceConstraintName(resourceConstraint.getName());
    stateExecutionData.setResourceConstraintCapacity(resourceConstraint.getCapacity());
    stateExecutionData.setUsage(permits);
    return anExecutionResponse().withStateExecutionData(stateExecutionData);
  }
}
