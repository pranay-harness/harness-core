package io.harness.engine.executables.handlers;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.execution.status.Status.QUEUED;
import static io.harness.waiter.OrchestrationNotifyEventListener.ORCHESTRATION;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.ambiance.Ambiance;
import io.harness.ambiance.Level;
import io.harness.engine.AmbianceHelper;
import io.harness.engine.ExecutionEngine;
import io.harness.engine.ExecutionEngineDispatcher;
import io.harness.engine.executables.ExecutableInvoker;
import io.harness.engine.executables.InvokerPackage;
import io.harness.engine.resume.EngineResumeCallback;
import io.harness.engine.services.NodeExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.PlanExecution;
import io.harness.facilitator.modes.chain.child.ChildChainExecutable;
import io.harness.facilitator.modes.chain.child.ChildChainResponse;
import io.harness.persistence.HPersistence;
import io.harness.plan.Plan;
import io.harness.plan.PlanNode;
import io.harness.waiter.NotifyCallback;
import io.harness.waiter.WaitNotifyEngine;

import java.util.concurrent.ExecutorService;

public class ChildChainExecutableInvoker implements ExecutableInvoker {
  @Inject private ExecutionEngine engine;
  @Inject private AmbianceHelper ambianceHelper;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject @Named("enginePersistence") private HPersistence hPersistence;
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;

  @Override
  public void invokeExecutable(InvokerPackage invokerPackage) {
    ChildChainExecutable childChainExecutable = (ChildChainExecutable) invokerPackage.getStep();
    Ambiance ambiance = invokerPackage.getAmbiance();
    ChildChainResponse childChainResponse;
    if (invokerPackage.isStart()) {
      childChainResponse =
          childChainExecutable.executeFirstChild(ambiance, invokerPackage.getParameters(), invokerPackage.getInputs());
    } else {
      childChainResponse = childChainExecutable.executeNextChild(ambiance, invokerPackage.getParameters(),
          invokerPackage.getInputs(), invokerPackage.getPassThroughData(), invokerPackage.getResponseDataMap());
    }
    handleResponse(ambiance, childChainResponse);
  }

  private void handleResponse(Ambiance ambiance, ChildChainResponse childChainResponse) {
    String childInstanceId = generateUuid();
    PlanExecution planExecution = ambianceHelper.obtainExecutionInstance(ambiance);
    NodeExecution nodeExecution = ambianceHelper.obtainNodeExecution(ambiance);
    Plan plan = planExecution.getPlan();
    PlanNode node = plan.fetchNode(childChainResponse.getChildNodeId());
    Ambiance clonedAmbiance = ambiance.cloneForChild();
    clonedAmbiance.addLevel(Level.builder()
                                .setupId(node.getUuid())
                                .runtimeId(childInstanceId)
                                .identifier(node.getIdentifier())
                                .stepType(node.getStepType())
                                .build());
    NodeExecution childNodeExecution = NodeExecution.builder()
                                           .uuid(childInstanceId)
                                           .node(node)
                                           .planExecutionId(nodeExecution.getPlanExecutionId())
                                           .levels(clonedAmbiance.getLevels())
                                           .status(QUEUED)
                                           .notifyId(childInstanceId)
                                           .parentId(nodeExecution.getUuid())
                                           .build();
    hPersistence.save(childNodeExecution);
    executorService.submit(ExecutionEngineDispatcher.builder()
                               .ambiance(clonedAmbiance)
                               .executionEngine(engine)
                               .additionalInputs(childChainResponse.getAdditionalInputs())
                               .build());
    NotifyCallback callback = EngineResumeCallback.builder().nodeInstanceId(nodeExecution.getUuid()).build();
    waitNotifyEngine.waitForAllOn(ORCHESTRATION, callback, childInstanceId);
    nodeExecutionService.update(
        nodeExecution.getUuid(), ops -> ops.addToSet(NodeExecutionKeys.executableResponses, childChainResponse));
  }
}
