package software.wings.sm.states;

import static io.harness.govern.Switch.unhandled;
import static java.util.Arrays.asList;
import static software.wings.common.Constants.DEFAULT_ASYNC_CALL_TIMEOUT;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import io.harness.distribution.barrier.Barrier;
import lombok.Getter;
import lombok.Setter;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.BarrierExecutionData;
import software.wings.beans.BarrierInstance;
import software.wings.service.intfc.BarrierService;
import software.wings.sm.BarrierStatusData;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionResponse.Builder;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;
import software.wings.waitnotify.NotifyResponseData;

import java.util.Map;

/*
   The barrier is a primitive that will allow for multiple workflows that run in parallel to get aligned on particular
   step.

   Barrier properties:
   Identifier - an arbitrary string value that will identify the barrier. All barrier with the same name and scope from
   the workflows that execute in parallel will be collected into barrier group.

   The barrier state will wait for all the other states to be in a running status and then finish allowing for the
   workflow to continue with its execution.

   If a workflow fails before it's barrier state is reached it will signal the states from the other workflows and they
   will fail, producing the needed effect their respective workflow to execute needed rollbacks and finish.

   If a barrier is in a optional path that was skipped all the barrier state will be considered reached. This will
   unblock all the other workflows if all the other barrier states get reached.

   This logic is implemented on top of the generic io.harness.commons.distribution.barrier. For this abstraction we
   use the following three:

                         pipeline

                     /       |        \
                workflow  workflow  workflow
                    |        |         |
                  phase    phase     phase
                    |        |         |
                  step     step       step

*/

public class BarrierState extends State {
  private static String errorMsg =
      "The barrier endures since some of the tasks failed before all instances were reached.";

  @Inject @Transient private transient BarrierService barrierService;

  @Getter @Setter @Attributes(title = "Identifier") private String identifier;

  public BarrierState(String name) {
    super(name, StateType.BARRIER.name());
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
    updateBarrier(context);
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    updateBarrier(context);
    final Builder executionResponseBuilder = executionResponseBuilder();

    NotifyResponseData notifyResponseData = response.values().iterator().next();
    if (notifyResponseData instanceof BarrierStatusData && ((BarrierStatusData) notifyResponseData).isFailed()) {
      executionResponseBuilder.withExecutionStatus(ExecutionStatus.FAILED).withErrorMessage(errorMsg);
    }

    return executionResponseBuilder.build();
  }

  private ExecutionResponse executeInternal(ExecutionContext context) {
    BarrierInstance barrierInstance = updateBarrier(context);

    final Builder executionResponseBuilder = executionResponseBuilder();

    if (barrierInstance == null) {
      return executionResponseBuilder.withExecutionStatus(ExecutionStatus.SUCCESS).build();
    }
    final Barrier.State state = Barrier.State.valueOf(barrierInstance.getState());
    switch (state) {
      case DOWN:
        return executionResponseBuilder.withExecutionStatus(ExecutionStatus.SUCCESS).build();
      case ENDURE:
        return executionResponseBuilder.withExecutionStatus(ExecutionStatus.FAILED).withErrorMessage(errorMsg).build();
      case STANDING:
        break;
      default:
        unhandled(state);
    }
    return executionResponseBuilder.withAsync(true).withCorrelationIds(asList(barrierInstance.getUuid())).build();
  }

  private Builder executionResponseBuilder() {
    BarrierExecutionData stateExecutionData = new BarrierExecutionData();
    stateExecutionData.setIdentifier(identifier);
    return anExecutionResponse().withStateExecutionData(stateExecutionData);
  }

  private BarrierInstance updateBarrier(ExecutionContext context) {
    final String barrierId = barrierService.findByStep(
        context.getAppId(), context.getPipelineStateElementId(), context.getWorkflowExecutionId(), getIdentifier());

    // If this is noop barrier
    if (barrierId == null) {
      return null;
    }

    return barrierService.update(context.getAppId(), barrierId);
  }
}
