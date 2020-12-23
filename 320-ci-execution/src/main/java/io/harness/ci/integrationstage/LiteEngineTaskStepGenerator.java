package io.harness.ci.integrationstage;

import io.harness.beans.environment.BuildJobEnvInfo;
import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.beans.stages.IntegrationStageConfig;
import io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo;
import io.harness.plancreator.execution.ExecutionElementConfig;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.yaml.extended.ci.codebase.CodeBase;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LiteEngineTaskStepGenerator {
  private static final String LITE_ENGINE_TASK = "liteEngineTask";
  private static final String SEPARATOR = "-";
  @Inject private BuildJobEnvInfoBuilder buildJobEnvInfoBuilder;

  LiteEngineTaskStepInfo createLiteEngineTaskStepInfo(ExecutionElementConfig executionElement, CodeBase ciCodebase,
      StageElementConfig stageElementConfig, CIExecutionArgs ciExecutionArgs, String podName, Integer liteEngineCounter,
      boolean usePVC) {
    boolean isFirstPod = isFirstPod(liteEngineCounter);
    String liteEnginePodName = podName;
    if (!isFirstPod) {
      liteEnginePodName = podName + SEPARATOR + liteEngineCounter;
    }

    BuildJobEnvInfo buildJobEnvInfo = buildJobEnvInfoBuilder.getCIBuildJobEnvInfo(
        stageElementConfig, ciExecutionArgs, executionElement.getSteps(), isFirstPod, liteEnginePodName);

    IntegrationStageConfig integrationStageConfig = IntegrationStageUtils.getIntegrationStageConfig(stageElementConfig);

    boolean gitClone = RunTimeInputHandler.resolveGitClone(integrationStageConfig.getCloneRepository());
    if (isFirstPod) {
      return LiteEngineTaskStepInfo.builder()
          .identifier(LITE_ENGINE_TASK + liteEngineCounter)
          .ciCodebase(ciCodebase)
          .skipGitClone(gitClone)
          .usePVC(usePVC)
          .buildJobEnvInfo(buildJobEnvInfo)
          .steps(null)
          .executionElementConfig(executionElement)
          .build();
    } else {
      return LiteEngineTaskStepInfo.builder()
          .identifier(LITE_ENGINE_TASK + liteEngineCounter)
          .buildJobEnvInfo(buildJobEnvInfo)
          .usePVC(usePVC)
          .steps(null)
          .executionElementConfig(executionElement)
          .build();
    }
  }
  private boolean isFirstPod(Integer liteEngineCounter) {
    return liteEngineCounter == 1;
  }
}
