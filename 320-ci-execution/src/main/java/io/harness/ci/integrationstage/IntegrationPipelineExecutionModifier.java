package io.harness.ci.integrationstage;

import io.harness.beans.stages.IntegrationStageConfig;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.beans.yaml.extended.infrastrucutre.UseFromStageInfraYaml;
import io.harness.exception.InvalidArgumentsException;
import io.harness.executionplan.core.ExecutionPlanCreationContext;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.yaml.core.ParallelStageElement;
import io.harness.yaml.core.StageElement;
import io.harness.yaml.core.auxiliary.intfc.StageElementWrapper;
import io.harness.yaml.core.intfc.StageType;

import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class IntegrationPipelineExecutionModifier implements PipelineExecutionModifier {
  @Override
  public NgPipeline modifyExecutionPlan(NgPipeline ngPipeline, ExecutionPlanCreationContext context) {
    List<StageElementWrapper> stages = ngPipeline.getStages();
    List<StageElementWrapper> resolvedStages = resolveInfrastructureReferences(stages);
    ngPipeline.setStages(resolvedStages);
    return ngPipeline;
  }

  // resolves stage infrastructure references
  // references can't be back references
  // parallel section cant references infrastructure from peer stages
  // references and only reference real infrastructure, no transitivity is allowed
  private List<StageElementWrapper> resolveInfrastructureReferences(List<StageElementWrapper> stageElementWrapperList) {
    Map<String, Infrastructure> infraCache = new HashMap<>();

    for (StageElementWrapper stageElement : stageElementWrapperList) {
      // Single stage
      if (stageElement instanceof StageElement) {
        StageType stageType = ((StageElement) stageElement).getStageType();
        if (stageType instanceof StageElementConfig) {
          StageElementConfig integrationStage = (StageElementConfig) stageType;
          infraCache.putAll(resolveCIStage(infraCache, integrationStage));
        }
      }
      // Parallel stages
      if (stageElement instanceof ParallelStageElement) {
        List<StageElementWrapper> stageElementWrappers = ((ParallelStageElement) stageElement).getSections();
        // parallel stages
        Map<String, Infrastructure> parallelSectionInfraMap = new HashMap<>();

        // resolve use from stage using cache
        for (StageElementWrapper stageElementWrapper : stageElementWrappers) {
          if (!(stageElementWrapper instanceof StageElement)) {
            throw new InvalidArgumentsException("Only StageElement is allowed in parallel stage section");
          }
          StageType stageType = ((StageElement) stageElementWrapper).getStageType();
          if (stageType instanceof StageElementConfig) {
            StageElementConfig integrationStage = (StageElementConfig) stageType;
            parallelSectionInfraMap.putAll(resolveCIStage(infraCache, integrationStage));
          }
        }
        // merge cache
        infraCache.putAll(parallelSectionInfraMap);
      }
    }
    return stageElementWrapperList;
  }

  private Map<String, Infrastructure> resolveCIStage(
      Map<String, Infrastructure> infrastructureCache, StageElementConfig stageElementConfig) {
    IntegrationStageConfig integrationStage = IntegrationStageUtils.getIntegrationStageConfig(stageElementConfig);
    Map<String, Infrastructure> cacheUpdate = new HashMap<>();
    Infrastructure infrastructure = integrationStage.getInfrastructure();

    if (infrastructure.getType() == Infrastructure.Type.USE_FROM_STAGE) {
      Infrastructure actualInfrastructure =
          infrastructureCache.get(((UseFromStageInfraYaml) infrastructure).getUseFromStage().getStage());
      if (actualInfrastructure == null) {
        throw new InvalidArgumentsException("Stage identifier given in useFromStage doesn't exist.");
      } else {
        integrationStage.setInfrastructure(actualInfrastructure);
      }
    } else {
      cacheUpdate.put(stageElementConfig.getIdentifier(), integrationStage.getInfrastructure());
    }
    return cacheUpdate;
  }
}
