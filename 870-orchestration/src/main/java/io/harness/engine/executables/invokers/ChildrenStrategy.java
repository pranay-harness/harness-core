package io.harness.engine.executables.invokers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executables.ExecuteStrategy;
import io.harness.engine.executables.InvokerPackage;
import io.harness.engine.executables.ResumePackage;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ChildrenExecutableResponse;
import io.harness.pms.contracts.execution.ChildrenExecutableResponse.Child;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.execution.utils.LevelUtils;
import io.harness.pms.sdk.core.execution.PmsNodeExecutionService;
import io.harness.pms.sdk.core.steps.executables.ChildrenExecutable;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponseMapper;
import io.harness.pms.sdk.registries.StepRegistry;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"rawtypes", "unchecked"})
@OwnedBy(CDC)
@Redesign
public class ChildrenStrategy implements ExecuteStrategy {
  @Inject private PmsNodeExecutionService pmsNodeExecutionService;
  @Inject private StepRegistry stepRegistry;

  @Override
  public void start(InvokerPackage invokerPackage) {
    NodeExecutionProto nodeExecution = invokerPackage.getNodeExecution();
    ChildrenExecutable childrenExecutable = extractChildrenExecutable(nodeExecution);
    Ambiance ambiance = nodeExecution.getAmbiance();
    ChildrenExecutableResponse response = childrenExecutable.obtainChildren(ambiance,
        pmsNodeExecutionService.extractResolvedStepParameters(nodeExecution), invokerPackage.getInputPackage());
    handleResponse(nodeExecution, invokerPackage.getNodes(), response);
  }

  @Override
  public void resume(ResumePackage resumePackage) {
    NodeExecutionProto nodeExecution = resumePackage.getNodeExecution();
    Ambiance ambiance = nodeExecution.getAmbiance();
    ChildrenExecutable childrenExecutable = extractChildrenExecutable(nodeExecution);
    StepResponse stepResponse = childrenExecutable.handleChildrenResponse(ambiance,
        pmsNodeExecutionService.extractResolvedStepParameters(nodeExecution), resumePackage.getResponseDataMap());
    pmsNodeExecutionService.handleStepResponse(
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
      pmsNodeExecutionService.queueNodeExecution(childNodeExecution);
    }

    pmsNodeExecutionService.addExecutableResponse(nodeExecution.getUuid(), Status.UNRECOGNIZED,
        ExecutableResponse.newBuilder().setChildren(response).build(), callbackIds);
  }
}
