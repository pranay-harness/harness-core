package io.harness.engine.executables.invokers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.execution.status.Status.QUEUED;
import static io.harness.execution.status.Status.SUSPENDED;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.OrchestrationPublisherName;
import io.harness.ambiance.Ambiance;
import io.harness.ambiance.AmbianceUtils;
import io.harness.ambiance.Level;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.ExecutionEngineDispatcher;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executables.InvokeStrategy;
import io.harness.engine.executables.InvokerPackage;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.engine.resume.EngineResumeCallback;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.PlanExecution;
import io.harness.facilitator.modes.chain.child.ChildChainExecutable;
import io.harness.facilitator.modes.chain.child.ChildChainResponse;
import io.harness.plan.Plan;
import io.harness.plan.PlanNode;
import io.harness.state.io.StepResponseNotifyData;
import io.harness.waiter.NotifyCallback;
import io.harness.waiter.WaitNotifyEngine;

import java.util.concurrent.ExecutorService;

@SuppressWarnings({"rawtypes", "unchecked"})
@OwnedBy(CDC)
public class ChildChainStrategy implements InvokeStrategy {
  @Inject private OrchestrationEngine engine;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private PlanExecutionService planExecutionService;
  @Inject private AmbianceUtils ambianceUtils;
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;
  @Inject @Named(OrchestrationPublisherName.PUBLISHER_NAME) String publisherName;

  @Override
  public void invoke(InvokerPackage invokerPackage) {
    ChildChainExecutable childChainExecutable = (ChildChainExecutable) invokerPackage.getStep();
    Ambiance ambiance = invokerPackage.getAmbiance();
    ChildChainResponse childChainResponse;
    if (invokerPackage.isStart()) {
      childChainResponse = childChainExecutable.executeFirstChild(
          ambiance, invokerPackage.getParameters(), invokerPackage.getInputPackage());
    } else {
      childChainResponse = childChainExecutable.executeNextChild(ambiance, invokerPackage.getParameters(),
          invokerPackage.getInputPackage(), invokerPackage.getPassThroughData(), invokerPackage.getResponseDataMap());
    }
    handleResponse(ambiance, childChainResponse);
  }

  private void handleResponse(Ambiance ambiance, ChildChainResponse childChainResponse) {
    PlanExecution planExecution = planExecutionService.get(ambiance.getPlanExecutionId());
    NodeExecution nodeExecution = nodeExecutionService.get(ambiance.obtainCurrentRuntimeId());
    if (childChainResponse.isSuspend()) {
      suspendChain(childChainResponse, nodeExecution);
    } else {
      executeChild(ambiance, childChainResponse, planExecution, nodeExecution);
    }
  }

  private void executeChild(Ambiance ambiance, ChildChainResponse childChainResponse, PlanExecution planExecution,
      NodeExecution nodeExecution) {
    String childInstanceId = generateUuid();
    Plan plan = planExecution.getPlan();
    PlanNode node = plan.fetchNode(childChainResponse.getNextChildId());
    Ambiance clonedAmbiance = ambianceUtils.cloneForChild(ambiance);
    clonedAmbiance.addLevel(Level.fromPlanNode(childInstanceId, node));
    NodeExecution childNodeExecution = NodeExecution.builder()
                                           .uuid(childInstanceId)
                                           .node(node)
                                           .ambiance(clonedAmbiance)
                                           .status(QUEUED)
                                           .notifyId(childInstanceId)
                                           .parentId(nodeExecution.getUuid())
                                           .additionalInputs(childChainResponse.getAdditionalInputs())
                                           .build();
    nodeExecutionService.save(childNodeExecution);
    executorService.submit(
        ExecutionEngineDispatcher.builder().ambiance(clonedAmbiance).orchestrationEngine(engine).build());
    NotifyCallback callback = EngineResumeCallback.builder().nodeExecutionId(nodeExecution.getUuid()).build();
    waitNotifyEngine.waitForAllOn(publisherName, callback, childInstanceId);
    nodeExecutionService.update(
        nodeExecution.getUuid(), ops -> ops.addToSet(NodeExecutionKeys.executableResponses, childChainResponse));
  }

  private void suspendChain(ChildChainResponse childChainResponse, NodeExecution nodeExecution) {
    String ignoreNotifyId = "ignore-" + nodeExecution.getUuid();
    nodeExecutionService.update(
        nodeExecution.getUuid(), ops -> ops.addToSet(NodeExecutionKeys.executableResponses, childChainResponse));
    NotifyCallback callback = EngineResumeCallback.builder().nodeExecutionId(nodeExecution.getUuid()).build();
    waitNotifyEngine.waitForAllOn(publisherName, callback, ignoreNotifyId);
    PlanNode planNode = nodeExecution.getNode();
    waitNotifyEngine.doneWith(ignoreNotifyId,
        StepResponseNotifyData.builder()
            .nodeUuid(planNode.getUuid())
            .identifier(planNode.getIdentifier())
            .group(planNode.getGroup())
            .status(SUSPENDED)
            .description("Ignoring Execution as next child found to be null")
            .build());
  }
}
