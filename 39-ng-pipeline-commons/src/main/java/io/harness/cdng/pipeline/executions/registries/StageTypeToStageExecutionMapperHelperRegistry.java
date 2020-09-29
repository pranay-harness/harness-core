package io.harness.cdng.pipeline.executions.registries;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import io.harness.cdng.pipeline.StageTypeToStageExecutionSummaryMapper;
import io.harness.pipeline.executions.NGStageType;
import io.harness.registries.Registry;
import io.harness.registries.exceptions.DuplicateRegistryException;
import io.harness.registries.exceptions.UnregisteredKeyAccessException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class StageTypeToStageExecutionMapperHelperRegistry
    implements Registry<NGStageType, Class<? extends StageTypeToStageExecutionSummaryMapper<?>>> {
  @Inject private Injector injector;

  private final Map<NGStageType, Class<? extends StageTypeToStageExecutionSummaryMapper<?>>> registry =
      new ConcurrentHashMap<>();

  @Override
  public void register(
      NGStageType registryKey, Class<? extends StageTypeToStageExecutionSummaryMapper<?>> registrableEntity) {
    if (registry.containsKey(registryKey)) {
      throw new DuplicateRegistryException(
          getType(), "StageTypeToStageExecutionSummaryWrapper Already Registered with this type: " + registrableEntity);
    }
    registry.put(registryKey, registrableEntity);
  }

  @Override
  public StageTypeToStageExecutionSummaryMapper<?> obtain(NGStageType s) {
    if (registry.containsKey(s)) {
      return injector.getInstance(registry.get(s));
    }
    throw new UnregisteredKeyAccessException(
        getType(), "No StageTypeToStageExecutionMapperHelper registered for type: " + s);
  }

  @Override
  public String getType() {
    return "STAGE_EXECUTION_MAPPER";
  }
}