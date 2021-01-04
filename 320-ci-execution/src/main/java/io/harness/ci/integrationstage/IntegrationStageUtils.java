package io.harness.ci.integrationstage;

import io.harness.beans.stages.IntegrationStageConfig;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.plancreator.execution.ExecutionWrapperConfig;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.plancreator.steps.ParallelStepElementConfig;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.yaml.extended.ci.codebase.CodeBase;

import java.io.IOException;
import lombok.experimental.UtilityClass;

@UtilityClass
public class IntegrationStageUtils {
  public IntegrationStageConfig getIntegrationStageConfig(StageElementConfig stageElementConfig) {
    if (stageElementConfig.getType().equals("CI")) {
      return (IntegrationStageConfig) stageElementConfig.getStageType();
    } else {
      throw new CIStageExecutionException("Invalid stage type: " + stageElementConfig.getStageType());
    }
  }

  public ParallelStepElementConfig getParallelStepElementConfig(ExecutionWrapperConfig executionWrapperConfig) {
    try {
      return YamlUtils.read(executionWrapperConfig.getParallel().toString(), ParallelStepElementConfig.class);
    } catch (Exception ex) {
      throw new CIStageExecutionException("Failed to deserialize ExecutionWrapperConfig parallel node", ex);
    }
  }

  public StepElementConfig getStepElementConfig(ExecutionWrapperConfig executionWrapperConfig) {
    try {
      return YamlUtils.read(executionWrapperConfig.getStep().toString(), StepElementConfig.class);
    } catch (Exception ex) {
      throw new CIStageExecutionException("Failed to deserialize ExecutionWrapperConfig step node", ex);
    }
  }
  public CodeBase getCiCodeBase(YamlNode ciCodeBase) {
    try {
      return YamlUtils.read(ciCodeBase.toString(), CodeBase.class);
    } catch (IOException e) {
      throw new InvalidRequestException("Invalid yaml", e);
    }
  }
}
