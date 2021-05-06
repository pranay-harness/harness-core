package io.harness.pms.sdk.core.interrupt;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.govern.Switch.noop;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.AutoLogContext;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.ExecutableResponse.ResponseCase;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.interrupts.InterruptType;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.interrupts.InterruptEvent;
import io.harness.pms.sdk.core.registries.StepRegistry;
import io.harness.pms.sdk.core.steps.Step;
import io.harness.pms.sdk.core.steps.executables.Abortable;
import io.harness.pms.sdk.core.steps.executables.Failable;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueListener;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PIPELINE)
public class InterruptEventListener extends QueueListener<InterruptEvent> {
  @Inject private PMSInterruptService pmsInterruptService;
  @Inject private StepRegistry stepRegistry;

  @Inject
  public InterruptEventListener(QueueConsumer<InterruptEvent> queueConsumer) {
    super(queueConsumer, false);
  }

  @Override
  public void onMessage(InterruptEvent event) {
    try (AutoLogContext ignore = event.autoLogContext()) {
      InterruptType interruptType = event.getInterruptType();
      switch (interruptType) {
        case ABORT:
          handleAbort(event);
          break;
        case CUSTOM_FAILURE:
          handleFailure(event);
          break;
        default:
          log.warn("No Handling present for Interrupt Event of type : {}", interruptType);
          noop();
      }
    }
  }

  private void handleFailure(InterruptEvent event) {
    try {
      NodeExecutionProto nodeExecutionProto = event.getNodeExecution();
      StepType stepType = event.getNodeExecution().getNode().getStepType();
      Step<?> step = stepRegistry.obtain(stepType);
      if (step instanceof Failable) {
        StepParameters stepParameters = RecastOrchestrationUtils.fromDocumentJson(
            nodeExecutionProto.getResolvedStepParameters(), StepParameters.class);
        ((Failable) step).handleFailureInterrupt(nodeExecutionProto.getAmbiance(), stepParameters, event.getMetadata());
      }
      pmsInterruptService.handleFailure(event.getNotifyId());
    } catch (Exception ex) {
      throw new InvalidRequestException("Handling failure at sdk failed with exception - " + ex.getMessage()
          + " with interrupt event - " + event.toString());
    }
  }

  private void handleAbort(InterruptEvent event) {
    try {
      NodeExecutionProto nodeExecutionProto = event.getNodeExecution();
      StepType stepType = event.getNodeExecution().getNode().getStepType();
      Step<?> step = stepRegistry.obtain(stepType);
      if (step instanceof Abortable) {
        StepParameters stepParameters = RecastOrchestrationUtils.fromDocumentJson(
            nodeExecutionProto.getResolvedStepParameters(), StepParameters.class);
        ((Abortable) step)
            .handleAbort(
                nodeExecutionProto.getAmbiance(), stepParameters, extractExecutableResponses(nodeExecutionProto));
        pmsInterruptService.handleAbort(event.getNotifyId());
      } else {
        pmsInterruptService.handleAbort(event.getNotifyId());
      }
    } catch (Exception ex) {
      // Ignore
    }
  }

  private Object extractExecutableResponses(NodeExecutionProto nodeExecutionProto) {
    int responseCount = nodeExecutionProto.getExecutableResponsesCount();
    if (responseCount <= 0) {
      return null;
    }
    ExecutableResponse executableResponse = nodeExecutionProto.getExecutableResponses(responseCount - 1);
    ResponseCase responseCase = executableResponse.getResponseCase();
    switch (responseCase) {
      case ASYNC:
        return executableResponse.getAsync();
      case TASK:
        return executableResponse.getTask();
      case TASKCHAIN:
        return executableResponse.getTaskChain();
      case CHILD:
      case CHILDREN:
      case CHILDCHAIN:
        log.error("Only Leaf Nodes are supposed to be Abortable : {}", responseCase);
        throw new IllegalStateException("Not an abortable node");
      case RESPONSE_NOT_SET:
      default:
        log.warn("No Handling present for Executable Response of type : {}", responseCase);
        return null;
    }
  }
}
