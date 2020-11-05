package io.harness.integrationstage;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.environment.BuildJobEnvInfo;
import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.beans.stages.IntegrationStage;
import io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo;
import io.harness.yaml.core.ExecutionElement;
import io.harness.yaml.extended.ci.codebase.CodeBase;

@Singleton
public class LiteEngineTaskStepGenerator {
  private static final String LITE_ENGINE_TASK = "liteEngineTask";
  @Inject private BuildJobEnvInfoBuilder buildJobEnvInfoBuilder;

  LiteEngineTaskStepInfo createLiteEngineTaskStepInfo(ExecutionElement executionElement, CodeBase ciCodebase,
      IntegrationStage integrationStage, CIExecutionArgs ciExecutionArgs, String buildNumber, Integer liteEngineCounter,
      boolean usePVC, String accountId) {
    boolean isFirstPod = isFirstPod(liteEngineCounter);
    BuildJobEnvInfo buildJobEnvInfo = buildJobEnvInfoBuilder.getCIBuildJobEnvInfo(
        integrationStage, ciExecutionArgs, executionElement.getSteps(), isFirstPod, buildNumber);
    if (isFirstPod) {
      return LiteEngineTaskStepInfo.builder()
          .identifier(LITE_ENGINE_TASK + liteEngineCounter)
          .ciCodebase(ciCodebase)
          .skipGitClone(integrationStage.isSkipGitClone())
          .usePVC(usePVC)
          .buildJobEnvInfo(buildJobEnvInfo)
          .steps(executionElement)
          .accountId(accountId)
          .build();
    } else {
      return LiteEngineTaskStepInfo.builder()
          .identifier(LITE_ENGINE_TASK + liteEngineCounter)
          .buildJobEnvInfo(buildJobEnvInfo)
          .usePVC(usePVC)
          .steps(executionElement)
          .accountId(accountId)
          .build();
    }
  }
  private boolean isFirstPod(Integer liteEngineCounter) {
    return liteEngineCounter == 1;
  }
}
