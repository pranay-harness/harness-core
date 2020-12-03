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
import io.harness.engine.executables.InvocationHelper;
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
import io.harness.pms.execution.ChildExecutableResponse;
import io.harness.pms.execution.ExecutableResponse;
import io.harness.pms.execution.Status;
import io.harness.pms.plan.PlanNodeProto;
import io.harness.pms.sdk.core.steps.executables.ChildExecutable;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.registries.state.StepRegistry;
import io.harness.tasks.ResponseData;
import io.harness.waiter.NotifyCallback;
import io.harness.waiter.WaitNotifyEngine;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

@SuppressWarnings({"rawtypes", "unchecked"})
@OwnedBy(CDC)
@Redesign
public class ChildStrategy implements ExecuteStrategy {
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private PlanExecutionService planExecutionService;
  @Inject private OrchestrationEngine engine;
  @Inject private StepRegistry stepRegistry;
  @Inject private InvocationHelper invocationHelper;
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;
  @Inject @Named(OrchestrationPublisherName.PUBLISHER_NAME) String publisherName;

  @Override
  public void start(InvokerPackage invokerPackage) {
    NodeExecution nodeExecution = invokerPackage.getNodeExecution();
    Ambiance ambiance = nodeExecution.getAmbiance();
    ChildExecutable childExecutable = extractChildExecutable(nodeExecution);
    ChildExecutableResponse response = childExecutable.obtainChild(
        ambiance, nodeExecutionService.extractResolvedStepParameters(nodeExecution), invokerPackage.getInputPackage());
    handleResponse(ambiance, response);
  }

  @Override
  public void resume(ResumePackage resumePackage) {
    NodeExecution nodeExecution = resumePackage.getNodeExecution();
    Ambiance ambiance = nodeExecution.getAmbiance();
    ChildExecutable childExecutable = extractChildExecutable(nodeExecution);
    Map<String, ResponseData> accumulateResponses = invocationHelper.accumulateResponses(
        ambiance.getPlanExecutionId(), resumePackage.getResponseDataMap().keySet().iterator().next());
    StepResponse stepResponse = childExecutable.handleChildResponse(
        ambiance, nodeExecutionService.extractResolvedStepParameters(nodeExecution), accumulateResponses);
    engine.handleStepResponse(nodeExecution.getUuid(), stepResponse);
  }

  private ChildExecutable extractChildExecutable(NodeExecution nodeExecution) {
    PlanNodeProto node = nodeExecution.getNode();
    return (ChildExecutable) stepRegistry.obtain(node.getStepType());
  }

  private void handleResponse(Ambiance ambiance, ChildExecutableResponse response) {
    String childInstanceId = generateUuid();
    PlanExecution planExecution = planExecutionService.get(ambiance.getPlanExecutionId());
    NodeExecution nodeExecution = nodeExecutionService.get(AmbianceUtils.obtainCurrentRuntimeId(ambiance));
    Plan plan = planExecution.getPlan();
    PlanNodeProto node = plan.fetchNode(response.getChildNodeId());
    Ambiance clonedAmbiance =
        AmbianceUtils.cloneForChild(ambiance, LevelUtils.buildLevelFromPlanNode(childInstanceId, node));
    NodeExecution childNodeExecution = NodeExecution.builder()
                                           .uuid(childInstanceId)
                                           .node(node)
                                           .ambiance(clonedAmbiance)
                                           .status(Status.QUEUED)
                                           .notifyId(childInstanceId)
                                           .parentId(nodeExecution.getUuid())
                                           .progressDataMap(new LinkedHashMap<>())
                                           .build();
    nodeExecutionService.save(childNodeExecution);
    executorService.submit(
        ExecutionEngineDispatcher.builder().ambiance(clonedAmbiance).orchestrationEngine(engine).build());
    NotifyCallback callback = EngineResumeCallback.builder().nodeExecutionId(nodeExecution.getUuid()).build();
    waitNotifyEngine.waitForAllOn(publisherName, callback, childInstanceId);
    nodeExecutionService.update(nodeExecution.getUuid(),
        ops
        -> ops.addToSet(
            NodeExecutionKeys.executableResponses, ExecutableResponse.newBuilder().setChild(response).build()));
  }
}
