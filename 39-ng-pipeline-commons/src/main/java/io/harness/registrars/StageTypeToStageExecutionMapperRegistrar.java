package io.harness.registrars;

import io.harness.cdng.pipeline.StageTypeToStageExecutionSummaryMapper;
import io.harness.pipeline.executions.NGStageType;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

public interface StageTypeToStageExecutionMapperRegistrar {
  void register(Set<Pair<NGStageType, Class<? extends StageTypeToStageExecutionSummaryMapper>>> fieldClasses);
}
