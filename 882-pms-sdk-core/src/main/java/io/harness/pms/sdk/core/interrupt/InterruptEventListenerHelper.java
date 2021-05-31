package io.harness.pms.sdk.core.interrupt;

import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.ExecutableResponse.ResponseCase;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.registries.StepRegistry;
import io.harness.pms.sdk.core.steps.Step;
import io.harness.pms.sdk.core.steps.executables.Abortable;
import io.harness.pms.sdk.core.steps.executables.Failable;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

import com.google.inject.Inject;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InterruptEventListenerHelper {
  @Inject private PMSInterruptService pmsInterruptService;
  @Inject private StepRegistry stepRegistry;

  public void handleFailure(
      NodeExecutionProto nodeExecutionProto, Map<String, String> metadata, String interruptId, String notifyId) {
    try {
      Step<?> step = stepRegistry.obtain(nodeExecutionProto.getNode().getStepType());
      if (step instanceof Failable) {
        StepParameters stepParameters = RecastOrchestrationUtils.fromDocumentJson(
            nodeExecutionProto.getResolvedStepParameters(), StepParameters.class);
        ((Failable) step).handleFailureInterrupt(nodeExecutionProto.getAmbiance(), stepParameters, metadata);
      }
      pmsInterruptService.handleFailure(notifyId);
    } catch (Exception ex) {
      throw new InvalidRequestException("Handling failure at sdk failed with exception - " + ex.getMessage()
          + " with interrupt event - " + interruptId);
    }
  }

  public void handleAbort(NodeExecutionProto nodeExecutionProto, String notifyId) {
    try {
      StepType stepType = nodeExecutionProto.getNode().getStepType();
      Step<?> step = stepRegistry.obtain(stepType);
      if (step instanceof Abortable) {
        StepParameters stepParameters = RecastOrchestrationUtils.fromDocumentJson(
            nodeExecutionProto.getResolvedStepParameters(), StepParameters.class);
        ((Abortable) step)
            .handleAbort(
                nodeExecutionProto.getAmbiance(), stepParameters, extractExecutableResponses(nodeExecutionProto));
        pmsInterruptService.handleAbort(notifyId);
      } else {
        pmsInterruptService.handleAbort(notifyId);
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
