package io.harness.engine.executables.handlers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.waiter.OrchestrationNotifyEventListener.ORCHESTRATION;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.ambiance.Ambiance;
import io.harness.ambiance.Level;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
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
import io.harness.execution.status.Status;
import io.harness.facilitator.modes.child.ChildExecutable;
import io.harness.facilitator.modes.child.ChildExecutableResponse;
import io.harness.persistence.HPersistence;
import io.harness.plan.Plan;
import io.harness.plan.PlanNode;
import io.harness.waiter.NotifyCallback;
import io.harness.waiter.WaitNotifyEngine;

import java.util.concurrent.ExecutorService;

@OwnedBy(CDC)
@Redesign
public class ChildExecutableInvoker implements ExecutableInvoker {
  @Inject private AmbianceHelper ambianceHelper;
  @Inject @Named("enginePersistence") private HPersistence hPersistence;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private ExecutionEngine engine;
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;

  @Override
  public void invokeExecutable(InvokerPackage invokerPackage) {
    Ambiance ambiance = invokerPackage.getAmbiance();
    ChildExecutable childExecutable = (ChildExecutable) invokerPackage.getStep();
    ChildExecutableResponse response =
        childExecutable.obtainChild(ambiance, invokerPackage.getParameters(), invokerPackage.getInputs());
    handleResponse(ambiance, response);
  }

  private void handleResponse(Ambiance ambiance, ChildExecutableResponse response) {
    String childInstanceId = generateUuid();
    PlanExecution planExecution = ambianceHelper.obtainPlanExecution(ambiance);
    NodeExecution nodeExecution = ambianceHelper.obtainNodeExecution(ambiance);
    Plan plan = planExecution.getPlan();
    PlanNode node = plan.fetchNode(response.getChildNodeId());
    Ambiance clonedAmbiance = ambiance.cloneForChild();
    clonedAmbiance.addLevel(Level.fromPlanNode(childInstanceId, node));
    NodeExecution childNodeExecution = NodeExecution.builder()
                                           .uuid(childInstanceId)
                                           .node(node)
                                           .planExecutionId(nodeExecution.getPlanExecutionId())
                                           .levels(clonedAmbiance.getLevels())
                                           .status(Status.QUEUED)
                                           .notifyId(childInstanceId)
                                           .parentId(nodeExecution.getUuid())
                                           .additionalInputs(response.getAdditionalInputs())
                                           .build();
    hPersistence.save(childNodeExecution);
    executorService.submit(
        ExecutionEngineDispatcher.builder().ambiance(clonedAmbiance).executionEngine(engine).build());
    NotifyCallback callback = EngineResumeCallback.builder().nodeExecutionId(nodeExecution.getUuid()).build();
    waitNotifyEngine.waitForAllOn(ORCHESTRATION, callback, childInstanceId);
    nodeExecutionService.update(
        nodeExecution.getUuid(), ops -> ops.addToSet(NodeExecutionKeys.executableResponses, response));
  }
}
