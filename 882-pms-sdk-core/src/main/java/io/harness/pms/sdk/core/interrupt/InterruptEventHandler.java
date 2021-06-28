package io.harness.pms.sdk.core.interrupt;

import static io.harness.govern.Switch.noop;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.interrupts.InterruptEvent;
import io.harness.pms.contracts.interrupts.InterruptEvent.ResponseCase;
import io.harness.pms.contracts.interrupts.InterruptType;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.events.base.PmsBaseEventHandler;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.interrupt.publisher.SdkInterruptResponsePublisher;
import io.harness.pms.sdk.core.registries.StepRegistry;
import io.harness.pms.sdk.core.steps.Step;
import io.harness.pms.sdk.core.steps.executables.Abortable;
import io.harness.pms.sdk.core.steps.executables.Failable;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class InterruptEventHandler extends PmsBaseEventHandler<InterruptEvent> {
  @Inject private SdkInterruptResponsePublisher interruptEventNotifyPublisher;
  @Inject private StepRegistry stepRegistry;

  @Override
  protected Map<String, String> extraLogProperties(InterruptEvent event) {
    return ImmutableMap.<String, String>builder()
        .put("interruptType", event.getType().name())
        .put("interruptUuid", event.getInterruptUuid())
        .put("notifyId", event.getNotifyId())
        .build();
  }

  @Override
  protected Ambiance extractAmbiance(InterruptEvent event) {
    return event.getAmbiance();
  }

  @Override
  protected Map<String, String> extractMetricContext(InterruptEvent message) {
    return ImmutableMap.of();
  }

  @Override
  protected String getMetricPrefix(InterruptEvent message) {
    return null;
  }

  @Override
  protected void handleEventWithContext(InterruptEvent event) {
    InterruptType interruptType = event.getType();
    switch (interruptType) {
      case ABORT:
        handleAbort(event);
        log.info("[PMS_SDK] Handled ABORT InterruptEvent Successfully");
        break;
      case CUSTOM_FAILURE:
        handleFailure(event);
        log.info("[PMS_SDK] Handled CUSTOM_FAILURE InterruptEvent Successfully");
        break;
      default:
        log.warn("No Handling present for Interrupt Event of type : {}", interruptType);
        noop();
    }
  }

  public void handleFailure(InterruptEvent event) {
    try {
      Step<?> step = stepRegistry.obtain(AmbianceUtils.getCurrentStepType(event.getAmbiance()));
      if (step instanceof Failable) {
        StepParameters stepParameters =
            RecastOrchestrationUtils.fromDocumentJson(event.getStepParameters().toStringUtf8(), StepParameters.class);
        ((Failable) step).handleFailureInterrupt(event.getAmbiance(), stepParameters, event.getMetadataMap());
      }
      interruptEventNotifyPublisher.publishEvent(event.getNotifyId(), event.getType());
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
        interruptEventNotifyPublisher.publishEvent(event.getNotifyId(), event.getType());
      } else {
        interruptEventNotifyPublisher.publishEvent(event.getNotifyId(), event.getType());
      }
    } catch (Exception ex) {
      log.error("Handling abort at sdk failed with interrupt event - {} ", event.getInterruptUuid(), ex);
      // Even if error send feedback
      interruptEventNotifyPublisher.publishEvent(event.getNotifyId(), event.getType());
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
