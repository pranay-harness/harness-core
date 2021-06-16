package io.harness.pms.sdk.core.execution.events.node.start;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.start.NodeStartEvent;
import io.harness.pms.contracts.plan.NodeExecutionEventType;
import io.harness.pms.events.base.PmsBaseEventHandler;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.execution.EngineObtainmentHelper;
import io.harness.pms.sdk.core.execution.ExecutableProcessor;
import io.harness.pms.sdk.core.execution.ExecutableProcessorFactory;
import io.harness.pms.sdk.core.execution.InvokerPackage;
import io.harness.pms.sdk.core.execution.NodeExecutionUtils;
import io.harness.pms.sdk.core.execution.SdkNodeExecutionService;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
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
public class NodeStartEventHandler extends PmsBaseEventHandler<NodeStartEvent> {
  @Inject private ExecutableProcessorFactory executableProcessorFactory;
  @Inject private EngineObtainmentHelper engineObtainmentHelper;
  @Inject private SdkNodeExecutionService sdkNodeExecutionService;

  @Override
  protected Map<String, String> extraLogProperties(NodeStartEvent event) {
    return ImmutableMap.<String, String>builder().put("eventType", NodeExecutionEventType.START.name()).build();
  }

  @Override
  protected Ambiance extractAmbiance(NodeStartEvent event) {
    return event.getAmbiance();
  }

  @Override
  protected Map<String, String> extractMetricContext(NodeStartEvent message) {
    return ImmutableMap.<String, String>builder()
        .put("accountId", AmbianceUtils.getAccountId(message.getAmbiance()))
        .put("projectIdentifier", AmbianceUtils.getOrgIdentifier(message.getAmbiance()))
        .put("orgIdentifier", AmbianceUtils.getProjectIdentifier(message.getAmbiance()))
        .build();
  }
  @Override
  protected String getMetricPrefix(NodeStartEvent message) {
    return "start_event";
  }

  @Override
  public boolean handleEventWithContext(NodeStartEvent nodeStartEvent) {
    try {
      log.info("Starting to handle NodeStart event");
      ExecutableProcessor processor = executableProcessorFactory.obtainProcessor(nodeStartEvent.getMode());
      StepInputPackage inputPackage =
          engineObtainmentHelper.obtainInputPackage(nodeStartEvent.getAmbiance(), nodeStartEvent.getRefObjectsList());
      StepParameters stepParameters = RecastOrchestrationUtils.fromDocumentJson(
          nodeStartEvent.getStepParameters().toStringUtf8(), StepParameters.class);

      String passThoughString = nodeStartEvent.getFacilitatorPassThoroughData().toStringUtf8();
      PassThroughData passThroughData =
          RecastOrchestrationUtils.fromDocumentJson(passThoughString, PassThroughData.class);
      processor.handleStart(InvokerPackage.builder()
                                .ambiance(nodeStartEvent.getAmbiance())
                                .inputPackage(inputPackage)
                                .passThroughData(passThroughData)
                                .stepParameters(stepParameters)
                                .executionMode(nodeStartEvent.getMode())
                                .build());
      log.info("Successfully handled NodeStart event");
      return true;
    } catch (Exception ex) {
      log.error("Error while handle NodeStart event", ex);
      sdkNodeExecutionService.handleStepResponse(AmbianceUtils.obtainCurrentRuntimeId(nodeStartEvent.getAmbiance()),
          NodeExecutionUtils.constructStepResponse(ex));
      return true;
    }
  }
}
