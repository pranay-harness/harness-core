package io.harness.pms.sdk.core.execution;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.advisers.AdviseType;
import io.harness.pms.contracts.advisers.AdviserObtainment;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorResponseProto;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.contracts.steps.io.StepResponseProto;
import io.harness.pms.execution.AdviseNodeExecutionEventData;
import io.harness.pms.execution.NodeExecutionEvent;
import io.harness.pms.execution.NodeExecutionEventType;
import io.harness.pms.execution.ResumeNodeExecutionEventData;
import io.harness.pms.execution.StartNodeExecutionEventData;
import io.harness.pms.execution.utils.EngineExceptionUtils;
import io.harness.pms.sdk.core.adviser.Adviser;
import io.harness.pms.sdk.core.adviser.AdvisingEvent;
import io.harness.pms.sdk.core.facilitator.Facilitator;
import io.harness.pms.sdk.core.facilitator.FacilitatorResponse;
import io.harness.pms.sdk.core.facilitator.FacilitatorResponseMapper;
import io.harness.pms.sdk.core.registries.AdviserRegistry;
import io.harness.pms.sdk.core.registries.FacilitatorRegistry;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueListener;
import io.harness.tasks.ErrorResponseData;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
public class NodeExecutionEventListener extends QueueListener<NodeExecutionEvent> {
  @Inject private FacilitatorRegistry facilitatorRegistry;
  @Inject private AdviserRegistry adviserRegistry;
  @Inject private PmsNodeExecutionService pmsNodeExecutionService;
  @Inject private EngineObtainmentHelper engineObtainmentHelper;
  @Inject private ExecutableProcessorFactory executableProcessorFactory;

  @Inject
  public NodeExecutionEventListener(QueueConsumer<NodeExecutionEvent> queueConsumer) {
    super(queueConsumer, false);
  }

  @Override
  public void onMessage(NodeExecutionEvent event) {
    NodeExecutionEventType nodeExecutionEventType = event.getEventType();
    switch (nodeExecutionEventType) {
      case START:
        startExecution(event);
        break;
      case FACILITATE:
        facilitateExecution(event);
        break;
      case RESUME:
        resumeExecution(event);
        break;
      case ADVISE:
        adviseExecution(event);
        break;
      default:
        throw new UnsupportedOperationException("NodeExecution Event Has no handler" + event.getEventType());
    }
  }

  private void facilitateExecution(NodeExecutionEvent event) {
    try {
      NodeExecutionProto nodeExecution = event.getNodeExecution();
      Ambiance ambiance = nodeExecution.getAmbiance();
      StepInputPackage inputPackage = obtainInputPackage(nodeExecution);
      PlanNodeProto node = nodeExecution.getNode();
      FacilitatorResponse currFacilitatorResponse = null;
      for (FacilitatorObtainment obtainment : node.getFacilitatorObtainmentsList()) {
        Facilitator facilitator = facilitatorRegistry.obtain(obtainment.getType());
        currFacilitatorResponse =
            facilitator.facilitate(ambiance, pmsNodeExecutionService.extractResolvedStepParameters(nodeExecution),
                obtainment.getParameters().toByteArray(), inputPackage);
        if (currFacilitatorResponse != null) {
          break;
        }
      }
      if (currFacilitatorResponse == null) {
        pmsNodeExecutionService.handleFacilitationResponse(nodeExecution.getUuid(), event.getNotifyId(),
            FacilitatorResponseProto.newBuilder().setIsSuccessful(false).build());
        return;
      }
      pmsNodeExecutionService.handleFacilitationResponse(nodeExecution.getUuid(), event.getNotifyId(),
          FacilitatorResponseMapper.toFacilitatorResponseProto(currFacilitatorResponse));
    } catch (Exception ex) {
      pmsNodeExecutionService.handleFacilitationResponse(event.getNodeExecution().getUuid(), event.getNotifyId(),
          FacilitatorResponseProto.newBuilder().setIsSuccessful(false).build());
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
        ErrorResponseData errorResponseData = (ErrorResponseData) eventData.getResponse().values().iterator().next();
        StepResponseProto stepResponse =
            StepResponseProto.newBuilder()
                .setStatus(Status.ERRORED)
                .setFailureInfo(FailureInfo.newBuilder()
                                    .addAllFailureTypes(
                                        EngineExceptionUtils.transformFailureTypes(errorResponseData.getFailureTypes()))
                                    .setErrorMessage(errorResponseData.getErrorMessage())
                                    .build())
                .build();
        pmsNodeExecutionService.handleStepResponse(nodeExecution.getUuid(), stepResponse);
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

  private void adviseExecution(NodeExecutionEvent event) {
    NodeExecutionProto nodeExecutionProto = event.getNodeExecution();
    PlanNodeProto planNodeProto = nodeExecutionProto.getNode();
    AdviseNodeExecutionEventData data = (AdviseNodeExecutionEventData) event.getEventData();
    AdviserResponse adviserResponse = null;
    for (AdviserObtainment obtainment : planNodeProto.getAdviserObtainmentsList()) {
      Adviser adviser = adviserRegistry.obtain(obtainment.getType());
      AdvisingEvent advisingEvent = AdvisingEvent.builder()
                                        .nodeExecution(nodeExecutionProto)
                                        .toStatus(data.getToStatus())
                                        .fromStatus(data.getFromStatus())
                                        .adviserParameters(obtainment.getParameters().toByteArray())
                                        .build();
      if (adviser.canAdvise(advisingEvent)) {
        adviserResponse = adviser.onAdviseEvent(advisingEvent);
        if (adviserResponse != null) {
          break;
        }
      }
    }

    if (adviserResponse != null) {
      pmsNodeExecutionService.handleAdviserResponse(nodeExecutionProto.getUuid(), event.getNotifyId(), adviserResponse);
    } else {
      pmsNodeExecutionService.handleAdviserResponse(nodeExecutionProto.getUuid(), event.getNotifyId(),
          AdviserResponse.newBuilder().setType(AdviseType.UNKNOWN).build());
    }
  }
}
