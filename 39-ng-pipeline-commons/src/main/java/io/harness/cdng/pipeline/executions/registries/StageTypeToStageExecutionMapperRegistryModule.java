package io.harness.cdng.pipeline.executions.registries;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import io.harness.cdng.pipeline.StageTypeToStageExecutionSummaryMapper;
import io.harness.pipeline.executions.NGStageType;
import io.harness.registrars.StageTypeToStageExecutionMapperRegistrar;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
    Set classes = new HashSet<>();
    stageExecutionHelperRegistrarMap.values().forEach(
        stageExecutionHelperRegistrar -> { stageExecutionHelperRegistrar.register(classes); });
    StageTypeToStageExecutionMapperHelperRegistry visitorFieldRegistry =
        new StageTypeToStageExecutionMapperHelperRegistry();
    injector.injectMembers(visitorFieldRegistry);
    classes.forEach(pair -> {
      Pair<NGStageType, Class<? extends StageTypeToStageExecutionSummaryMapper<?>>>
          stageTypeToStageExecutionSummaryMapperClasses =
              (Pair<NGStageType, Class<? extends StageTypeToStageExecutionSummaryMapper<?>>>) pair;
      visitorFieldRegistry.register(stageTypeToStageExecutionSummaryMapperClasses.getLeft(),
          stageTypeToStageExecutionSummaryMapperClasses.getRight());
    });
    return visitorFieldRegistry;
  }
}
