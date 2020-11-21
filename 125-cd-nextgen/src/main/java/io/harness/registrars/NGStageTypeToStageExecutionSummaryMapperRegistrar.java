package io.harness.registrars;

import io.harness.cdng.pipeline.DeploymentStage;
import io.harness.cdng.pipeline.DeploymentStageTypeToStageExecutionSummaryMapper;
import io.harness.ngpipeline.pipeline.StageTypeToStageExecutionSummaryMapper;
import io.harness.pipeline.executions.NGStageType;

import com.google.inject.Injector;
import com.google.inject.Singleton;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

@Singleton
public class NGStageTypeToStageExecutionSummaryMapperRegistrar implements StageTypeToStageExecutionMapperRegistrar {
  @Override
  public void register(
      Injector injector, Set<Pair<NGStageType, StageTypeToStageExecutionSummaryMapper<?>>> fieldClasses) {
    fieldClasses.add(Pair.of(DeploymentStage.DEPLOYMENT_STAGE_TYPE,
        injector.getInstance(DeploymentStageTypeToStageExecutionSummaryMapper.class)));
  }
}
