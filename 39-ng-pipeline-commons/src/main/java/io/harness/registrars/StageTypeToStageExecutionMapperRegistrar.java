package io.harness.registrars;

import com.google.inject.Injector;

import io.harness.ngpipeline.pipeline.StageTypeToStageExecutionSummaryMapper;
import io.harness.pipeline.executions.NGStageType;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

public interface StageTypeToStageExecutionMapperRegistrar {
  void register(Injector injector, Set<Pair<NGStageType, StageTypeToStageExecutionSummaryMapper<?>>> fieldClasses);
}
