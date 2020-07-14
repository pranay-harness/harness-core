package io.harness.cdng.executionplan.utils;

import io.harness.cdng.pipeline.CDPipeline;
import io.harness.cdng.pipeline.CDStage;
import io.harness.cdng.pipeline.DeploymentStage;
import io.harness.executionplan.core.CreateExecutionPlanContext;
import io.harness.yaml.core.StageElement;
import lombok.experimental.UtilityClass;

import java.util.Optional;

@UtilityClass
public class PlanCreatorConfigUtils {
  public static final String CD_PIPELINE_CONFIG = "CD_PIPELINE_CONFIG";
  public static final String CD_CURRENT_STAGE_CONFIG = "CD_CURRENT_STAGE_CONFIG";

  public static void setPipelineConfig(CDPipeline pipeline, CreateExecutionPlanContext context) {
    setConfig(CD_PIPELINE_CONFIG, pipeline, context);
  }

  public static Optional<CDPipeline> getPipelineConfig(CreateExecutionPlanContext context) {
    return getConfig(CD_PIPELINE_CONFIG, context);
  }

  public static void setCurrentStageConfig(CDStage stage, CreateExecutionPlanContext context) {
    setConfig(CD_CURRENT_STAGE_CONFIG, stage, context);
  }

  public static Optional<CDStage> getCurrentStageConfig(CreateExecutionPlanContext context) {
    return getConfig(CD_CURRENT_STAGE_CONFIG, context);
  }

  private <T> void setConfig(String key, T config, CreateExecutionPlanContext context) {
    if (config == null) {
      context.removeAttribute(key);
    } else {
      context.addAttribute(key, config);
    }
  }

  private <T> Optional<T> getConfig(String key, CreateExecutionPlanContext context) {
    return context.getAttribute(key);
  }

  public CDStage getGivenDeploymentStageFromPipeline(CreateExecutionPlanContext context, String stageIdentifier) {
    Optional<CDPipeline> pipelineConfig = getPipelineConfig(context);
    if (pipelineConfig.isPresent()) {
      CDPipeline pipeline = pipelineConfig.get();
      return pipeline.getStages()
          .stream()
          .map(stage -> (DeploymentStage) ((StageElement) stage).getStageType())
          .filter(deploymentStage -> deploymentStage.getIdentifier().equals(stageIdentifier))
          .findFirst()
          .orElse(null);
    } else {
      throw new IllegalArgumentException("Pipeline config doesn't exist.");
    }
  }
}
