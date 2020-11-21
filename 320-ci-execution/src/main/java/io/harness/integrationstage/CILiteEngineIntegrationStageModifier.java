package io.harness.integrationstage;

import static io.harness.common.CIExecutionConstants.CI_PIPELINE_CONFIG;

import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.beans.executionargs.ExecutionArgs;
import io.harness.beans.stages.IntegrationStage;
import io.harness.exception.InvalidRequestException;
import io.harness.executionplan.core.ExecutionPlanCreationContext;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.yaml.core.ExecutionElement;
import io.harness.yaml.core.intfc.StageType;
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
  public ExecutionElement modifyExecutionPlan(
      ExecutionElement execution, StageType stageType, ExecutionPlanCreationContext context) {
    IntegrationStage integrationStage = (IntegrationStage) stageType;
    log.info(
        "Modifying execution plan to add lite entine step for integration stage {}", integrationStage.getIdentifier());
    CIExecutionArgs ciExecutionArgs =
        (CIExecutionArgs) context.getAttribute(ExecutionArgs.EXEC_ARGS)
            .orElseThrow(()
                             -> new InvalidRequestException(
                                 "Execution arguments are empty for pipeline execution " + context.getAccountId()));

    NgPipeline pipeline =
        (NgPipeline) context.getAttribute(CI_PIPELINE_CONFIG)
            .orElseThrow(()
                             -> new InvalidRequestException(
                                 "Pipeline config is empty for pipeline execution " + context.getAccountId()));

    return getCILiteEngineTaskExecution(
        integrationStage, ciExecutionArgs, pipeline.getCiCodebase(), context.getAccountId());
  }

  private ExecutionElement getCILiteEngineTaskExecution(
      IntegrationStage integrationStage, CIExecutionArgs ciExecutionArgs, CodeBase ciCodebase, String accountId) {
    return ExecutionElement.builder()
        .steps(ciLiteEngineStepGroupUtils.createExecutionWrapperWithLiteEngineSteps(
            integrationStage, ciExecutionArgs, ciCodebase, accountId))
        .build();
  }
}
