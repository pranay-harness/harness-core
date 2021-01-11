package io.harness.pms.sdk.core.execution.invokers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ChildExecutableResponse;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.execution.utils.LevelUtils;
import io.harness.pms.sdk.core.execution.ExecuteStrategy;
import io.harness.pms.sdk.core.execution.InvokerPackage;
import io.harness.pms.sdk.core.execution.PmsNodeExecutionService;
import io.harness.pms.sdk.core.execution.ResumePackage;
import io.harness.pms.sdk.core.registries.StepRegistry;
import io.harness.pms.sdk.core.steps.executables.ChildExecutable;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponseMapper;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"rawtypes", "unchecked"})
@OwnedBy(CDC)
public class ChildStrategy implements ExecuteStrategy {
  @Inject private PmsNodeExecutionService pmsNodeExecutionService;
  @Inject private StepRegistry stepRegistry;

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
    Map<String, ResponseData> accumulateResponses = pmsNodeExecutionService.accumulateResponses(
        ambiance.getPlanExecutionId(), resumePackage.getResponseDataMap().keySet().iterator().next());
    StepResponse stepResponse = childExecutable.handleChildResponse(
        ambiance, pmsNodeExecutionService.extractResolvedStepParameters(nodeExecution), accumulateResponses);
    pmsNodeExecutionService.handleStepResponse(
        nodeExecution.getUuid(), StepResponseMapper.toStepResponseProto(stepResponse));
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
    pmsNodeExecutionService.addExecutableResponse(nodeExecution.getUuid(), Status.NO_OP,
        ExecutableResponse.newBuilder().setChild(response).build(), Collections.singletonList(childInstanceId));
  }
}
