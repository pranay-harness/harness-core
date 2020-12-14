package io.harness.engine.executables.invokers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.pms.execution.Status.ABORTED;
import static io.harness.pms.execution.Status.QUEUED;
import static io.harness.pms.execution.Status.SUSPENDED;

import io.harness.OrchestrationPublisherName;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.EngineObtainmentHelper;
import io.harness.engine.executables.ExecuteStrategy;
import io.harness.engine.executables.InvocationHelper;
import io.harness.engine.executables.InvokerPackage;
import io.harness.engine.executables.ResumePackage;
import io.harness.engine.resume.EngineResumeCallback;
import io.harness.execution.NodeExecutionUtils;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.execution.ChildChainExecutableResponse;
import io.harness.pms.execution.ExecutableResponse;
import io.harness.pms.execution.NodeExecutionProto;
import io.harness.pms.execution.Status;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.execution.utils.LevelUtils;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.pms.plan.PlanNodeProto;
import io.harness.pms.sdk.core.execution.PmsNodeExecutionService;
import io.harness.pms.sdk.core.steps.executables.ChildChainExecutable;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.registries.StepRegistry;
import io.harness.serializer.KryoSerializer;
import io.harness.state.io.StepResponseNotifyData;
import io.harness.tasks.ResponseData;
import io.harness.waiter.NotifyCallback;
import io.harness.waiter.WaitNotifyEngine;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings({"rawtypes", "unchecked"})
@OwnedBy(CDC)
public class ChildChainStrategy implements ExecuteStrategy {
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private PmsNodeExecutionService pmsNodeExecutionService;
  @Inject private InvocationHelper invocationHelper;
  @Inject private StepRegistry stepRegistry;
  @Inject private EngineObtainmentHelper engineObtainmentHelper;
  @Inject @Named(OrchestrationPublisherName.PUBLISHER_NAME) String publisherName;
  @Inject private KryoSerializer kryoSerializer;

  @Override
  public void start(InvokerPackage invokerPackage) {
    NodeExecutionProto nodeExecution = invokerPackage.getNodeExecution();
    ChildChainExecutable childChainExecutable = extractExecutable(nodeExecution);
    ChildChainExecutableResponse childChainResponse;
    childChainResponse = childChainExecutable.executeFirstChild(nodeExecution.getAmbiance(),
        pmsNodeExecutionService.extractResolvedStepParameters(nodeExecution), invokerPackage.getInputPackage());
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
      accumulatedResponse = invocationHelper.accumulateResponses(
          ambiance.getPlanExecutionId(), resumePackage.getResponseDataMap().keySet().iterator().next());
    }
    byte[] passThrowDataBytes = lastChildChainExecutableResponse.getPassThroughData().toByteArray();
    PassThroughData passThroughData = passThrowDataBytes.length == 0 ? new PassThroughData() {
    } : (PassThroughData) kryoSerializer.asObject(passThrowDataBytes);
    if (lastChildChainExecutableResponse.getLastLink() || lastChildChainExecutableResponse.getSuspend()
        || isBroken(accumulatedResponse) || isAborted(accumulatedResponse)) {
      StepResponse stepResponse = childChainExecutable.finalizeExecution(ambiance,
          pmsNodeExecutionService.extractResolvedStepParameters(nodeExecution), passThroughData, accumulatedResponse);
      pmsNodeExecutionService.handleStepResponse(nodeExecution.getUuid(), stepResponse);
    } else {
      StepInputPackage inputPackage =
          engineObtainmentHelper.obtainInputPackage(ambiance, nodeExecution.getNode().getRebObjectsList());
      ChildChainExecutableResponse chainResponse = childChainExecutable.executeNextChild(ambiance,
          pmsNodeExecutionService.extractResolvedStepParameters(nodeExecution), inputPackage, passThroughData,
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
    pmsNodeExecutionService.queueNodeExecution(childNodeExecution);

    pmsNodeExecutionService.addExecutableResponse(nodeExecution.getUuid(), Status.UNRECOGNIZED,
        ExecutableResponse.newBuilder().setChildChain(childChainResponse).build(),
        Collections.singletonList(childInstanceId));
  }

  private void suspendChain(ChildChainExecutableResponse childChainResponse, NodeExecutionProto nodeExecution) {
    String ignoreNotifyId = "ignore-" + nodeExecution.getUuid();
    pmsNodeExecutionService.addExecutableResponse(nodeExecution.getUuid(), Status.UNRECOGNIZED,
        ExecutableResponse.newBuilder().setChildChain(childChainResponse).build(), Collections.emptyList());

    NotifyCallback callback = EngineResumeCallback.builder().nodeExecutionId(nodeExecution.getUuid()).build();
    waitNotifyEngine.waitForAllOn(publisherName, callback, ignoreNotifyId);
    PlanNodeProto planNode = nodeExecution.getNode();
    waitNotifyEngine.doneWith(ignoreNotifyId,
        StepResponseNotifyData.builder()
            .nodeUuid(planNode.getUuid())
            .identifier(planNode.getIdentifier())
            .group(planNode.getGroup())
            .status(SUSPENDED)
            .description("Ignoring Execution as next child found to be null")
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
