package io.harness.cdng.executionplan.utils;

import io.harness.cdng.pipeline.CDStage;
import io.harness.cdng.pipeline.DeploymentStage;
import io.harness.executionplan.core.ExecutionPlanCreationContext;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.yaml.core.ParallelStageElement;
import io.harness.yaml.core.StageElement;
import io.harness.yaml.core.auxiliary.intfc.StageElementWrapper;

import java.util.Optional;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PlanCreatorConfigUtils {
  public static final String CD_PIPELINE_CONFIG = "CD_PIPELINE_CONFIG";
  public static final String CD_CURRENT_STAGE_CONFIG = "CD_CURRENT_STAGE_CONFIG";

  public static void setPipelineConfig(NgPipeline pipeline, ExecutionPlanCreationContext context) {
    setConfig(CD_PIPELINE_CONFIG, pipeline, context);
  }

  public static Optional<NgPipeline> getPipelineConfig(ExecutionPlanCreationContext context) {
    return getConfig(CD_PIPELINE_CONFIG, context);
  }

  public static void setCurrentStageConfig(CDStage stage, ExecutionPlanCreationContext context) {
    setConfig(CD_CURRENT_STAGE_CONFIG, stage, context);
  }

  public static Optional<CDStage> getCurrentStageConfig(ExecutionPlanCreationContext context) {
    return getConfig(CD_CURRENT_STAGE_CONFIG, context);
  }

  public <T> void setConfig(String key, T config, ExecutionPlanCreationContext context) {
    if (config == null) {
      context.removeAttribute(key);
    } else {
      context.addAttribute(key, config);
    }
  }

  public <T> Optional<T> getConfig(String key, ExecutionPlanCreationContext context) {
    return context.getAttribute(key);
  }

  public CDStage getGivenDeploymentStageFromPipeline(ExecutionPlanCreationContext context, String stageIdentifier) {
    Optional<NgPipeline> pipelineConfig = getPipelineConfig(context);
    if (pipelineConfig.isPresent()) {
      NgPipeline pipeline = pipelineConfig.get();
      for (StageElementWrapper stage : pipeline.getStages()) {
        if (stage instanceof StageElement) {
          DeploymentStage deploymentStage = (DeploymentStage) ((StageElement) stage).getStageType();
          if (deploymentStage.getIdentifier().equals(stageIdentifier)) {
            return deploymentStage;
          }
        } else if (stage instanceof ParallelStageElement) {
          ParallelStageElement parallelStage = (ParallelStageElement) stage;
          for (StageElementWrapper stageElement : parallelStage.getSections()) {
            DeploymentStage deploymentStage = (DeploymentStage) ((StageElement) stageElement).getStageType();
            if (deploymentStage.getIdentifier().equals(stageIdentifier)) {
              return deploymentStage;
            }
          }
        }
      }
      return null;
    } else {
      throw new IllegalArgumentException("Pipeline config doesn't exist.");
    }
  }
}
