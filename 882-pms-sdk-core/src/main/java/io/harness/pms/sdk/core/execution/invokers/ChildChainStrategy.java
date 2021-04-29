package io.harness.pms.sdk.core.execution.invokers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.pms.contracts.execution.Status.ABORTED;
import static io.harness.pms.contracts.execution.Status.QUEUED;
import static io.harness.pms.contracts.execution.Status.SUSPENDED;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ChildChainExecutableResponse;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.events.AddExecutableResponseRequest;
import io.harness.pms.contracts.execution.events.QueueNodeExecutionRequest;
import io.harness.pms.contracts.execution.events.SuspendChainRequest;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.execution.utils.LevelUtils;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.pms.sdk.core.execution.EngineObtainmentHelper;
import io.harness.pms.sdk.core.execution.ExecuteStrategy;
import io.harness.pms.sdk.core.execution.InvokerPackage;
import io.harness.pms.sdk.core.execution.NodeExecutionUtils;
import io.harness.pms.sdk.core.execution.ResumePackage;
import io.harness.pms.sdk.core.execution.SdkNodeExecutionService;
import io.harness.pms.sdk.core.registries.StepRegistry;
import io.harness.pms.sdk.core.steps.executables.ChildChainExecutable;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.ResponseDataMapper;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponseMapper;
import io.harness.pms.sdk.core.steps.io.StepResponseNotifyData;
import io.harness.serializer.KryoSerializer;
import io.harness.tasks.ResponseData;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings({"rawtypes", "unchecked"})
@OwnedBy(CDC)
public class ChildChainStrategy implements ExecuteStrategy {
  @Inject private SdkNodeExecutionService sdkNodeExecutionService;
  @Inject private StepRegistry stepRegistry;
  @Inject private EngineObtainmentHelper engineObtainmentHelper;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private StrategyHelper strategyHelper;
  @Inject private ResponseDataMapper responseDataMapper;

  @Override
  public void start(InvokerPackage invokerPackage) {
    NodeExecutionProto nodeExecution = invokerPackage.getNodeExecution();
    ChildChainExecutable childChainExecutable = extractExecutable(nodeExecution);
    ChildChainExecutableResponse childChainResponse;
    childChainResponse = childChainExecutable.executeFirstChild(nodeExecution.getAmbiance(),
        sdkNodeExecutionService.extractResolvedStepParameters(nodeExecution), invokerPackage.getInputPackage());
    handleResponse(nodeExecution, invokerPackage.getNodes(), childChainResponse);
  }

  @Override
  public void resume(ResumePackage resumePackage) {
    NodeExecutionProto nodeExecution = resumePackage.getNodeExecution();
    Ambiance ambiance = nodeExecution.getAmbiance();
    ChildChainExecutable childChainExecutable = extractExecutable(nodeExecution);
    ChildChainExecutableResponse lastChildChainExecutableResponse = Preconditions.checkNotNull(
        Objects.requireNonNull(NodeExecutionUtils.obtainLatestExecutableResponse(nodeExecution)).getChildChain());
    Map<String, ResponseData> accumulatedResponse = resumePackage.getResponseDataMap();
    if (!lastChildChainExecutableResponse.getSuspend()) {
      accumulatedResponse = sdkNodeExecutionService.accumulateResponses(
          ambiance.getPlanExecutionId(), resumePackage.getResponseDataMap().keySet().iterator().next());
    }
    byte[] passThrowDataBytes = lastChildChainExecutableResponse.getPassThroughData().toByteArray();
    PassThroughData passThroughData = passThrowDataBytes.length == 0 ? new PassThroughData() {
    } : (PassThroughData) kryoSerializer.asObject(passThrowDataBytes);
    if (lastChildChainExecutableResponse.getLastLink() || lastChildChainExecutableResponse.getSuspend()
        || isBroken(accumulatedResponse) || isAborted(accumulatedResponse)) {
      StepResponse stepResponse = childChainExecutable.finalizeExecution(ambiance,
          sdkNodeExecutionService.extractResolvedStepParameters(nodeExecution), passThroughData, accumulatedResponse);
      sdkNodeExecutionService.handleStepResponse(
          nodeExecution.getUuid(), StepResponseMapper.toStepResponseProto(stepResponse));
    } else {
      StepInputPackage inputPackage =
          engineObtainmentHelper.obtainInputPackage(ambiance, nodeExecution.getNode().getRebObjectsList());
      ChildChainExecutableResponse chainResponse = childChainExecutable.executeNextChild(ambiance,
          sdkNodeExecutionService.extractResolvedStepParameters(nodeExecution), inputPackage, passThroughData,
          accumulatedResponse);
      handleResponse(nodeExecution, resumePackage.getNodes(), chainResponse);
    }
  }

