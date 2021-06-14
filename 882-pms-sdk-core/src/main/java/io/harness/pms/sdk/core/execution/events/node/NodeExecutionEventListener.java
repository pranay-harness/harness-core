package io.harness.pms.sdk.core.execution.events.node;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.pms.contracts.execution.Status.ABORTED;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.logging.AutoLogContext;
import io.harness.manage.GlobalContextManager;
import io.harness.monitoring.MonitoringContext;
import io.harness.pms.contracts.advisers.AdviseType;
import io.harness.pms.contracts.advisers.AdviserObtainment;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.execution.ChildChainExecutableResponse;
import io.harness.pms.contracts.execution.ExecutionMode;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.TaskChainExecutableResponse;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.plan.NodeExecutionEventType;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.contracts.steps.io.StepResponseProto;
import io.harness.pms.execution.AdviseNodeExecutionEventData;
import io.harness.pms.execution.NodeExecutionEvent;
import io.harness.pms.execution.ProgressNodeExecutionEventData;
import io.harness.pms.execution.ResumeNodeExecutionEventData;
import io.harness.pms.execution.utils.EngineExceptionUtils;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.pms.gitsync.PmsGitSyncBranchContextGuard;
import io.harness.pms.gitsync.PmsGitSyncHelper;
import io.harness.pms.sdk.core.adviser.Adviser;
import io.harness.pms.sdk.core.adviser.AdvisingEvent;
import io.harness.pms.sdk.core.execution.ChainDetails;
import io.harness.pms.sdk.core.execution.EngineObtainmentHelper;
import io.harness.pms.sdk.core.execution.ExecutableProcessor;
import io.harness.pms.sdk.core.execution.ExecutableProcessorFactory;
import io.harness.pms.sdk.core.execution.NodeExecutionUtils;
import io.harness.pms.sdk.core.execution.ProgressPackage;
import io.harness.pms.sdk.core.execution.ResumePackage;
import io.harness.pms.sdk.core.execution.ResumePackage.ResumePackageBuilder;
import io.harness.pms.sdk.core.execution.SdkNodeExecutionService;
import io.harness.pms.sdk.core.registries.AdviserRegistry;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepResponseNotifyData;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueListenerWithObservers;
import io.harness.serializer.KryoSerializer;
import io.harness.tasks.ErrorResponseData;
import io.harness.tasks.ProgressData;
import io.harness.tasks.ResponseData;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PIPELINE)
@Slf4j
@Singleton
public class NodeExecutionEventListener extends QueueListenerWithObservers<NodeExecutionEvent> {
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
      GlobalContextManager.upsertGlobalContextRecord(
          MonitoringContext.builder().isMonitoringEnabled(event.isMonitoringEnabled()).build());
      onMessageInternalWithContext(event);
    }
  }

  private void onMessageInternalWithContext(NodeExecutionEvent event) {
    boolean handled;
    NodeExecutionEventType nodeExecutionEventType = event.getEventType();
    log.info("Starting to handle NodeExecutionEvent of type: {}", event.getEventType());
    switch (nodeExecutionEventType) {
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
              .ambiance(nodeExecution.getAmbiance())
              .stepParameters(sdkNodeExecutionService.extractResolvedStepParameters(nodeExecution))
              .progressData((ProgressData) kryoSerializer.asInflatedObject(eventData.getProgressBytes()))
              .build();
      processor.handleProgress(progressPackage);
      return true;
    } catch (Exception ex) {
      log.error("Error while Handling progress", ex);
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

      processor.handleResume(buildResumePackage(nodeExecution, response));
      return true;
    } catch (Exception ex) {
      log.error("Error while resuming execution", ex);
      sdkNodeExecutionService.handleStepResponse(nodeExecution.getUuid(), NodeExecutionUtils.constructStepResponse(ex));
      return false;
    }
  }

  private ResumePackage buildResumePackage(NodeExecutionProto nodeExecution, Map<String, ResponseData> response) {
    ResumePackageBuilder builder =
        ResumePackage.builder()
            .ambiance(nodeExecution.getAmbiance())
            .stepParameters(sdkNodeExecutionService.extractResolvedStepParameters(nodeExecution))
            .stepInputPackage(engineObtainmentHelper.obtainInputPackage(
                nodeExecution.getAmbiance(), nodeExecution.getNode().getRebObjectsList()))
            .responseDataMap(response);

    if (nodeExecution.getMode() == ExecutionMode.TASK_CHAIN || nodeExecution.getMode() == ExecutionMode.CHILD_CHAIN) {
      ExecutionMode mode = nodeExecution.getMode();
      switch (mode) {
        case TASK_CHAIN:
          TaskChainExecutableResponse lastLinkResponse =
              Objects.requireNonNull(NodeExecutionUtils.obtainLatestExecutableResponse(nodeExecution)).getTaskChain();
          builder
              .chainDetails(ChainDetails.builder()
                                .shouldEnd(lastLinkResponse.getChainEnd())
                                .passThroughData((PassThroughData) kryoSerializer.asObject(
                                    lastLinkResponse.getPassThroughData().toByteArray()))
                                .build())
              .build();
          break;
        case CHILD_CHAIN:
          ChildChainExecutableResponse lastChildChainExecutableResponse = Preconditions.checkNotNull(
              Objects.requireNonNull(NodeExecutionUtils.obtainLatestExecutableResponse(nodeExecution)).getChildChain());

          byte[] passThrowDataBytes = lastChildChainExecutableResponse.getPassThroughData().toByteArray();
          PassThroughData passThroughData = passThrowDataBytes.length == 0 ? new PassThroughData() {
          } : (PassThroughData) kryoSerializer.asObject(passThrowDataBytes);
          boolean chainEnd = lastChildChainExecutableResponse.getLastLink()
              || lastChildChainExecutableResponse.getSuspend() || isBroken(response) || isAborted(response);
          builder.chainDetails(ChainDetails.builder().shouldEnd(chainEnd).passThroughData(passThroughData).build());
          break;
        default:
          log.error("This Should Not Happen not a chain mode");
      }
    }
    return builder.build();
  }

  private boolean isBroken(Map<String, ResponseData> accumulatedResponse) {
    return accumulatedResponse.values().stream().anyMatch(stepNotifyResponse
        -> StatusUtils.brokeStatuses().contains(((StepResponseNotifyData) stepNotifyResponse).getStatus()));
  }

  private boolean isAborted(Map<String, ResponseData> accumulatedResponse) {
    return accumulatedResponse.values().stream().anyMatch(
        stepNotifyResponse -> ABORTED == (((StepResponseNotifyData) stepNotifyResponse).getStatus()));
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
      sdkNodeExecutionService.handleEventError(
          event.getEventType(), event.getNotifyId(), NodeExecutionUtils.constructFailureInfo(ex));
      return false;
    }
  }
}
