package io.harness.pms.sdk.core.execution.events.node.resume;

import static io.harness.pms.contracts.execution.Status.ABORTED;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ExecutionMode;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.plan.NodeExecutionEventType;
import io.harness.pms.contracts.resume.NodeResumeEvent;
import io.harness.pms.contracts.steps.io.StepResponseProto;
import io.harness.pms.events.base.PmsBaseEventHandler;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.execution.utils.EngineExceptionUtils;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.pms.sdk.core.execution.ChainDetails;
import io.harness.pms.sdk.core.execution.ChainDetails.ChainDetailsBuilder;
import io.harness.pms.sdk.core.execution.EngineObtainmentHelper;
import io.harness.pms.sdk.core.execution.ExecutableProcessor;
import io.harness.pms.sdk.core.execution.ExecutableProcessorFactory;
import io.harness.pms.sdk.core.execution.NodeExecutionUtils;
import io.harness.pms.sdk.core.execution.ResumePackage;
import io.harness.pms.sdk.core.execution.ResumePackage.ResumePackageBuilder;
import io.harness.pms.sdk.core.execution.SdkNodeExecutionService;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.sdk.core.steps.io.StepResponseNotifyData;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.serializer.KryoSerializer;
import io.harness.tasks.ErrorResponseData;
import io.harness.tasks.ResponseData;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
@Slf4j
public class NodeResumeEventHandler extends PmsBaseEventHandler<NodeResumeEvent> {
  @Inject private SdkNodeExecutionService sdkNodeExecutionService;
  @Inject private EngineObtainmentHelper engineObtainmentHelper;
  @Inject private ExecutableProcessorFactory executableProcessorFactory;
  @Inject private KryoSerializer kryoSerializer;

  @Override
  protected Map<String, String> extractMetricContext(NodeResumeEvent message) {
    return ImmutableMap.<String, String>builder()
        .put("accountId", AmbianceUtils.getAccountId(message.getAmbiance()))
        .put("projectIdentifier", AmbianceUtils.getOrgIdentifier(message.getAmbiance()))
        .put("orgIdentifier", AmbianceUtils.getProjectIdentifier(message.getAmbiance()))
        .build();
  }

  @Override
  protected String getMetricPrefix(NodeResumeEvent message) {
    return "progress_event";
  }

  @Override
  @NonNull
  protected Map<String, String> extraLogProperties(NodeResumeEvent event) {
    return ImmutableMap.<String, String>builder().put("eventType", NodeExecutionEventType.PROGRESS.name()).build();
  }

  @Override
  protected Ambiance extractAmbiance(NodeResumeEvent event) {
    return event.getAmbiance();
  }

  @Override
  protected void handleEventWithContext(NodeResumeEvent event) {
    ExecutableProcessor processor = executableProcessorFactory.obtainProcessor(event.getExecutionMode());
    Map<String, ResponseData> response = new HashMap<>();
    if (EmptyPredicate.isNotEmpty(event.getResponseMap())) {
      event.getResponseMap().forEach(
          (k, v) -> response.put(k, (ResponseData) kryoSerializer.asInflatedObject(v.toByteArray())));
    }

    String nodeExecutionId = AmbianceUtils.obtainCurrentRuntimeId(event.getAmbiance());
    Preconditions.checkArgument(isNotBlank(nodeExecutionId), "nodeExecutionId is null or empty");
    try {
      if (event.getAsyncError()) {
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
        sdkNodeExecutionService.handleStepResponse(nodeExecutionId, stepResponse);
        return;
      }

      processor.handleResume(buildResumePackage(event, response));
    } catch (Exception ex) {
      log.error("Error while resuming execution", ex);
      sdkNodeExecutionService.handleStepResponse(nodeExecutionId, NodeExecutionUtils.constructStepResponse(ex));
    }
  }

  private ResumePackage buildResumePackage(NodeResumeEvent event, Map<String, ResponseData> response) {
    StepParameters stepParameters =
        RecastOrchestrationUtils.fromDocumentJson(event.getStepParameters().toStringUtf8(), StepParameters.class);

    ResumePackageBuilder builder =
        ResumePackage.builder()
            .ambiance(event.getAmbiance())
            .stepParameters(stepParameters)
            .stepInputPackage(engineObtainmentHelper.obtainInputPackage(event.getAmbiance(), event.getRefObjectsList()))
            .responseDataMap(response);

    // TODO (prashant) : Change ChildChainResponse Pass through data handling
    if (event.hasChainDetails()) {
      io.harness.pms.contracts.resume.ChainDetails chainDetailsProto = event.getChainDetails();
      ChainDetailsBuilder chainDetailsBuilder = ChainDetails.builder().shouldEnd(calculateIsEnd(event, response));
      if (EmptyPredicate.isNotEmpty(chainDetailsProto.getPassThroughData())) {
        chainDetailsBuilder.passThroughData(
            (PassThroughData) kryoSerializer.asObject(chainDetailsProto.getPassThroughData().toByteArray()));
      }
      builder.chainDetails(chainDetailsBuilder.build());
    }
    return builder.build();
  }

  private boolean calculateIsEnd(NodeResumeEvent event, Map<String, ResponseData> response) {
    if (event.getExecutionMode() != ExecutionMode.CHILD_CHAIN) {
      return event.getChainDetails().getIsEnd();
    }
    return event.getChainDetails().getIsEnd() || isBroken(response) || isAborted(response);
  }

  private boolean isBroken(Map<String, ResponseData> accumulatedResponse) {
    return accumulatedResponse.values().stream().anyMatch(stepNotifyResponse
        -> StatusUtils.brokeStatuses().contains(((StepResponseNotifyData) stepNotifyResponse).getStatus()));
  }

  private boolean isAborted(Map<String, ResponseData> accumulatedResponse) {
    return accumulatedResponse.values().stream().anyMatch(
        stepNotifyResponse -> ABORTED == (((StepResponseNotifyData) stepNotifyResponse).getStatus()));
  }
}
