/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ci.integrationstage;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.environment.BuildJobEnvInfo;
import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.beans.serializer.RunTimeInputHandler;
import io.harness.beans.stages.IntegrationStageConfig;
import io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.beans.yaml.extended.infrastrucutre.K8sDirectInfraYaml;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.plancreator.execution.ExecutionElementConfig;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.timeout.Timeout;
import io.harness.yaml.extended.ci.codebase.CodeBase;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@OwnedBy(HarnessTeam.CI)
public class LiteEngineTaskStepGenerator {
  private static final String LITE_ENGINE_TASK = "liteEngineTask";
  private static final String SEPARATOR = "-";
  @Inject private BuildJobEnvInfoBuilder buildJobEnvInfoBuilder;

  LiteEngineTaskStepInfo createLiteEngineTaskStepInfo(ExecutionElementConfig executionElement, CodeBase ciCodebase,
      StageElementConfig stageElementConfig, CIExecutionArgs ciExecutionArgs, String podName, Integer liteEngineCounter,
      boolean usePVC, Infrastructure infrastructure) {
    boolean isFirstPod = isFirstPod(liteEngineCounter);
    String liteEnginePodName = podName;
    if (!isFirstPod) {
      liteEnginePodName = podName + SEPARATOR + liteEngineCounter;
    }

    BuildJobEnvInfo buildJobEnvInfo = buildJobEnvInfoBuilder.getCIBuildJobEnvInfo(
        stageElementConfig, ciExecutionArgs, executionElement.getSteps(), isFirstPod, liteEnginePodName);

    IntegrationStageConfig integrationStageConfig = IntegrationStageUtils.getIntegrationStageConfig(stageElementConfig);

    boolean gitClone = RunTimeInputHandler.resolveGitClone(integrationStageConfig.getCloneCodebase());
    if (isFirstPod) {
      return LiteEngineTaskStepInfo.builder()
          .identifier(LITE_ENGINE_TASK + liteEngineCounter)
          .name(LITE_ENGINE_TASK + liteEngineCounter)
          .infrastructure(infrastructure)
          .ciCodebase(ciCodebase)
          .skipGitClone(!gitClone)
          .usePVC(usePVC)
          .buildJobEnvInfo(buildJobEnvInfo)
          .executionElementConfig(executionElement)
          .timeout(getTimeout(infrastructure))
          .build();
    } else {
      return LiteEngineTaskStepInfo.builder()
          .identifier(LITE_ENGINE_TASK + liteEngineCounter)
          .name(LITE_ENGINE_TASK + liteEngineCounter)
          .buildJobEnvInfo(buildJobEnvInfo)
          .infrastructure(infrastructure)
          .usePVC(usePVC)
          .executionElementConfig(executionElement)
          .timeout(getTimeout(infrastructure))
          .build();
    }
  }

  private boolean isFirstPod(Integer liteEngineCounter) {
    return liteEngineCounter == 1;
  }

  private int getTimeout(Infrastructure infrastructure) {
    if (infrastructure == null || ((K8sDirectInfraYaml) infrastructure).getSpec() == null) {
      throw new CIStageExecutionException("Input infrastructure can not be empty");
    }

    ParameterField<String> timeout = ((K8sDirectInfraYaml) infrastructure).getSpec().getInitTimeout();

    int timeoutInMillis = LiteEngineTaskStepInfo.DEFAULT_TIMEOUT;
    if (timeout != null && timeout.fetchFinalValue() != null && isNotEmpty((String) timeout.fetchFinalValue())) {
      timeoutInMillis = (int) Timeout.fromString((String) timeout.fetchFinalValue()).getTimeoutInMillis();
    }
    return timeoutInMillis;
  }
}
