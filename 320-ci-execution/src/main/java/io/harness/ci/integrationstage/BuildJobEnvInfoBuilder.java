package io.harness.ci.integrationstage;

import static io.harness.common.CIExecutionConstants.STEP_WORK_DIR;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.environment.AwsVmBuildJobInfo;
import io.harness.beans.environment.BuildJobEnvInfo;
import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.beans.stages.IntegrationStageConfig;
import io.harness.beans.steps.stepinfo.InitializeStepInfo;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure.Type;
import io.harness.beans.yaml.extended.infrastrucutre.K8sDirectInfraYaml;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.plancreator.execution.ExecutionWrapperConfig;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.timeout.Timeout;

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
    } // TODO (shubham): Handle Use from stage for AWS VM
    else if (infrastructure.getType() == Type.AWS_VM) {
      return AwsVmBuildJobInfo.builder().workDir(STEP_WORK_DIR).build();
    } else {
      throw new IllegalArgumentException("Input infrastructure type is not of type kubernetes");
    }
  }

  public int getTimeout(Infrastructure infrastructure) {
    if (infrastructure == null) {
      throw new CIStageExecutionException("Input infrastructure can not be empty");
    }

    if (infrastructure.getType() == Type.KUBERNETES_DIRECT) {
      return getK8Timeout((K8sDirectInfraYaml) infrastructure);
    }

    return InitializeStepInfo.DEFAULT_TIMEOUT;
  }

  private int getK8Timeout(K8sDirectInfraYaml k8sDirectInfraYaml) {
    if (k8sDirectInfraYaml.getSpec() == null) {
      throw new CIStageExecutionException("Input infrastructure can not be empty");
    }

    ParameterField<String> timeout = k8sDirectInfraYaml.getSpec().getInitTimeout();

    int timeoutInMillis = InitializeStepInfo.DEFAULT_TIMEOUT;
    if (timeout != null && timeout.fetchFinalValue() != null && isNotEmpty((String) timeout.fetchFinalValue())) {
      timeoutInMillis = (int) Timeout.fromString((String) timeout.fetchFinalValue()).getTimeoutInMillis();
    }
    return timeoutInMillis;
  }
}