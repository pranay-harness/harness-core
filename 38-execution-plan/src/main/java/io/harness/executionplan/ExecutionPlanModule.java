package io.harness.executionplan;

import static com.google.inject.Scopes.SINGLETON;

import io.harness.executionplan.core.ExecutionPlanCreatorRegistry;
import io.harness.executionplan.core.impl.ExecutionPlanCreatorRegistryImpl;
import io.harness.executionplan.plancreators.ParallelStepPlanCreator;
import io.harness.executionplan.service.ExecutionPlanCreatorHelper;
import io.harness.executionplan.service.ExecutionPlanCreatorService;
import io.harness.executionplan.service.impl.ExecutionPlanCreatorServiceImpl;
import io.harness.govern.DependencyModule;

import java.util.Collections;
import java.util.Set;

public class ExecutionPlanModule extends DependencyModule {
  private static volatile ExecutionPlanModule instance;

  public static ExecutionPlanModule getInstance() {
    if (instance == null) {
      instance = new ExecutionPlanModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(ExecutionPlanCreatorService.class).to(ExecutionPlanCreatorServiceImpl.class).in(SINGLETON);
    bind(ExecutionPlanCreatorRegistry.class).to(ExecutionPlanCreatorRegistryImpl.class).in(SINGLETON);
    bind(ParallelStepPlanCreator.class).in(SINGLETON);
    bind(ExecutionPlanCreatorHelper.class).in(SINGLETON);
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return Collections.emptySet();
  }
}
