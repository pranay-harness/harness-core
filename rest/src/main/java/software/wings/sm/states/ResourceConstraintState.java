package software.wings.sm.states;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Arrays.asList;
import static software.wings.common.Constants.DEFAULT_ASYNC_CALL_TIMEOUT;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.distribution.constraint.Constraint;
import io.harness.distribution.constraint.ConstraintException;
import io.harness.distribution.constraint.Consumer;
import io.harness.distribution.constraint.ConsumerId;
import lombok.Getter;
import lombok.Setter;
import software.wings.api.ResourceConstraintExecutionData;
import software.wings.beans.ResourceConstraint;
import software.wings.beans.ResourceConstraintInstance;
import software.wings.exception.InvalidRequestException;
import software.wings.exception.WingsException;
import software.wings.lock.PersistentLocker;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ResourceConstraintService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionResponse.Builder;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;
import software.wings.waitnotify.NotifyResponseData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 */

public class ResourceConstraintState extends State {
  public static final String PERSISTENT_LOCK_NAME = "ResourceConstraintState";
  private static String errorMsg =
      "The barrier endures since some of the tasks failed before all instances were reached.";

  @Inject private transient AppService applicationService;
  @Inject private transient ResourceConstraintService resourceConstraintService;
  @Inject private transient PersistentLocker persistentLocker;

  @Getter @Setter private String resourceConstraintId;

  @Getter @Setter private int permits;

  public enum HoldingScope { PIPELINE, WORKFLOW, PHASE, PHASE_SECTION, NEXT_STEP }

  @Getter @Setter private String holdingScope;

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
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    final Builder executionResponseBuilder = executionResponseBuilder();
    return executionResponseBuilder.build();
  }

  private ExecutionResponse executeInternal(ExecutionContext context) {
    final Builder executionResponseBuilder = executionResponseBuilder();

    String accountId = applicationService.getAccountIdByAppId(context.getAppId());

    final ResourceConstraint resourceConstraint = resourceConstraintService.get(accountId, resourceConstraintId);
    final Constraint constraint = resourceConstraintService.createAbstraction(resourceConstraint);

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
    constraintContext.put(ResourceConstraintInstance.ORDER_KEY,
        resourceConstraintService.getMaxOrder(context.getAppId(), resourceConstraintId) + 1);

    String consumerId = generateUuid();
    try {
      final Consumer.State state = constraint.registerConsumer(
          new ConsumerId(consumerId), permits, constraintContext, resourceConstraintService.getRegistry());

      if (state == Consumer.State.ACTIVE) {
        return executionResponseBuilder.withExecutionStatus(ExecutionStatus.SUCCESS).build();
      }
    } catch (ConstraintException exception) {
      throw new WingsException(exception);
    }

    return executionResponseBuilder.withAsync(true).withCorrelationIds(asList(consumerId)).build();
  }

  private Builder executionResponseBuilder() {
    ResourceConstraintExecutionData stateExecutionData = new ResourceConstraintExecutionData();
    return anExecutionResponse().withStateExecutionData(stateExecutionData);
  }
}
