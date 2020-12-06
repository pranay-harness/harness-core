package io.harness.engine.executables.invokers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.pms.execution.Status.ABORTED;
import static io.harness.pms.execution.Status.QUEUED;
import static io.harness.pms.execution.Status.SUSPENDED;

import io.harness.OrchestrationPublisherName;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.EngineObtainmentHelper;
import io.harness.engine.ExecutionEngineDispatcher;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executables.ExecuteStrategy;
import io.harness.engine.executables.InvocationHelper;
import io.harness.engine.executables.InvokerPackage;
import io.harness.engine.executables.ResumePackage;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.engine.resume.EngineResumeCallback;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.PlanExecution;
import io.harness.plan.Plan;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.execution.ChildChainExecutableResponse;
import io.harness.pms.execution.ExecutableResponse;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.execution.utils.LevelUtils;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.pms.plan.PlanNodeProto;
import io.harness.pms.sdk.core.registries.StepRegistry;
import io.harness.pms.sdk.core.steps.executables.ChildChainExecutable;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.serializer.KryoSerializer;
import io.harness.state.io.StepResponseNotifyData;
import io.harness.tasks.ResponseData;
import io.harness.waiter.NotifyCallback;
import io.harness.waiter.WaitNotifyEngine;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

@SuppressWarnings({"rawtypes", "unchecked"})
@OwnedBy(CDC)
public class ChildChainStrategy implements ExecuteStrategy {
  @Inject private OrchestrationEngine engine;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private PlanExecutionService planExecutionService;
  @Inject private InvocationHelper invocationHelper;
  @Inject private StepRegistry stepRegistry;
  @Inject private EngineObtainmentHelper engineObtainmentHelper;
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;
  @Inject @Named(OrchestrationPublisherName.PUBLISHER_NAME) String publisherName;
  @Inject private KryoSerializer kryoSerializer;

  @Override
  public void start(InvokerPackage invokerPackage) {
    NodeExecution nodeExecution = invokerPackage.getNodeExecution();
    ChildChainExecutable childChainExecutable = extractExecutable(nodeExecution);
    Ambiance ambiance = nodeExecution.getAmbiance();
    ChildChainExecutableResponse childChainResponse;
    childChainResponse = childChainExecutable.executeFirstChild(
        ambiance, nodeExecutionService.extractResolvedStepParameters(nodeExecution), invokerPackage.getInputPackage());
    handleResponse(ambiance, childChainResponse);
  }

  @Override
  public void resume(ResumePackage resumePackage) {
    NodeExecution nodeExecution = resumePackage.getNodeExecution();
    Ambiance ambiance = nodeExecution.getAmbiance();
    ChildChainExecutable childChainExecutable = extractExecutable(nodeExecution);
    ChildChainExecutableResponse lastChildChainExecutableResponse = Preconditions.checkNotNull(
        Objects.requireNonNull(nodeExecution.obtainLatestExecutableResponse()).getChildChain());
    Map<String, ResponseData> accumulatedResponse = resumePackage.getResponseDataMap();
    if (!lastChildChainExecutableResponse.getSuspend()) {
      accumulatedResponse = invocationHelper.accumulateResponses(
          ambiance.getPlanExecutionId(), resumePackage.getResponseDataMap().keySet().iterator().next());
    }
    if (lastChildChainExecutableResponse.getLastLink() || lastChildChainExecutableResponse.getSuspend()
        || isBroken(accumulatedResponse) || isAborted(accumulatedResponse)) {
      StepResponse stepResponse = childChainExecutable.finalizeExecution(ambiance,
          nodeExecutionService.extractResolvedStepParameters(nodeExecution),
          (PassThroughData) kryoSerializer.asObject(
              lastChildChainExecutableResponse.getPassThroughData().toByteArray()),
          accumulatedResponse);
      engine.handleStepResponse(nodeExecution.getUuid(), stepResponse);
    } else {
      StepInputPackage inputPackage =
          engineObtainmentHelper.obtainInputPackage(ambiance, nodeExecution.getNode().getRebObjectsList());
      ChildChainExecutableResponse chainResponse = childChainExecutable.executeNextChild(ambiance,
          nodeExecutionService.extractResolvedStepParameters(nodeExecution), inputPackage,
          (PassThroughData) kryoSerializer.asObject(
              lastChildChainExecutableResponse.getPassThroughData().toByteArray()),
          accumulatedResponse);
      handleResponse(ambiance, chainResponse);
    }
  }

  ChildChainExecutable extractExecutable(NodeExecution nodeExecution) {
    PlanNodeProto node = nodeExecution.getNode();
    return (ChildChainExecutable) stepRegistry.obtain(node.getStepType());
  }

  private void handleResponse(Ambiance ambiance, ChildChainExecutableResponse childChainResponse) {
    PlanExecution planExecution = planExecutionService.get(ambiance.getPlanExecutionId());
    NodeExecution nodeExecution = nodeExecutionService.get(AmbianceUtils.obtainCurrentRuntimeId(ambiance));
    if (childChainResponse.getSuspend()) {
      suspendChain(childChainResponse, nodeExecution);
    } else {
      executeChild(ambiance, childChainResponse, planExecution, nodeExecution);
    }
  }

  private void executeChild(Ambiance ambiance, ChildChainExecutableResponse childChainResponse,
      PlanExecution planExecution, NodeExecution nodeExecution) {
    String childInstanceId = generateUuid();
    Plan plan = planExecution.getPlan();
    PlanNodeProto node = plan.fetchNode(childChainResponse.getNextChildId());
    Ambiance clonedAmbiance =
        AmbianceUtils.cloneForChild(ambiance, LevelUtils.buildLevelFromPlanNode(childInstanceId, node));
    NodeExecution childNodeExecution = NodeExecution.builder()
                                           .uuid(childInstanceId)
                                           .node(node)
                                           .ambiance(clonedAmbiance)
                                           .status(QUEUED)
                                           .notifyId(childInstanceId)
                                           .parentId(nodeExecution.getUuid())
                                           .progressDataMap(new LinkedHashMap<>())
                                           .build();
    nodeExecutionService.save(childNodeExecution);
    executorService.submit(
        ExecutionEngineDispatcher.builder().ambiance(clonedAmbiance).orchestrationEngine(engine).build());
    NotifyCallback callback = EngineResumeCallback.builder().nodeExecutionId(nodeExecution.getUuid()).build();
    waitNotifyEngine.waitForAllOn(publisherName, callback, childInstanceId);
    nodeExecutionService.update(nodeExecution.getUuid(),
        ops
        -> ops.addToSet(NodeExecutionKeys.executableResponses,
            ExecutableResponse.newBuilder().setChildChain(childChainResponse).build()));
  }

  private void suspendChain(ChildChainExecutableResponse childChainResponse, NodeExecution nodeExecution) {
    String ignoreNotifyId = "ignore-" + nodeExecution.getUuid();
    nodeExecutionService.update(nodeExecution.getUuid(),
        ops
        -> ops.addToSet(NodeExecutionKeys.executableResponses,
            ExecutableResponse.newBuilder().setChildChain(childChainResponse).build()));
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
