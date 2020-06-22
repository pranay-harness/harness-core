package io.harness.engine.executables.invokers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.waiter.OrchestrationNotifyEventListener.ORCHESTRATION;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.ambiance.Ambiance;
import io.harness.ambiance.Level;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.ExecutionEngine;
import io.harness.engine.ExecutionEngineDispatcher;
import io.harness.engine.executables.InvokeStrategy;
import io.harness.engine.executables.InvokerPackage;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.engine.resume.EngineResumeCallback;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.PlanExecution;
import io.harness.execution.status.Status;
import io.harness.facilitator.modes.children.ChildrenExecutable;
import io.harness.facilitator.modes.children.ChildrenExecutableResponse;
import io.harness.facilitator.modes.children.ChildrenExecutableResponse.Child;
import io.harness.plan.Plan;
import io.harness.plan.PlanNode;
import io.harness.waiter.NotifyCallback;
import io.harness.waiter.WaitNotifyEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

@OwnedBy(CDC)
@Redesign
public class ChildrenStrategy implements InvokeStrategy {
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private ExecutionEngine engine;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private PlanExecutionService planExecutionService;
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;

  @Override
  public void invoke(InvokerPackage invokerPackage) {
    Ambiance ambiance = invokerPackage.getAmbiance();
    ChildrenExecutable childrenExecutable = (ChildrenExecutable) invokerPackage.getStep();
    ChildrenExecutableResponse response =
        childrenExecutable.obtainChildren(ambiance, invokerPackage.getParameters(), invokerPackage.getInputs());
    handleResponse(ambiance, response);
  }

  private void handleResponse(Ambiance ambiance, ChildrenExecutableResponse response) {
    PlanExecution planExecution = planExecutionService.get(ambiance.getPlanExecutionId());
    NodeExecution nodeExecution = nodeExecutionService.get(ambiance.obtainCurrentRuntimeId());
    Plan plan = planExecution.getPlan();
    List<String> callbackIds = new ArrayList<>();
    for (Child child : response.getChildren()) {
      String uuid = generateUuid();
      callbackIds.add(uuid);
      PlanNode node = plan.fetchNode(child.getChildNodeId());
      Ambiance clonedAmbiance = ambiance.cloneForChild();
      clonedAmbiance.addLevel(Level.fromPlanNode(uuid, node));
      NodeExecution childNodeExecution = NodeExecution.builder()
                                             .uuid(uuid)
                                             .node(node)
                                             .ambiance(clonedAmbiance)
                                             .status(Status.QUEUED)
                                             .notifyId(uuid)
                                             .parentId(nodeExecution.getUuid())
                                             .additionalInputs(child.getAdditionalInputs())
                                             .build();
      nodeExecutionService.save(childNodeExecution);
      executorService.submit(
          ExecutionEngineDispatcher.builder().ambiance(clonedAmbiance).executionEngine(engine).build());
    }
    NotifyCallback callback = EngineResumeCallback.builder().nodeExecutionId(nodeExecution.getUuid()).build();
    waitNotifyEngine.waitForAllOn(ORCHESTRATION, callback, callbackIds.toArray(new String[0]));
    nodeExecutionService.update(
        nodeExecution.getUuid(), ops -> ops.addToSet(NodeExecutionKeys.executableResponses, response));
  }
}
