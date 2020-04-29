package io.harness.engine.executables.handlers;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.state.execution.status.NodeExecutionStatus.CHILD_WAITING;
import static io.harness.waiter.OrchestrationNotifyEventListener.ORCHESTRATION;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.ambiance.Ambiance;
import io.harness.ambiance.LevelExecution;
import io.harness.annotations.Redesign;
import io.harness.engine.AmbianceHelper;
import io.harness.engine.EngineStatusHelper;
import io.harness.engine.ExecutionEngine;
import io.harness.engine.ExecutionEngineDispatcher;
import io.harness.engine.executables.ExecutableInvoker;
import io.harness.engine.executables.InvokerPackage;
import io.harness.engine.resume.EngineResumeCallback;
import io.harness.facilitate.modes.child.ChildExecutable;
import io.harness.facilitate.modes.child.ChildExecutableResponse;
import io.harness.persistence.HPersistence;
import io.harness.plan.ExecutionNode;
import io.harness.plan.Plan;
import io.harness.registries.level.LevelRegistry;
import io.harness.state.execution.ExecutionNodeInstance;
import io.harness.state.execution.ExecutionNodeInstance.ExecutionNodeInstanceKeys;
import io.harness.state.execution.PlanExecution;
import io.harness.state.execution.status.NodeExecutionStatus;
import io.harness.waiter.NotifyCallback;
import io.harness.waiter.WaitNotifyEngine;

import java.util.concurrent.ExecutorService;

@Redesign
public class ChildExecutableInvoker implements ExecutableInvoker {
  @Inject private AmbianceHelper ambianceHelper;
  @Inject private LevelRegistry levelRegistry;
  @Inject private HPersistence hPersistence;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private EngineStatusHelper engineStatusHelper;
  @Inject private ExecutionEngine engine;
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;

  @Override
  public void invokeExecutable(InvokerPackage invokerPackage) {
    Ambiance ambiance = invokerPackage.getAmbiance();
    ChildExecutable childExecutable = (ChildExecutable) invokerPackage.getState();
    ChildExecutableResponse response =
        childExecutable.obtainChild(ambiance, invokerPackage.getParameters(), invokerPackage.getInputs());
    handleResponse(ambiance, response);
  }

  private void handleResponse(Ambiance ambiance, ChildExecutableResponse response) {
    String childInstanceId = generateUuid();
    PlanExecution planExecution = ambianceHelper.obtainExecutionInstance(ambiance);
    ExecutionNodeInstance nodeInstance = ambianceHelper.obtainNodeInstance(ambiance);
    Plan plan = planExecution.getPlan();
    ExecutionNode node = plan.fetchNode(response.getChildNodeId());
    Ambiance clonedAmbiance = ambiance.cloneForChild(levelRegistry.obtain(node.getLevelType()));
    clonedAmbiance.addLevelExecution(LevelExecution.builder()
                                         .setupId(node.getUuid())
                                         .runtimeId(childInstanceId)
                                         .level(levelRegistry.obtain(node.getLevelType()))
                                         .build());
    ExecutionNodeInstance childInstance = ExecutionNodeInstance.builder()
                                              .uuid(childInstanceId)
                                              .node(node)
                                              .ambiance(clonedAmbiance)
                                              .status(NodeExecutionStatus.QUEUED)
                                              .notifyId(childInstanceId)
                                              .parentId(nodeInstance.getUuid())
                                              .additionalInputs(response.getAdditionalInputs())
                                              .build();
    hPersistence.save(childInstance);
    executorService.submit(
        ExecutionEngineDispatcher.builder().ambiance(clonedAmbiance).executionEngine(engine).build());
    NotifyCallback callback = EngineResumeCallback.builder().nodeInstanceId(nodeInstance.getUuid()).build();
    waitNotifyEngine.waitForAllOn(ORCHESTRATION, callback, childInstanceId);
    engineStatusHelper.updateNodeInstance(
        nodeInstance.getUuid(), ops -> ops.set(ExecutionNodeInstanceKeys.status, CHILD_WAITING));
  }
}
