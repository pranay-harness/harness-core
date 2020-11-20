package io.harness.ngpipeline.pipeline.executions.registries;

import com.google.inject.Singleton;

import io.harness.ngpipeline.pipeline.StageTypeToStageExecutionSummaryMapper;
import io.harness.pipeline.executions.NGStageType;
import io.harness.registries.Registry;
import io.harness.registries.exceptions.DuplicateRegistryException;
import io.harness.registries.exceptions.UnregisteredKeyAccessException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class StageTypeToStageExecutionMapperHelperRegistry
    implements Registry<NGStageType, StageTypeToStageExecutionSummaryMapper<?>> {
  private final Map<NGStageType, StageTypeToStageExecutionSummaryMapper<?>> registry = new ConcurrentHashMap<>();

  @Override
  public void register(NGStageType registryKey, StageTypeToStageExecutionSummaryMapper<?> registrableEntity) {
    if (registry.containsKey(registryKey)) {
      throw new DuplicateRegistryException(
          getType(), "StageTypeToStageExecutionSummaryWrapper Already Registered with this type: " + registrableEntity);
    }
    registry.put(registryKey, registrableEntity);
  }

  @Override
  public StageTypeToStageExecutionSummaryMapper<?> obtain(NGStageType s) {
    if (registry.containsKey(s)) {
      return registry.get(s);
    }
    throw new UnregisteredKeyAccessException(
        getType(), "No StageTypeToStageExecutionMapperHelper registered for type: " + s);
  }

  @Override
  public String getType() {
    return "STAGE_EXECUTION_MAPPER";
  }
}
