package io.harness.pms.sdk.core.facilitator.eventhandler;

import io.harness.logging.AutoLogContext;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.facilitators.FacilitatorEvent;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorResponseProto;
import io.harness.pms.contracts.refobjects.RefObject;
import io.harness.pms.execution.utils.FacilitatorEventUtils;
import io.harness.pms.sdk.core.execution.EngineObtainmentHelper;
import io.harness.pms.sdk.core.execution.SdkNodeExecutionService;
import io.harness.pms.sdk.core.facilitator.Facilitator;
import io.harness.pms.sdk.core.facilitator.FacilitatorResponse;
import io.harness.pms.sdk.core.facilitator.FacilitatorResponseMapper;
import io.harness.pms.sdk.core.registries.FacilitatorRegistry;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

import com.google.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FacilitatorEventHandlerImpl implements FacilitatorEventHandler {
  @Inject private FacilitatorRegistry facilitatorRegistry;
  @Inject private SdkNodeExecutionService sdkNodeExecutionService;
  @Inject private EngineObtainmentHelper engineObtainmentHelper;

  @Override
  public boolean handleEvent(FacilitatorEvent event) {
    boolean handled;
    try (AutoLogContext ignore = FacilitatorEventUtils.obtainLogContext(event)) {
      handled = handleFacilitatorEventInternally(event);
      showLog(handled);
    }
    return handled;
  }

  private boolean handleFacilitatorEventInternally(FacilitatorEvent event) {
    try {
      Ambiance ambiance = event.getAmbiance();
      StepInputPackage inputPackage = obtainInputPackage(ambiance, event.getRefObjectsList());
      FacilitatorResponse currFacilitatorResponse = null;
      for (FacilitatorObtainment obtainment : event.getFacilitatorObtainmentsList()) {
        Facilitator facilitator = facilitatorRegistry.obtain(obtainment.getType());
        StepParameters stepParameters =
            RecastOrchestrationUtils.fromDocumentJson(event.getStepParameters().toStringUtf8(), StepParameters.class);
        currFacilitatorResponse =
            facilitator.facilitate(ambiance, stepParameters, obtainment.getParameters().toByteArray(), inputPackage);
        if (currFacilitatorResponse != null) {
          break;
        }
      }
      if (currFacilitatorResponse == null) {
        sdkNodeExecutionService.handleFacilitationResponse(event.getNodeExecutionId(), event.getNotifyId(),
            FacilitatorResponseProto.newBuilder().setIsSuccessful(false).build());
        return true;
      }
      sdkNodeExecutionService.handleFacilitationResponse(event.getNodeExecutionId(), event.getNotifyId(),
          FacilitatorResponseMapper.toFacilitatorResponseProto(currFacilitatorResponse));
      return true;
    } catch (Exception ex) {
      log.error("Error while facilitating execution", ex);
      return false;
    }
  }

  private StepInputPackage obtainInputPackage(Ambiance ambiance, List<RefObject> refObjectList) {
    return engineObtainmentHelper.obtainInputPackage(ambiance, refObjectList);
  }

  private void showLog(boolean isSuccess) {
    if (isSuccess) {
      log.info("[PMS_SDK] Handled Facilitator Event Successfully");
    }
  }
}
