package io.harness.integrationstage;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.environment.BuildJobEnvInfo;
import io.harness.beans.stages.IntegrationStage;
import io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo;
import io.harness.yaml.core.ExecutionElement;

@Singleton
public class LiteEngineTaskStepGenerator {
  private static final String LITE_ENGINE_TASK = "liteEngineTask";
  @Inject private BuildJobEnvInfoBuilder buildJobEnvInfoBuilder;

  LiteEngineTaskStepInfo createLiteEngineTaskStepInfo(ExecutionElement executionElement, String branchName,
      String gitConnectorIdentifier, IntegrationStage integrationStage, String buildNumber, Integer parallelism,
      Integer liteEngineCounter, boolean usePVC) {
    boolean isFirstPod = isFirstPod(liteEngineCounter);
    BuildJobEnvInfo buildJobEnvInfo =
        buildJobEnvInfoBuilder.getCIBuildJobEnvInfo(integrationStage, isFirstPod, buildNumber, parallelism);
    if (isFirstPod) {
      return LiteEngineTaskStepInfo.builder()
          .identifier(LITE_ENGINE_TASK + liteEngineCounter)
          .gitConnectorIdentifier(gitConnectorIdentifier)
          .branchName(branchName)
          .usePVC(usePVC)
          .buildJobEnvInfo(buildJobEnvInfo)
          .steps(executionElement)
          .build();
    } else {
      return LiteEngineTaskStepInfo.builder()
          .identifier(LITE_ENGINE_TASK + liteEngineCounter)
          .buildJobEnvInfo(buildJobEnvInfo)
          .usePVC(usePVC)
          .steps(executionElement)
          .build();
    }
  }
  private boolean isFirstPod(Integer liteEngineCounter) {
    return liteEngineCounter == 1;
  }
}
