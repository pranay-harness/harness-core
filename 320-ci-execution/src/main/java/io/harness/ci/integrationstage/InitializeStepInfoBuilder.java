package io.harness.ci.integrationstage;

import io.harness.beans.environment.BuildJobEnvInfo;
import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.plancreator.execution.ExecutionWrapperConfig;
import io.harness.plancreator.stages.stage.StageElementConfig;

import java.util.List;

public interface InitializeStepInfoBuilder {
  BuildJobEnvInfo getInitializeStepInfoBuilder(
      StageElementConfig stageElementConfig, CIExecutionArgs ciExecutionArgs, List<ExecutionWrapperConfig> steps);
}
