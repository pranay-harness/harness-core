package io.harness.registrars;

import io.harness.annotations.dev.ToBeDeleted;
import io.harness.ngpipeline.pipeline.StageTypeToStageExecutionSummaryMapper;
import io.harness.pipeline.executions.NGStageType;

import com.google.inject.Injector;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

@ToBeDeleted
@Deprecated
public interface StageTypeToStageExecutionMapperRegistrar {
  void register(Injector injector, Set<Pair<NGStageType, StageTypeToStageExecutionSummaryMapper<?>>> fieldClasses);
}
