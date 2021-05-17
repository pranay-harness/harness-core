package io.harness.pms.sdk.core.execution.invokers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ChildExecutableResponse;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.execution.events.SpawnChildRequest;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.sdk.core.execution.ExecuteStrategy;
import io.harness.pms.sdk.core.execution.InvokerPackage;
import io.harness.pms.sdk.core.execution.ResumePackage;
import io.harness.pms.sdk.core.execution.SdkNodeExecutionService;
import io.harness.pms.sdk.core.registries.StepRegistry;
import io.harness.pms.sdk.core.steps.executables.ChildExecutable;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponseMapper;

import com.google.inject.Inject;

@SuppressWarnings({"rawtypes", "unchecked"})
@OwnedBy(PIPELINE)
public class ChildStrategy implements ExecuteStrategy {
  @Inject private SdkNodeExecutionService sdkNodeExecutionService;
  @Inject private StepRegistry stepRegistry;

  @Override
  public void start(InvokerPackage invokerPackage) {
    NodeExecutionProto nodeExecution = invokerPackage.getNodeExecution();
    Ambiance ambiance = nodeExecution.getAmbiance();
    ChildExecutable childExecutable = extractStep(nodeExecution);
    ChildExecutableResponse response = childExecutable.obtainChild(ambiance,
        sdkNodeExecutionService.extractResolvedStepParameters(nodeExecution), invokerPackage.getInputPackage());
    handleResponse(nodeExecution, response);
  }

  @Override
  public void resume(ResumePackage resumePackage) {
    NodeExecutionProto nodeExecution = resumePackage.getNodeExecution();
    Ambiance ambiance = nodeExecution.getAmbiance();
    ChildExecutable childExecutable = extractStep(nodeExecution);
    StepResponse stepResponse = childExecutable.handleChildResponse(ambiance,
        sdkNodeExecutionService.extractResolvedStepParameters(nodeExecution), resumePackage.getResponseDataMap());
    sdkNodeExecutionService.handleStepResponse(
        nodeExecution.getUuid(), StepResponseMapper.toStepResponseProto(stepResponse));
  }

  @Override
  public ChildExecutable extractStep(NodeExecutionProto nodeExecution) {
    PlanNodeProto node = nodeExecution.getNode();
    return (ChildExecutable) stepRegistry.obtain(node.getStepType());
  }

  private void handleResponse(NodeExecutionProto nodeExecution, ChildExecutableResponse response) {
    Ambiance ambiance = nodeExecution.getAmbiance();

    SpawnChildRequest spawnChildRequest = SpawnChildRequest.newBuilder()
                                              .setPlanExecutionId(ambiance.getPlanExecutionId())
                                              .setNodeExecutionId(nodeExecution.getUuid())
                                              .setChild(response)
                                              .build();

    sdkNodeExecutionService.spawnChild(spawnChildRequest);
  }
}
