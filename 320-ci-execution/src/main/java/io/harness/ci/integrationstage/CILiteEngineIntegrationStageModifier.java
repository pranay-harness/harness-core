package io.harness.ci.integrationstage;

import io.harness.beans.execution.ManualExecutionSource;
import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.ci.beans.entities.BuildNumberDetails;
import io.harness.plancreator.execution.ExecutionElementConfig;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
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
      StageElementConfig stageElementConfig, PlanCreationContext context, String podName) {
    log.info("Modifying execution plan to add lite entine step for integration stage {}",
        stageElementConfig.getIdentifier());
    //    CIExecutionArgs ciExecutionArgs =
    //        (CIExecutionArgs) context.getAttribute(ExecutionArgs.EXEC_ARGS)
    //            .orElseThrow(()
    //                             -> new InvalidRequestException(
    //                                 "Execution arguments are empty for pipeline execution " +
    //                                 context.getAccountId()));

    //    NgPipeline pipeline =
    //        (NgPipeline) context.getAttribute(CI_PIPELINE_CONFIG)
    //            .orElseThrow(()
    //                             -> new InvalidRequestException(
    //                                 "Pipeline config is empty for pipeline execution " + context.getAccountId()));

    CIExecutionArgs ciExecutionArgs = CIExecutionArgs.builder()
                                          .executionSource(ManualExecutionSource.builder().branch("master").build())
                                          .buildNumberDetails(BuildNumberDetails.builder().buildNumber(10l).build())
                                          .build();
    return getCILiteEngineTaskExecution(stageElementConfig, ciExecutionArgs, null, podName, execution.getUuid());
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
