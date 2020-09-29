package io.harness.registrars;

import com.google.inject.Singleton;

import io.harness.cdng.pipeline.DeploymentStage;
import io.harness.cdng.pipeline.DeploymentStageTypeToStageExecutionSummaryMapper;
import io.harness.cdng.pipeline.StageTypeToStageExecutionSummaryMapper;
import io.harness.pipeline.executions.NGStageType;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

@Singleton
public class NGStageTypeToStageExecutionSummaryMapperRegistrar implements StageTypeToStageExecutionMapperRegistrar {
  @Override
  public void register(Set<Pair<NGStageType, Class<? extends StageTypeToStageExecutionSummaryMapper>>> fieldClasses) {
    fieldClasses.add(
        Pair.of(DeploymentStage.DEPLOYMENT_STAGE_TYPE, DeploymentStageTypeToStageExecutionSummaryMapper.class));
  }
}
