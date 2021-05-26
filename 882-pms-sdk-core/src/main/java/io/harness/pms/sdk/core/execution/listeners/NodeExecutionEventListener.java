package io.harness.pms.sdk.core.execution.listeners;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.ExceptionUtils;
import io.harness.logging.AutoLogContext;
import io.harness.pms.contracts.advisers.AdviseType;
import io.harness.pms.contracts.advisers.AdviserObtainment;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorResponseProto;
import io.harness.pms.contracts.plan.NodeExecutionEventType;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.contracts.steps.io.StepResponseProto;
import io.harness.pms.execution.AdviseNodeExecutionEventData;
import io.harness.pms.execution.NodeExecutionEvent;
import io.harness.pms.execution.ProgressNodeExecutionEventData;
import io.harness.pms.execution.ResumeNodeExecutionEventData;
import io.harness.pms.execution.StartNodeExecutionEventData;
import io.harness.pms.execution.utils.EngineExceptionUtils;
import io.harness.pms.gitsync.PmsGitSyncBranchContextGuard;
import io.harness.pms.gitsync.PmsGitSyncHelper;
import io.harness.pms.sdk.core.adviser.Adviser;
import io.harness.pms.sdk.core.adviser.AdvisingEvent;
import io.harness.pms.sdk.core.execution.EngineObtainmentHelper;
import io.harness.pms.sdk.core.execution.ExecutableProcessor;
import io.harness.pms.sdk.core.execution.ExecutableProcessorFactory;
import io.harness.pms.sdk.core.execution.InvokerPackage;
import io.harness.pms.sdk.core.execution.ProgressPackage;
import io.harness.pms.sdk.core.execution.ResumePackage;
import io.harness.pms.sdk.core.execution.SdkNodeExecutionService;
import io.harness.pms.sdk.core.facilitator.Facilitator;
import io.harness.pms.sdk.core.facilitator.FacilitatorResponse;
import io.harness.pms.sdk.core.facilitator.FacilitatorResponseMapper;
import io.harness.pms.sdk.core.registries.AdviserRegistry;
import io.harness.pms.sdk.core.registries.FacilitatorRegistry;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueListenerWithObservers;
import io.harness.serializer.KryoSerializer;
import io.harness.tasks.ErrorResponseData;
import io.harness.tasks.ProgressData;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
@Singleton
public class NodeExecutionEventListener extends QueueListenerWithObservers<NodeExecutionEvent> {
  @Inject private FacilitatorRegistry facilitatorRegistry;
  @Inject private AdviserRegistry adviserRegistry;
  @Inject private SdkNodeExecutionService sdkNodeExecutionService;
  @Inject private EngineObtainmentHelper engineObtainmentHelper;
  @Inject private ExecutableProcessorFactory executableProcessorFactory;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private PmsGitSyncHelper pmsGitSyncHelper;

  @Inject
  public NodeExecutionEventListener(QueueConsumer<NodeExecutionEvent> queueConsumer) {
    super(queueConsumer, false);
  }

  @Override
  public void onMessageInternal(NodeExecutionEvent event) {
    try (PmsGitSyncBranchContextGuard ignore1 =
             pmsGitSyncHelper.createGitSyncBranchContextGuard(event.getNodeExecution().getAmbiance(), true);
         AutoLogContext ignore2 = event.autoLogContext()) {
      onMessageInternalWithContext(event);
    }
  }

  private void onMessageInternalWithContext(NodeExecutionEvent event) {
    boolean handled;
    NodeExecutionEventType nodeExecutionEventType = event.getEventType();
    log.info("Starting to handle NodeExecutionEvent of type: {}", event.getEventType());
    switch (nodeExecutionEventType) {
      case START:
        handled = startExecution(event);
        break;
      case FACILITATE:
        handled = facilitateExecution(event);
        break;
      case RESUME:
        handled = resumeExecution(event);
        break;
      case ADVISE:
        handled = adviseExecution(event);
        break;
      case PROGRESS:
        handled = handleProgress(event);
        break;
      default:
        throw new UnsupportedOperationException("NodeExecution Event Has no handler" + event.getEventType());
    }
    log.info(
        "Handled NodeExecutionEvent of type: {} {}", event.getEventType(), handled ? "SUCCESSFULLY" : "UNSUCCESSFULLY");
  }

  private boolean handleProgress(NodeExecutionEvent event) {
    NodeExecutionProto nodeExecution = event.getNodeExecution();
    try {
      ExecutableProcessor processor = executableProcessorFactory.obtainProcessor(nodeExecution.getMode());
      ProgressNodeExecutionEventData eventData = (ProgressNodeExecutionEventData) event.getEventData();
      ProgressPackage progressPackage =
          ProgressPackage.builder()
              .nodeExecution(event.getNodeExecution())
              .progressData((ProgressData) kryoSerializer.asInflatedObject(eventData.getProgressBytes()))
              .build();
      processor.handleProgress(progressPackage);
      return true;
    } catch (Exception ex) {
      log.error("Error while Handling progress", ex);
      return false;
    }
  }

