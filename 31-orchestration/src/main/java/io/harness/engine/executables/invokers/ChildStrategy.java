package io.harness.engine.executables.invokers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.OrchestrationPublisherName;
import io.harness.ambiance.Ambiance;
import io.harness.ambiance.Level;
import io.harness.annotations.Redesign;
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
import io.harness.execution.status.Status;
import io.harness.facilitator.modes.child.ChildExecutable;
import io.harness.facilitator.modes.child.ChildExecutableResponse;
import io.harness.plan.Plan;
import io.harness.plan.PlanNode;
import io.harness.waiter.NotifyCallback;
import io.harness.waiter.WaitNotifyEngine;

import java.util.concurrent.ExecutorService;

@OwnedBy(CDC)
@Redesign
public class ChildStrategy implements InvokeStrategy {
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private PlanExecutionService planExecutionService;
  @Inject private OrchestrationEngine engine;
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;
  @Inject @Named(OrchestrationPublisherName.PUBLISHER_NAME) String publisherName;

  @Override
  public void invoke(InvokerPackage invokerPackage) {
    Ambiance ambiance = invokerPackage.getAmbiance();
    ChildExecutable childExecutable = (ChildExecutable) invokerPackage.getStep();
    ChildExecutableResponse response =
        childExecutable.obtainChild(ambiance, invokerPackage.getParameters(), invokerPackage.getInputPackage());
    handleResponse(ambiance, response);
  }

  private void handleResponse(Ambiance ambiance, ChildExecutableResponse response) {
    String childInstanceId = generateUuid();
    PlanExecution planExecution = planExecutionService.get(ambiance.getPlanExecutionId());
    NodeExecution nodeExecution = nodeExecutionService.get(ambiance.obtainCurrentRuntimeId());
    Plan plan = planExecution.getPlan();
    PlanNode node = plan.fetchNode(response.getChildNodeId());
    Ambiance clonedAmbiance = ambiance.cloneForChild();
    clonedAmbiance.addLevel(Level.fromPlanNode(childInstanceId, node));
    NodeExecution childNodeExecution = NodeExecution.builder()
                                           .uuid(childInstanceId)
                                           .node(node)
                                           .ambiance(clonedAmbiance)
                                           .status(Status.QUEUED)
                                           .notifyId(childInstanceId)
                                           .parentId(nodeExecution.getUuid())
                                           .additionalInputs(response.getAdditionalInputs())
                                           .build();
    nodeExecutionService.save(childNodeExecution);
    executorService.submit(
        ExecutionEngineDispatcher.builder().ambiance(clonedAmbiance).orchestrationEngine(engine).build());
    NotifyCallback callback = EngineResumeCallback.builder().nodeExecutionId(nodeExecution.getUuid()).build();
    waitNotifyEngine.waitForAllOn(publisherName, callback, childInstanceId);
    nodeExecutionService.update(
        nodeExecution.getUuid(), ops -> ops.addToSet(NodeExecutionKeys.executableResponses, response));
  }
}
