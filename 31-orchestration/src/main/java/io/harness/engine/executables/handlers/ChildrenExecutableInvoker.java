package io.harness.engine.executables.handlers;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.state.execution.status.NodeExecutionStatus.CHILDREN_WAITING;
import static io.harness.waiter.OrchestrationNotifyEventListener.ORCHESTRATION;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.annotations.Redesign;
import io.harness.engine.AmbianceHelper;
import io.harness.engine.EngineStatusHelper;
import io.harness.engine.ExecutionEngine;
import io.harness.engine.ExecutionEngineDispatcher;
import io.harness.engine.executables.ExecutableInvoker;
import io.harness.engine.executables.InvokerPackage;
import io.harness.engine.resume.EngineResumeCallback;
import io.harness.facilitate.modes.children.ChildrenExecutable;
import io.harness.facilitate.modes.children.ChildrenExecutableResponse;
import io.harness.facilitate.modes.children.ChildrenExecutableResponse.Child;
import io.harness.persistence.HPersistence;
import io.harness.plan.ExecutionNode;
import io.harness.plan.ExecutionPlan;
import io.harness.state.execution.ExecutionInstance;
import io.harness.state.execution.ExecutionNodeInstance;
import io.harness.state.execution.status.NodeExecutionStatus;
import io.harness.state.io.ambiance.Ambiance;
import io.harness.state.io.ambiance.Ambiance.AmbianceBuilder;
import io.harness.state.io.ambiance.Level;
import io.harness.waiter.NotifyCallback;
import io.harness.waiter.WaitNotifyEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Redesign
public class ChildrenExecutableInvoker implements ExecutableInvoker {
  @Inject private HPersistence hPersistence;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private ExecutionEngine engine;
  @Inject private EngineStatusHelper engineStatusHelper;
  @Inject private AmbianceHelper ambianceHelper;
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;

  @Override
  public void invokeExecutable(InvokerPackage invokerPackage) {
    Ambiance ambiance = invokerPackage.getAmbiance();
    ChildrenExecutable childrenExecutable = (ChildrenExecutable) invokerPackage.getState();
    ChildrenExecutableResponse response =
        childrenExecutable.obtainChildren(ambiance, invokerPackage.getParameters(), invokerPackage.getInputs());
    handleResponse(ambiance, response);
  }

  private void handleResponse(Ambiance ambiance, ChildrenExecutableResponse response) {
    ExecutionInstance executionInstance = ambianceHelper.obtainExecutionInstance(ambiance);
    ExecutionNodeInstance nodeInstance = ambianceHelper.obtainNodeInstance(ambiance);
    ExecutionPlan plan = executionInstance.getExecutionPlan();
    List<String> callbackIds = new ArrayList<>();
    for (Child child : response.getChildren()) {
      String uuid = generateUuid();
      callbackIds.add(uuid);
      AmbianceBuilder clonedBuilder = ambiance.cloneBuilder();
      ExecutionNode node = plan.fetchNode(child.getChildNodeId());
      Ambiance clonedAmbiance =
          clonedBuilder.level("currentNode", Level.builder().setupId(node.getUuid()).runtimeId(uuid).build()).build();
      ExecutionNodeInstance childInstance = ExecutionNodeInstance.builder()
                                                .uuid(uuid)
                                                .node(node)
                                                .ambiance(ambiance)
                                                .status(NodeExecutionStatus.QUEUED)
                                                .notifyId(uuid)
                                                .additionalInputs(child.getAdditionalInputs())
                                                .build();
      hPersistence.save(childInstance);
      executorService.submit(
          ExecutionEngineDispatcher.builder().ambiance(clonedAmbiance).executionEngine(engine).build());
    }
    NotifyCallback callback = EngineResumeCallback.builder().nodeInstanceId(nodeInstance.getUuid()).build();
    waitNotifyEngine.waitForAllOn(ORCHESTRATION, callback, callbackIds.toArray(new String[0]));
    engineStatusHelper.updateNodeInstance(nodeInstance.getUuid(), CHILDREN_WAITING, operations -> {});
  }
}
