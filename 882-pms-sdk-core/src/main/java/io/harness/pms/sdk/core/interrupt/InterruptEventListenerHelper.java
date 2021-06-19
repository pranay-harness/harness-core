package io.harness.pms.sdk.core.interrupt;

import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.interrupts.InterruptEvent;
import io.harness.pms.contracts.interrupts.InterruptEvent.ResponseCase;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.registries.StepRegistry;
import io.harness.pms.sdk.core.steps.Step;
import io.harness.pms.sdk.core.steps.executables.Abortable;
import io.harness.pms.sdk.core.steps.executables.Failable;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InterruptEventListenerHelper {
  @Inject private PMSInterruptService pmsInterruptService;
  @Inject private StepRegistry stepRegistry;

  public void handleFailure(InterruptEvent event) {
    try {
      Step<?> step = stepRegistry.obtain(AmbianceUtils.getCurrentStepType(event.getAmbiance()));
      if (step instanceof Failable) {
        StepParameters stepParameters =
            RecastOrchestrationUtils.fromDocumentJson(event.getStepParameters().toStringUtf8(), StepParameters.class);
        ((Failable) step).handleFailureInterrupt(event.getAmbiance(), stepParameters, event.getMetadataMap());
      }
      pmsInterruptService.handleFailure(event.getNotifyId());
    } catch (Exception ex) {
      throw new InvalidRequestException("Handling failure at sdk failed with exception - " + ex.getMessage()
          + " with interrupt event - " + event.getInterruptUuid());
    }
  }

  public void handleAbort(InterruptEvent event) {
    try {
      StepType stepType = AmbianceUtils.getCurrentStepType(event.getAmbiance());
      Step<?> step = stepRegistry.obtain(stepType);
      if (step instanceof Abortable) {
        StepParameters stepParameters =
            RecastOrchestrationUtils.fromDocumentJson(event.getStepParameters().toStringUtf8(), StepParameters.class);
        ((Abortable) step).handleAbort(event.getAmbiance(), stepParameters, extractExecutableResponses(event));
        pmsInterruptService.handleAbort(event.getNotifyId());
      } else {
        pmsInterruptService.handleAbort(event.getNotifyId());
      }
    } catch (Exception ex) {
      log.error("Handling abort at sdk failed with interrupt event - {} ", event.getInterruptUuid(), ex);
      // Even if error send feedback
      pmsInterruptService.handleAbort(event.getNotifyId());
    }
  }

  private Object extractExecutableResponses(InterruptEvent interruptEvent) {
    ResponseCase responseCase = interruptEvent.getResponseCase();
    switch (responseCase) {
      case ASYNC:
        return interruptEvent.getAsync();
      case TASK:
        return interruptEvent.getTask();
      case TASKCHAIN:
        return interruptEvent.getTaskChain();
      case RESPONSE_NOT_SET:
      default:
        log.warn("No Handling present for Executable Response of type : {}", responseCase);
        return null;
    }
  }
}
