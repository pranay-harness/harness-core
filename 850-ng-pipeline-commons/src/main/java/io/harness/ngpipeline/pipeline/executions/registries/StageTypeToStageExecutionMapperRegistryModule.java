package io.harness.ngpipeline.pipeline.executions.registries;

import io.harness.ngpipeline.pipeline.StageTypeToStageExecutionSummaryMapper;
import io.harness.pipeline.executions.NGStageType;
import io.harness.registrars.StageTypeToStageExecutionMapperRegistrar;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

@Singleton
public class StageTypeToStageExecutionMapperRegistryModule extends AbstractModule {
  private static StageTypeToStageExecutionMapperRegistryModule instance;

  public static synchronized StageTypeToStageExecutionMapperRegistryModule getInstance() {
    if (instance == null) {
      instance = new StageTypeToStageExecutionMapperRegistryModule();
    }
    return instance;
  }

  @Provides
  @Singleton
  StageTypeToStageExecutionMapperHelperRegistry providesStageTypeToStageExecutionSummaryMapperHelper(
      Injector injector, Map<String, StageTypeToStageExecutionMapperRegistrar> stageExecutionHelperRegistrarMap) {
    Set<Pair<NGStageType, StageTypeToStageExecutionSummaryMapper<?>>> classes = new HashSet<>();
    stageExecutionHelperRegistrarMap.values().forEach(
        stageExecutionHelperRegistrar -> stageExecutionHelperRegistrar.register(injector, classes));
    StageTypeToStageExecutionMapperHelperRegistry visitorFieldRegistry =
        new StageTypeToStageExecutionMapperHelperRegistry();
    injector.injectMembers(visitorFieldRegistry);
    classes.forEach(pair -> { visitorFieldRegistry.register(pair.getLeft(), pair.getRight()); });
    return visitorFieldRegistry;
  }
}