  private boolean facilitateExecution(NodeExecutionEvent event) {
    try {
      NodeExecutionProto nodeExecution = event.getNodeExecution();
      Ambiance ambiance = nodeExecution.getAmbiance();
      StepInputPackage inputPackage = obtainInputPackage(nodeExecution);
      PlanNodeProto node = nodeExecution.getNode();
      FacilitatorResponse currFacilitatorResponse = null;
      for (FacilitatorObtainment obtainment : node.getFacilitatorObtainmentsList()) {
        Facilitator facilitator = facilitatorRegistry.obtain(obtainment.getType());
        currFacilitatorResponse =
            facilitator.facilitate(ambiance, sdkNodeExecutionService.extractResolvedStepParameters(nodeExecution),
                obtainment.getParameters().toByteArray(), inputPackage);
        if (currFacilitatorResponse != null) {
          break;
        }
      }
      if (currFacilitatorResponse == null) {
        sdkNodeExecutionService.handleFacilitationResponse(nodeExecution.getUuid(), event.getNotifyId(),
            FacilitatorResponseProto.newBuilder().setIsSuccessful(false).build());
        return true;
      }
      sdkNodeExecutionService.handleFacilitationResponse(nodeExecution.getUuid(), event.getNotifyId(),
          FacilitatorResponseMapper.toFacilitatorResponseProto(currFacilitatorResponse));
      return true;
    } catch (Exception ex) {
      log.error("Error while facilitating execution", ex);
      sdkNodeExecutionService.handleEventError(event.getEventType(), event.getNotifyId(), constructFailureInfo(ex));
      return false;
    }
  }

  private boolean startExecution(NodeExecutionEvent event) {
    NodeExecutionProto nodeExecution = event.getNodeExecution();
    try {
      ExecutableProcessor processor = executableProcessorFactory.obtainProcessor(nodeExecution.getMode());
      StepInputPackage inputPackage = obtainInputPackage(nodeExecution);
      StartNodeExecutionEventData eventData = (StartNodeExecutionEventData) event.getEventData();
      FacilitatorResponse facilitatorResponse = eventData.getFacilitatorResponse() == null
          ? null
          : FacilitatorResponseMapper.fromFacilitatorResponseProto(eventData.getFacilitatorResponse());
      processor.handleStart(
          InvokerPackage.builder()
              .inputPackage(inputPackage)
              .passThroughData(facilitatorResponse == null ? null : facilitatorResponse.getPassThroughData())
              .nodeExecution(nodeExecution)
              .build());
      return true;
    } catch (Exception ex) {
      log.error("Error while starting execution", ex);
      sdkNodeExecutionService.handleStepResponse(nodeExecution.getUuid(), constructStepResponse(ex));
      return false;
    }
  }

  private boolean resumeExecution(NodeExecutionEvent event) {
    NodeExecutionProto nodeExecution = event.getNodeExecution();
    ExecutableProcessor processor = executableProcessorFactory.obtainProcessor(nodeExecution.getMode());
    ResumeNodeExecutionEventData eventData = (ResumeNodeExecutionEventData) event.getEventData();
    Map<String, ResponseData> response = new HashMap<>();
    if (EmptyPredicate.isNotEmpty(eventData.getResponse())) {
      eventData.getResponse().forEach((k, v) -> response.put(k, (ResponseData) kryoSerializer.asInflatedObject(v)));
    }
    try {
      if (eventData.isAsyncError()) {
        ErrorResponseData errorResponseData = (ErrorResponseData) response.values().iterator().next();
        StepResponseProto stepResponse =
            StepResponseProto.newBuilder()
                .setStatus(Status.ERRORED)
                .setFailureInfo(FailureInfo.newBuilder()
                                    .addAllFailureTypes(EngineExceptionUtils.transformToOrchestrationFailureTypes(
                                        errorResponseData.getFailureTypes()))
                                    .setErrorMessage(errorResponseData.getErrorMessage())
                                    .build())
                .build();
        sdkNodeExecutionService.handleStepResponse(nodeExecution.getUuid(), stepResponse);
        return true;
      }

      processor.handleResume(ResumePackage.builder().nodeExecution(nodeExecution).responseDataMap(response).build());
      return true;
    } catch (Exception ex) {
      log.error("Error while resuming execution", ex);
      sdkNodeExecutionService.handleStepResponse(nodeExecution.getUuid(), constructStepResponse(ex));
      return false;
    }
  }

  private StepInputPackage obtainInputPackage(NodeExecutionProto nodeExecution) {
    return engineObtainmentHelper.obtainInputPackage(
        nodeExecution.getAmbiance(), nodeExecution.getNode().getRebObjectsList());
  }

  private boolean adviseExecution(NodeExecutionEvent event) {
    try {
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
        sdkNodeExecutionService.handleAdviserResponse(
            nodeExecutionProto.getUuid(), event.getNotifyId(), adviserResponse);
      } else {
        sdkNodeExecutionService.handleAdviserResponse(nodeExecutionProto.getUuid(), event.getNotifyId(),
            AdviserResponse.newBuilder().setType(AdviseType.UNKNOWN).build());
      }
      return true;
    } catch (Exception ex) {
      log.error("Error while advising execution", ex);
      sdkNodeExecutionService.handleEventError(event.getEventType(), event.getNotifyId(), constructFailureInfo(ex));
      return false;
    }
  }

  private static FailureInfo constructFailureInfo(Exception ex) {
    return FailureInfo.newBuilder()
        .addAllFailureTypes(EngineExceptionUtils.getOrchestrationFailureTypes(ex))
        .setErrorMessage(ExceptionUtils.getMessage(ex))
        .build();
  }

  private static StepResponseProto constructStepResponse(Exception ex) {
    return StepResponseProto.newBuilder().setStatus(Status.FAILED).setFailureInfo(constructFailureInfo(ex)).build();
  }
}
