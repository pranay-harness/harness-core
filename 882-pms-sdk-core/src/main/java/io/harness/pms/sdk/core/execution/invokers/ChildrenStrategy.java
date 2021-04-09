package io.harness.pms.sdk.core.execution.invokers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ChildrenExecutableResponse;
import io.harness.pms.contracts.execution.ChildrenExecutableResponse.Child;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.execution.utils.LevelUtils;
import io.harness.pms.sdk.core.execution.ExecuteStrategy;
import io.harness.pms.sdk.core.execution.InvokerPackage;
import io.harness.pms.sdk.core.execution.ResumePackage;
import io.harness.pms.sdk.core.execution.SdkNodeExecutionService;
import io.harness.pms.sdk.core.registries.StepRegistry;
import io.harness.pms.sdk.core.steps.executables.ChildrenExecutable;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponseMapper;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"rawtypes", "unchecked"})
@OwnedBy(CDC)
public class ChildrenStrategy implements ExecuteStrategy {
  @Inject private SdkNodeExecutionService sdkNodeExecutionService;
  @Inject private StepRegistry stepRegistry;

  @Override
  public void start(InvokerPackage invokerPackage) {
    NodeExecutionProto nodeExecution = invokerPackage.getNodeExecution();
    ChildrenExecutable childrenExecutable = extractChildrenExecutable(nodeExecution);
    Ambiance ambiance = nodeExecution.getAmbiance();
    ChildrenExecutableResponse response = childrenExecutable.obtainChildren(ambiance,
        sdkNodeExecutionService.extractResolvedStepParameters(nodeExecution), invokerPackage.getInputPackage());
    handleResponse(nodeExecution, invokerPackage.getNodes(), response);
  }

  @Override
  public void resume(ResumePackage resumePackage) {
    NodeExecutionProto nodeExecution = resumePackage.getNodeExecution();
    Ambiance ambiance = nodeExecution.getAmbiance();
    ChildrenExecutable childrenExecutable = extractChildrenExecutable(nodeExecution);
    StepResponse stepResponse = childrenExecutable.handleChildrenResponse(ambiance,
        sdkNodeExecutionService.extractResolvedStepParameters(nodeExecution), resumePackage.getResponseDataMap());
    sdkNodeExecutionService.handleStepResponse(
        nodeExecution.getUuid(), StepResponseMapper.toStepResponseProto(stepResponse));
  }

  private ChildrenExecutable extractChildrenExecutable(NodeExecutionProto nodeExecution) {
    PlanNodeProto node = nodeExecution.getNode();
    return (ChildrenExecutable) stepRegistry.obtain(node.getStepType());
  }

  private void handleResponse(
      NodeExecutionProto nodeExecution, List<PlanNodeProto> nodes, ChildrenExecutableResponse response) {
    Ambiance ambiance = nodeExecution.getAmbiance();
    List<String> callbackIds = new ArrayList<>();
    for (Child child : response.getChildrenList()) {
      String uuid = generateUuid();
      callbackIds.add(uuid);
      PlanNodeProto node = findNode(nodes, child.getChildNodeId());
      Ambiance clonedAmbiance = AmbianceUtils.cloneForChild(ambiance, LevelUtils.buildLevelFromPlanNode(uuid, node));
      NodeExecutionProto childNodeExecution = NodeExecutionProto.newBuilder()
                                                  .setUuid(uuid)
                                                  .setNode(node)
                                                  .setAmbiance(clonedAmbiance)
                                                  .setStatus(Status.QUEUED)
                                                  .setNotifyId(uuid)
                                                  .setParentId(nodeExecution.getUuid())
                                                  .build();
      sdkNodeExecutionService.queueNodeExecution(childNodeExecution);
    }

    sdkNodeExecutionService.addExecutableResponse(nodeExecution.getUuid(), Status.NO_OP,
        ExecutableResponse.newBuilder().setChildren(response).build(), callbackIds);
  }
}
