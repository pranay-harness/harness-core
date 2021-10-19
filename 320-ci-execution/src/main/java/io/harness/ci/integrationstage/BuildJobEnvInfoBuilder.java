package io.harness.ci.integrationstage;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.environment.BuildJobEnvInfo;
import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.beans.stages.IntegrationStageConfig;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure.Type;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.plancreator.execution.ExecutionWrapperConfig;
import io.harness.plancreator.stages.stage.StageElementConfig;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.CI)
public class BuildJobEnvInfoBuilder {
  @Inject private InitializeStepInfoBuilder initializeStepInfoBuilder;

  public BuildJobEnvInfo getCIBuildJobEnvInfo(
      StageElementConfig stageElementConfig, CIExecutionArgs ciExecutionArgs, List<ExecutionWrapperConfig> steps) {
    // TODO Only kubernetes is supported currently
    IntegrationStageConfig integrationStage = (IntegrationStageConfig) stageElementConfig.getStageType();
    if (integrationStage.getInfrastructure() == null) {
      throw new CIStageExecutionException("Input infrastructure is not set");
    }

    Infrastructure infrastructure = integrationStage.getInfrastructure();
    if (infrastructure.getType() == Infrastructure.Type.KUBERNETES_DIRECT
        || infrastructure.getType() == Type.USE_FROM_STAGE) {
      return initializeStepInfoBuilder.getInitializeStepInfoBuilder(stageElementConfig, ciExecutionArgs, steps);
    } else {
      throw new IllegalArgumentException("Input infrastructure type is not of type kubernetes");
    }
  }
}