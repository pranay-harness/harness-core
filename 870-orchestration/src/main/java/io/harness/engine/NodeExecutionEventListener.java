package io.harness.engine;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.engine.executables.ExecutableProcessor;
import io.harness.engine.executables.ExecutableProcessorFactory;
import io.harness.engine.executables.InvokerPackage;
import io.harness.engine.executables.ResumePackage;
import io.harness.engine.executions.node.NodeExecutionProtoService;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.execution.NodeExecutionEvent;
import io.harness.pms.execution.NodeExecutionProto;
import io.harness.pms.execution.ResumeNodeExecutionEventData;
import io.harness.pms.execution.StartNodeExecutionEventData;
import io.harness.pms.execution.Status;
import io.harness.pms.execution.failure.FailureInfo;
import io.harness.pms.facilitators.FacilitatorObtainment;
import io.harness.pms.facilitators.FacilitatorResponseProto;
import io.harness.pms.plan.PlanNodeProto;
import io.harness.pms.sdk.core.facilitator.Facilitator;
import io.harness.pms.sdk.core.facilitator.FacilitatorResponse;
import io.harness.pms.sdk.core.facilitator.FacilitatorResponseMapper;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.registries.AdviserRegistry;
import io.harness.pms.sdk.registries.FacilitatorRegistry;
import io.harness.pms.sdk.registries.StepRegistry;
import io.harness.pms.steps.io.StepResponseProto;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueListener;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
public class NodeExecutionEventListener extends QueueListener<NodeExecutionEvent> {
  @Inject private StepRegistry stepRegistry;
  @Inject private AdviserRegistry adviserRegistry;
  @Inject private FacilitatorRegistry facilitatorRegistry;
  @Inject private NodeExecutionProtoService nodeExecutionProtoService;
  @Inject private EngineObtainmentHelper engineObtainmentHelper;
  @Inject private ExecutableProcessorFactory executableProcessorFactory;

  @Inject
  public NodeExecutionEventListener(QueueConsumer<NodeExecutionEvent> queueConsumer) {
    super(queueConsumer, false);
  }

  @Override
  public void onMessage(NodeExecutionEvent event) {
    log.info("Notifying for NodeExecutionEvent: type: {}, id: {}", event.getEventType().name(),
        event.getNodeExecution().getUuid());
  }

  private void facilitateExecution(NodeExecutionEvent event) {
    try {
      NodeExecutionProto nodeExecution = event.getNodeExecution();
      Ambiance ambiance = nodeExecution.getAmbiance();
      StepInputPackage inputPackage = obtainInputPackage(nodeExecution);
      PlanNodeProto node = nodeExecution.getNode();
      FacilitatorResponseProto facilitatorResponse;
      for (FacilitatorObtainment obtainment : node.getFacilitatorObtainmentsList()) {
        Facilitator facilitator = facilitatorRegistry.obtain(obtainment.getType());
        FacilitatorResponse currFacilitatorResponse =
            facilitator.facilitate(ambiance, nodeExecutionProtoService.extractResolvedStepParameters(nodeExecution),
                obtainment.getParameters().toByteArray(), inputPackage);
        if (currFacilitatorResponse != null) {
          facilitatorResponse = FacilitatorResponseMapper.toFacilitatorResponseProto(currFacilitatorResponse);
          break;
        }
      }
      // TODO: Send the facilitator response to pipeline service for processing
    } catch (Exception ignored) {
      // TODO: Send error to pipeline service for processing
    }
  }

  private void startExecution(NodeExecutionEvent event) {
    try {
      NodeExecutionProto nodeExecution = event.getNodeExecution();
      ExecutableProcessor processor = executableProcessorFactory.obtainProcessor(nodeExecution.getMode());
      StepInputPackage inputPackage = obtainInputPackage(nodeExecution);
      StartNodeExecutionEventData eventData = (StartNodeExecutionEventData) event.getEventData();
      FacilitatorResponse facilitatorResponse = eventData.getFacilitatorResponse() == null
          ? null
          : FacilitatorResponseMapper.fromFacilitatorResponseProto(eventData.getFacilitatorResponse());
      processor.handleStart(
          InvokerPackage.builder()
              .inputPackage(inputPackage)
              .nodes(eventData.getNodes())
              .passThroughData(facilitatorResponse == null ? null : facilitatorResponse.getPassThroughData())
              .nodeExecution(nodeExecution)
              .build());
    } catch (Exception ignored) {
      // TODO: Send error to pipeline service for processing
    }
  }

  private void resumeExecution(NodeExecutionEvent event) {
    NodeExecutionProto nodeExecution = event.getNodeExecution();
    ExecutableProcessor processor = executableProcessorFactory.obtainProcessor(nodeExecution.getMode());
    ResumeNodeExecutionEventData eventData = (ResumeNodeExecutionEventData) event.getEventData();
    try {
      if (eventData.isAsyncError()) {
        ErrorNotifyResponseData errorNotifyResponseData =
            (ErrorNotifyResponseData) eventData.getResponse().values().iterator().next();
        StepResponseProto stepResponse =
            StepResponseProto.newBuilder()
                .setStatus(Status.ERRORED)
                .setFailureInfo(FailureInfo.newBuilder()
                                    .addAllFailureTypes(EngineExceptionUtils.transformFailureTypes(
                                        errorNotifyResponseData.getFailureTypes()))
                                    .setErrorMessage(errorNotifyResponseData.getErrorMessage())
                                    .build())
                .build();
        // TODO: Send the step response to pipeline service for processing
        return;
      }

      processor.handleResume(ResumePackage.builder()
                                 .nodeExecution(nodeExecution)
                                 .nodes(eventData.getNodes())
                                 .responseDataMap(eventData.getResponse())
                                 .build());
    } catch (Exception ex) {
      // TODO: Send error to pipeline service for processing
    }
  }

  private StepInputPackage obtainInputPackage(NodeExecutionProto nodeExecution) {
    return engineObtainmentHelper.obtainInputPackage(
        nodeExecution.getAmbiance(), nodeExecution.getNode().getRebObjectsList());
  }
}
