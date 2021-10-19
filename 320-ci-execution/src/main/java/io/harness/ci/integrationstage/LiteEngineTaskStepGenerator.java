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
  @Inject private BuildJobEnvInfoBuilder buildJobEnvInfoBuilder;

  LiteEngineTaskStepInfo createLiteEngineTaskStepInfo(ExecutionElementConfig executionElement, CodeBase ciCodebase,
      StageElementConfig stageElementConfig, CIExecutionArgs ciExecutionArgs, Infrastructure infrastructure) {
    BuildJobEnvInfo buildJobEnvInfo =
        buildJobEnvInfoBuilder.getCIBuildJobEnvInfo(stageElementConfig, ciExecutionArgs, executionElement.getSteps());

    IntegrationStageConfig integrationStageConfig = IntegrationStageUtils.getIntegrationStageConfig(stageElementConfig);

    boolean gitClone = RunTimeInputHandler.resolveGitClone(integrationStageConfig.getCloneCodebase());

    return LiteEngineTaskStepInfo.builder()
        .identifier(LITE_ENGINE_TASK)
        .name(LITE_ENGINE_TASK)
        .infrastructure(infrastructure)
        .ciCodebase(ciCodebase)
        .skipGitClone(!gitClone)
        .buildJobEnvInfo(buildJobEnvInfo)
        .executionElementConfig(executionElement)
        .timeout(getTimeout(infrastructure))
        .build();
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
