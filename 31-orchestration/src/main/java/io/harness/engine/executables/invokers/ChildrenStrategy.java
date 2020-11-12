package io.harness.engine.executables.invokers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.OrchestrationPublisherName;
import io.harness.ambiance.Ambiance;
import io.harness.ambiance.AmbianceUtils;
import io.harness.ambiance.Level;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.ExecutionEngineDispatcher;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executables.ExecuteStrategy;
import io.harness.engine.executables.InvokerPackage;
import io.harness.engine.executables.ResumePackage;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.engine.resume.EngineResumeCallback;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.PlanExecution;
import io.harness.facilitator.modes.children.ChildrenExecutable;
import io.harness.facilitator.modes.children.ChildrenExecutableResponse;
import io.harness.facilitator.modes.children.ChildrenExecutableResponse.Child;
import io.harness.plan.Plan;
import io.harness.plan.PlanNode;
import io.harness.pms.execution.Status;
import io.harness.registries.state.StepRegistry;
import io.harness.state.io.StepResponse;
import io.harness.waiter.NotifyCallback;
import io.harness.waiter.WaitNotifyEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

@SuppressWarnings({"rawtypes", "unchecked"})
@OwnedBy(CDC)
@Redesign
public class ChildrenStrategy implements ExecuteStrategy {
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private OrchestrationEngine engine;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private PlanExecutionService planExecutionService;
  @Inject private AmbianceUtils ambianceUtils;
  @Inject private StepRegistry stepRegistry;
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;
  @Inject @Named(OrchestrationPublisherName.PUBLISHER_NAME) String publisherName;

  @Override
  public void start(InvokerPackage invokerPackage) {
    NodeExecution nodeExecution = invokerPackage.getNodeExecution();
    ChildrenExecutable childrenExecutable = extractChildrenExecutable(nodeExecution);
    Ambiance ambiance = nodeExecution.getAmbiance();
    ChildrenExecutableResponse response = childrenExecutable.obtainChildren(
        ambiance, nodeExecution.getResolvedStepParameters(), invokerPackage.getInputPackage());
    handleResponse(ambiance, response);
  }

  @Override
  public void resume(ResumePackage resumePackage) {
    NodeExecution nodeExecution = resumePackage.getNodeExecution();
    Ambiance ambiance = nodeExecution.getAmbiance();
    ChildrenExecutable childrenExecutable = extractChildrenExecutable(nodeExecution);
    StepResponse stepResponse = childrenExecutable.handleChildrenResponse(
        ambiance, nodeExecution.getResolvedStepParameters(), resumePackage.getResponseDataMap());
    engine.handleStepResponse(nodeExecution.getUuid(), stepResponse);
  }

  private ChildrenExecutable extractChildrenExecutable(NodeExecution nodeExecution) {
    PlanNode node = nodeExecution.getNode();
    return (ChildrenExecutable) stepRegistry.obtain(node.getStepType());
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
      Ambiance clonedAmbiance = ambianceUtils.cloneForChild(ambiance);
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
          ExecutionEngineDispatcher.builder().ambiance(clonedAmbiance).orchestrationEngine(engine).build());
    }
    NotifyCallback callback = EngineResumeCallback.builder().nodeExecutionId(nodeExecution.getUuid()).build();
    waitNotifyEngine.waitForAllOn(publisherName, callback, callbackIds.toArray(new String[0]));
    nodeExecutionService.update(
        nodeExecution.getUuid(), ops -> ops.addToSet(NodeExecutionKeys.executableResponses, response));
  }
}
