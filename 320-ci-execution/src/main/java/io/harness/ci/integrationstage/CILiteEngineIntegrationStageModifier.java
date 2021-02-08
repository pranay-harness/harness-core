package io.harness.ci.integrationstage;

import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.ci.beans.entities.BuildNumberDetails;
import io.harness.plancreator.execution.ExecutionElementConfig;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.contracts.plan.PlanCreationContextValue;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.extended.ci.codebase.Build;
import io.harness.yaml.extended.ci.codebase.CodeBase;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * Modifies saved integration stage execution plan by appending pre and post execution steps for setting up pod and
 * adding cleanup
 */

@Slf4j
@Singleton
public class CILiteEngineIntegrationStageModifier implements StageExecutionModifier {
  @Inject private CILiteEngineStepGroupUtils ciLiteEngineStepGroupUtils;

  @Override
  public ExecutionElementConfig modifyExecutionPlan(ExecutionElementConfig execution,
      StageElementConfig stageElementConfig, PlanCreationContext context, String podName, CodeBase ciCodeBase) {
    log.info("Modifying execution plan to add lite engine step for integration stage {}",
        stageElementConfig.getIdentifier());

    PlanCreationContextValue planCreationContextValue = context.getGlobalContext().get("metadata");
    ExecutionMetadata executionMetadata = planCreationContextValue.getMetadata();

    ParameterField<Build> parameterFieldBuild = null;
    if (ciCodeBase != null) {
      parameterFieldBuild = ciCodeBase.getBuild();
    }

    CIExecutionArgs ciExecutionArgs =
        CIExecutionArgs.builder()
            .executionSource(IntegrationStageUtils.buildExecutionSource(
                executionMetadata, stageElementConfig.getIdentifier(), parameterFieldBuild))
            .buildNumberDetails(
                BuildNumberDetails.builder().buildNumber((long) executionMetadata.getRunSequence()).build())
            .build();

    log.info("Build execution args for integration stage  {}", stageElementConfig.getIdentifier());
    return getCILiteEngineTaskExecution(stageElementConfig, ciExecutionArgs, ciCodeBase, podName, execution.getUuid());
  }

  private ExecutionElementConfig getCILiteEngineTaskExecution(StageElementConfig integrationStage,
      CIExecutionArgs ciExecutionArgs, CodeBase ciCodebase, String podName, String uuid) {
    return ExecutionElementConfig.builder()
        .uuid(uuid)
        .steps(ciLiteEngineStepGroupUtils.createExecutionWrapperWithLiteEngineSteps(
            integrationStage, ciExecutionArgs, ciCodebase, podName))
        .build();
  }
}
