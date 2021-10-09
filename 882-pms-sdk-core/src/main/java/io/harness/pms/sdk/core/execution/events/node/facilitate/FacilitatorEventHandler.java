package io.harness.pms.sdk.core.execution.events.node.facilitate;

import static io.harness.pms.sdk.core.execution.events.node.NodeEventHelper.buildStepInputPackage;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.facilitators.FacilitatorEvent;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorResponseProto;
import io.harness.pms.contracts.plan.NodeExecutionEventType;
import io.harness.pms.events.base.PmsBaseEventHandler;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.execution.EngineObtainmentHelper;
import io.harness.pms.sdk.core.execution.SdkNodeExecutionService;
import io.harness.pms.sdk.core.registries.FacilitatorRegistry;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class FacilitatorEventHandler extends PmsBaseEventHandler<FacilitatorEvent> {
  @Inject private FacilitatorRegistry facilitatorRegistry;
  @Inject private SdkNodeExecutionService sdkNodeExecutionService;

  @Override
  protected String getMetricPrefix(FacilitatorEvent message) {
    return "facilitator_event";
  }

  @Override
  protected Map<String, String> extraLogProperties(FacilitatorEvent event) {
    return ImmutableMap.<String, String>builder()
        .put("eventType", NodeExecutionEventType.FACILITATE.name())
        .put("notifyId", event.getNotifyId())
        .build();
  }

  @Override
  protected Ambiance extractAmbiance(FacilitatorEvent event) {
    return event.getAmbiance();
  }

  @Override
  protected void handleEventWithContext(FacilitatorEvent event) {
    try {
      Ambiance ambiance = event.getAmbiance();
      FacilitatorResponse currFacilitatorResponse = null;
      for (FacilitatorObtainment obtainment : event.getFacilitatorObtainmentsList()) {
        Facilitator facilitator = facilitatorRegistry.obtain(obtainment.getType());
        StepParameters stepParameters =
            RecastOrchestrationUtils.fromJson(event.getStepParameters().toStringUtf8(), StepParameters.class);
        currFacilitatorResponse =
            facilitator.facilitate(ambiance, stepParameters, obtainment.getParameters().toByteArray(), buildStepInputPackage(event.getResolvedInputList()));
        if (currFacilitatorResponse != null) {
          break;
        }
      }
      if (currFacilitatorResponse == null) {
        log.info("Calculated Facilitator response is null. Returning response Successful false");
        sdkNodeExecutionService.handleFacilitationResponse(
            ambiance, event.getNotifyId(), FacilitatorResponseProto.newBuilder().setIsSuccessful(false).build());
        return;
      }
      sdkNodeExecutionService.handleFacilitationResponse(
          ambiance, event.getNotifyId(), FacilitatorResponseMapper.toFacilitatorResponseProto(currFacilitatorResponse));
      log.info("Facilitation Event Handled Successfully");
    } catch (Exception ex) {
      log.error("Error while facilitating execution", ex);
    }
  }
}
