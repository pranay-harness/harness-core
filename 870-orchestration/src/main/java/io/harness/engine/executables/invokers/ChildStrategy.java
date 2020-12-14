package io.harness.engine.executables.invokers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executables.ExecuteStrategy;
import io.harness.engine.executables.InvocationHelper;
import io.harness.engine.executables.InvokerPackage;
import io.harness.engine.executables.ResumePackage;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.execution.ChildExecutableResponse;
import io.harness.pms.execution.ExecutableResponse;
import io.harness.pms.execution.NodeExecutionProto;
import io.harness.pms.execution.Status;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.execution.utils.LevelUtils;
import io.harness.pms.plan.PlanNodeProto;
import io.harness.pms.sdk.core.execution.PmsNodeExecutionService;
import io.harness.pms.sdk.core.steps.executables.ChildExecutable;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.registries.StepRegistry;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"rawtypes", "unchecked"})
@OwnedBy(CDC)
@Redesign
public class ChildStrategy implements ExecuteStrategy {
  @Inject private PmsNodeExecutionService pmsNodeExecutionService;
  @Inject private StepRegistry stepRegistry;
  @Inject private InvocationHelper invocationHelper;

  @Override
  public void start(InvokerPackage invokerPackage) {
    NodeExecutionProto nodeExecution = invokerPackage.getNodeExecution();
    Ambiance ambiance = nodeExecution.getAmbiance();
    ChildExecutable childExecutable = extractChildExecutable(nodeExecution);
    ChildExecutableResponse response = childExecutable.obtainChild(ambiance,
        pmsNodeExecutionService.extractResolvedStepParameters(nodeExecution), invokerPackage.getInputPackage());
    handleResponse(nodeExecution, invokerPackage.getNodes(), response);
  }

  @Override
  public void resume(ResumePackage resumePackage) {
    NodeExecutionProto nodeExecution = resumePackage.getNodeExecution();
    Ambiance ambiance = nodeExecution.getAmbiance();
    ChildExecutable childExecutable = extractChildExecutable(nodeExecution);
    Map<String, ResponseData> accumulateResponses = invocationHelper.accumulateResponses(
        ambiance.getPlanExecutionId(), resumePackage.getResponseDataMap().keySet().iterator().next());
    StepResponse stepResponse = childExecutable.handleChildResponse(
        ambiance, pmsNodeExecutionService.extractResolvedStepParameters(nodeExecution), accumulateResponses);
    pmsNodeExecutionService.handleStepResponse(nodeExecution.getUuid(), stepResponse);
  }

  private ChildExecutable extractChildExecutable(NodeExecutionProto nodeExecution) {
    PlanNodeProto node = nodeExecution.getNode();
    return (ChildExecutable) stepRegistry.obtain(node.getStepType());
  }

  private void handleResponse(
      NodeExecutionProto nodeExecution, List<PlanNodeProto> nodes, ChildExecutableResponse response) {
    Ambiance ambiance = nodeExecution.getAmbiance();
    String childInstanceId = generateUuid();
    PlanNodeProto node = findNode(nodes, response.getChildNodeId());
    Ambiance clonedAmbiance =
        AmbianceUtils.cloneForChild(ambiance, LevelUtils.buildLevelFromPlanNode(childInstanceId, node));
    NodeExecutionProto childNodeExecution = NodeExecutionProto.newBuilder()
                                                .setUuid(childInstanceId)
                                                .setNode(node)
                                                .setAmbiance(clonedAmbiance)
                                                .setStatus(Status.QUEUED)
                                                .setNotifyId(childInstanceId)
                                                .setParentId(nodeExecution.getUuid())
                                                .build();
    pmsNodeExecutionService.queueNodeExecution(childNodeExecution);
    pmsNodeExecutionService.addExecutableResponse(nodeExecution.getUuid(), Status.UNRECOGNIZED,
        ExecutableResponse.newBuilder().setChild(response).build(), Collections.singletonList(childInstanceId));
  }
}
