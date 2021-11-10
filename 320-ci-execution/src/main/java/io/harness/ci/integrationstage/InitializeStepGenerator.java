package io.harness.ci.integrationstage;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.environment.BuildJobEnvInfo;
import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.beans.serializer.RunTimeInputHandler;
import io.harness.beans.stages.IntegrationStageConfig;
import io.harness.beans.steps.stepinfo.InitializeStepInfo;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.plancreator.execution.ExecutionElementConfig;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.yaml.extended.ci.codebase.CodeBase;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@OwnedBy(HarnessTeam.CI)
public class InitializeStepGenerator {
  private static final String INITIALIZE_TASK = InitializeStepInfo.STEP_TYPE.getType();
  @Inject private BuildJobEnvInfoBuilder buildJobEnvInfoBuilder;

  InitializeStepInfo createInitializeStepInfo(ExecutionElementConfig executionElement, CodeBase ciCodebase,
      StageElementConfig stageElementConfig, CIExecutionArgs ciExecutionArgs, Infrastructure infrastructure) {
    BuildJobEnvInfo buildJobEnvInfo =
        buildJobEnvInfoBuilder.getCIBuildJobEnvInfo(stageElementConfig, ciExecutionArgs, executionElement.getSteps());

    IntegrationStageConfig integrationStageConfig = IntegrationStageUtils.getIntegrationStageConfig(stageElementConfig);

    boolean gitClone = RunTimeInputHandler.resolveGitClone(integrationStageConfig.getCloneCodebase());

    return InitializeStepInfo.builder()
        .identifier(INITIALIZE_TASK)
        .name(INITIALIZE_TASK)
        .infrastructure(infrastructure)
        .ciCodebase(ciCodebase)
        .skipGitClone(!gitClone)
        .buildJobEnvInfo(buildJobEnvInfo)
        .executionElementConfig(executionElement)
        .timeout(buildJobEnvInfoBuilder.getTimeout(infrastructure))
        .build();
  }
}