  ChildChainExecutable extractExecutable(NodeExecutionProto nodeExecution) {
    PlanNodeProto node = nodeExecution.getNode();
    return (ChildChainExecutable) stepRegistry.obtain(node.getStepType());
  }

  private void handleResponse(
      NodeExecutionProto nodeExecution, List<PlanNodeProto> nodes, ChildChainExecutableResponse childChainResponse) {
    Ambiance ambiance = nodeExecution.getAmbiance();
    if (childChainResponse.getSuspend()) {
      suspendChain(childChainResponse, nodeExecution);
    } else {
      executeChild(ambiance, childChainResponse, nodes, nodeExecution);
    }
  }

  private void executeChild(Ambiance ambiance, ChildChainExecutableResponse childChainResponse,
      List<PlanNodeProto> nodes, NodeExecutionProto nodeExecution) {
    String childInstanceId = generateUuid();
    PlanNodeProto node = findNode(nodes, childChainResponse.getNextChildId());
    Ambiance clonedAmbiance =
        AmbianceUtils.cloneForChild(ambiance, LevelUtils.buildLevelFromPlanNode(childInstanceId, node));
    NodeExecutionProto childNodeExecution = NodeExecutionProto.newBuilder()
                                                .setUuid(childInstanceId)
                                                .setNode(node)
                                                .setAmbiance(clonedAmbiance)
                                                .setStatus(QUEUED)
                                                .setNotifyId(childInstanceId)
                                                .setParentId(nodeExecution.getUuid())
                                                .build();

    QueueNodeExecutionRequest queueNodeExecutionRequest =
        strategyHelper.getQueueNodeExecutionRequest(childNodeExecution);
    AddExecutableResponseRequest addExecutableResponseRequest =
        strategyHelper.getAddExecutableResponseRequest(nodeExecution.getUuid(), Status.NO_OP,
            ExecutableResponse.newBuilder().setChildChain(childChainResponse).build(),
            Collections.singletonList(childInstanceId));
    sdkNodeExecutionService.queueNodeExecutionAndAddExecutableResponse(
        nodeExecution.getUuid(), queueNodeExecutionRequest, addExecutableResponseRequest);
  }

  private void suspendChain(ChildChainExecutableResponse childChainResponse, NodeExecutionProto nodeExecution) {
    PlanNodeProto planNode = nodeExecution.getNode();
    Map<String, ByteString> responseBytes =
        responseDataMapper.toResponseDataProto(Collections.singletonMap("ignore-" + nodeExecution.getUuid(),
            StepResponseNotifyData.builder()
                .nodeUuid(planNode.getUuid())
                .identifier(planNode.getIdentifier())
                .group(planNode.getGroup())
                .status(SUSPENDED)
                .description("Ignoring Execution as next child found to be null")
                .build()));
    sdkNodeExecutionService.suspendChainExecution(nodeExecution.getUuid(),
        SuspendChainRequest.newBuilder()
            .setNodeExecutionId(nodeExecution.getUuid())
            .setExecutableResponse(ExecutableResponse.newBuilder().setChildChain(childChainResponse).build())
            .setIsError(false)
            .putAllResponse(responseBytes)
            .build());
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
