package io.harness.engine.executables.handlers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.waiter.OrchestrationNotifyEventListener.ORCHESTRATION;

import com.google.common.base.Preconditions;
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
import io.harness.execution.status.NodeExecutionStatus;
import io.harness.facilitator.modes.children.ChildrenExecutable;
import io.harness.facilitator.modes.children.ChildrenExecutableResponse;
import io.harness.facilitator.modes.children.ChildrenExecutableResponse.Child;
import io.harness.persistence.HPersistence;
import io.harness.plan.ExecutionNode;
import io.harness.plan.Plan;
import io.harness.waiter.NotifyCallback;
import io.harness.waiter.WaitNotifyEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

@OwnedBy(CDC)
@Redesign
public class ChildrenExecutableInvoker implements ExecutableInvoker {
  @Inject private HPersistence hPersistence;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private ExecutionEngine engine;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private AmbianceHelper ambianceHelper;
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;

  @Override
  public void invokeExecutable(InvokerPackage invokerPackage) {
    Ambiance ambiance = invokerPackage.getAmbiance();
    ChildrenExecutable childrenExecutable = (ChildrenExecutable) invokerPackage.getStep();
    ChildrenExecutableResponse response =
        childrenExecutable.obtainChildren(ambiance, invokerPackage.getParameters(), invokerPackage.getInputs());
    handleResponse(ambiance, response);
  }

  private void handleResponse(Ambiance ambiance, ChildrenExecutableResponse response) {
    PlanExecution planExecution = Preconditions.checkNotNull(ambianceHelper.obtainExecutionInstance(ambiance));
    NodeExecution nodeExecution = Preconditions.checkNotNull(ambianceHelper.obtainNodeExecution(ambiance));
    Plan plan = planExecution.getPlan();
    List<String> callbackIds = new ArrayList<>();
    for (Child child : response.getChildren()) {
      String uuid = generateUuid();
      callbackIds.add(uuid);
      ExecutionNode node = plan.fetchNode(child.getChildNodeId());
      Ambiance clonedAmbiance = ambiance.cloneForChild();
      clonedAmbiance.addLevel(Level.builder()
                                  .setupId(node.getUuid())
                                  .runtimeId(uuid)
                                  .identifier(node.getIdentifier())
                                  .stepType(node.getStepType())
                                  .build());
      NodeExecution childNodeExecution = NodeExecution.builder()
                                             .uuid(uuid)
                                             .node(node)
                                             .planExecutionId(clonedAmbiance.getPlanExecutionId())
                                             .levels(clonedAmbiance.getLevels())
                                             .status(NodeExecutionStatus.QUEUED)
                                             .notifyId(uuid)
                                             .parentId(nodeExecution.getUuid())
                                             .build();
      hPersistence.save(childNodeExecution);
      executorService.submit(ExecutionEngineDispatcher.builder()
                                 .ambiance(clonedAmbiance)
                                 .executionEngine(engine)
                                 .additionalInputs(child.getAdditionalInputs())
                                 .build());
    }
    NotifyCallback callback = EngineResumeCallback.builder().nodeInstanceId(nodeExecution.getUuid()).build();
    waitNotifyEngine.waitForAllOn(ORCHESTRATION, callback, callbackIds.toArray(new String[0]));
    nodeExecutionService.update(
        nodeExecution.getUuid(), ops -> ops.set(NodeExecutionKeys.executableResponse, response));
  }
}
