package io.harness.engine.executables.invokers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.AmbianceUtils;
import io.harness.LevelUtils;
import io.harness.OrchestrationPublisherName;
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
import io.harness.plan.Plan;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.execution.ChildrenExecutableResponse;
import io.harness.pms.execution.ChildrenExecutableResponse.Child;
import io.harness.pms.execution.ExecutableResponse;
import io.harness.pms.execution.Status;
import io.harness.pms.plan.PlanNodeProto;
import io.harness.pms.sdk.core.steps.executables.ChildrenExecutable;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.registries.state.StepRegistry;
import io.harness.waiter.NotifyCallback;
import io.harness.waiter.WaitNotifyEngine;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
  @Inject private StepRegistry stepRegistry;
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;
  @Inject @Named(OrchestrationPublisherName.PUBLISHER_NAME) String publisherName;

  @Override
  public void start(InvokerPackage invokerPackage) {
    NodeExecution nodeExecution = invokerPackage.getNodeExecution();
    ChildrenExecutable childrenExecutable = extractChildrenExecutable(nodeExecution);
    Ambiance ambiance = nodeExecution.getAmbiance();
    ChildrenExecutableResponse response = childrenExecutable.obtainChildren(
        ambiance, nodeExecutionService.extractResolvedStepParameters(nodeExecution), invokerPackage.getInputPackage());
    handleResponse(ambiance, response);
  }

  @Override
  public void resume(ResumePackage resumePackage) {
    NodeExecution nodeExecution = resumePackage.getNodeExecution();
    Ambiance ambiance = nodeExecution.getAmbiance();
    ChildrenExecutable childrenExecutable = extractChildrenExecutable(nodeExecution);
    StepResponse stepResponse = childrenExecutable.handleChildrenResponse(ambiance,
        nodeExecutionService.extractResolvedStepParameters(nodeExecution), resumePackage.getResponseDataMap());
    engine.handleStepResponse(nodeExecution.getUuid(), stepResponse);
  }

  private ChildrenExecutable extractChildrenExecutable(NodeExecution nodeExecution) {
    PlanNodeProto node = nodeExecution.getNode();
    return (ChildrenExecutable) stepRegistry.obtain(node.getStepType());
  }

  private void handleResponse(Ambiance ambiance, ChildrenExecutableResponse response) {
    PlanExecution planExecution = planExecutionService.get(ambiance.getPlanExecutionId());
    NodeExecution nodeExecution = nodeExecutionService.get(AmbianceUtils.obtainCurrentRuntimeId(ambiance));
    Plan plan = planExecution.getPlan();
    List<String> callbackIds = new ArrayList<>();
    for (Child child : response.getChildrenList()) {
      String uuid = generateUuid();
      callbackIds.add(uuid);
      PlanNodeProto node = plan.fetchNode(child.getChildNodeId());
      Ambiance clonedAmbiance = AmbianceUtils.cloneForChild(ambiance, LevelUtils.buildLevelFromPlanNode(uuid, node));
      NodeExecution childNodeExecution = NodeExecution.builder()
                                             .uuid(uuid)
                                             .node(node)
                                             .ambiance(clonedAmbiance)
                                             .status(Status.QUEUED)
                                             .notifyId(uuid)
                                             .parentId(nodeExecution.getUuid())
                                             .progressDataMap(new LinkedHashMap<>())
                                             .build();
      nodeExecutionService.save(childNodeExecution);
      executorService.submit(
          ExecutionEngineDispatcher.builder().ambiance(clonedAmbiance).orchestrationEngine(engine).build());
    }
    NotifyCallback callback = EngineResumeCallback.builder().nodeExecutionId(nodeExecution.getUuid()).build();
    waitNotifyEngine.waitForAllOn(publisherName, callback, callbackIds.toArray(new String[0]));
    nodeExecutionService.update(nodeExecution.getUuid(),
        ops
        -> ops.addToSet(
            NodeExecutionKeys.executableResponses, ExecutableResponse.newBuilder().setChildren(response).build()));
  }
}
