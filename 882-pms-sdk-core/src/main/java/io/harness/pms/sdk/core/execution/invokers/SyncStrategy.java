package io.harness.pms.sdk.core.execution.invokers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.SyncExecutableResponse;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.execution.ExecuteStrategy;
import io.harness.pms.sdk.core.execution.InvokerPackage;
import io.harness.pms.sdk.core.execution.SdkNodeExecutionService;
import io.harness.pms.sdk.core.registries.StepRegistry;
import io.harness.pms.sdk.core.steps.executables.SyncExecutable;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponseMapper;

import com.google.inject.Inject;
import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings({"rawtypes", "unchecked"})
@OwnedBy(PIPELINE)
@Slf4j
public class SyncStrategy implements ExecuteStrategy {
  @Inject private StepRegistry stepRegistry;
  @Inject private SdkNodeExecutionService sdkNodeExecutionService;

  @Override
  public void start(InvokerPackage invokerPackage) {
    NodeExecutionProto nodeExecution = invokerPackage.getNodeExecution();
    Ambiance ambiance = nodeExecution.getAmbiance();
    SyncExecutable syncExecutable = extractStep(nodeExecution);
    StepResponse stepResponse =
        syncExecutable.executeSync(ambiance, sdkNodeExecutionService.extractResolvedStepParameters(nodeExecution),
            invokerPackage.getInputPackage(), invokerPackage.getPassThroughData());
    sdkNodeExecutionService.addExecutableResponse(nodeExecution.getUuid(), Status.NO_OP,
        ExecutableResponse.newBuilder()
            .setSync(SyncExecutableResponse.newBuilder()
                         .addAllLogKeys(syncExecutable.getLogKeys(nodeExecution.getAmbiance()))
                         .addAllUnits(syncExecutable.getCommandUnits(nodeExecution.getAmbiance()))
                         .build())
            .build(),
        new ArrayList<>());
    sdkNodeExecutionService.handleStepResponse(
        AmbianceUtils.obtainCurrentRuntimeId(ambiance), StepResponseMapper.toStepResponseProto(stepResponse));
  }

  @Override
  public SyncExecutable extractStep(NodeExecutionProto nodeExecution) {
    PlanNodeProto node = nodeExecution.getNode();
    return (SyncExecutable) stepRegistry.obtain(node.getStepType());
  }
}
