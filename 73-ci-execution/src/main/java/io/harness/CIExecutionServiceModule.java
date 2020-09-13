package io.harness;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;

import io.harness.core.ci.services.CIBuildService;
import io.harness.core.ci.services.CIBuildServiceImpl;
import io.harness.engine.expressions.AmbianceExpressionEvaluatorProvider;
import io.harness.impl.CIPipelineExecutionService;
import io.harness.impl.CIPipelineExecutionServiceImpl;
import io.harness.registrars.ExecutionRegistrar;
import io.harness.registries.registrar.StepRegistrar;

public class CIExecutionServiceModule extends AbstractModule {
  private static CIExecutionServiceModule instance;

  public static CIExecutionServiceModule getInstance() {
    if (instance == null) {
      instance = new CIExecutionServiceModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    install(OrchestrationModule.getInstance(OrchestrationModuleConfig.builder()
                                                .expressionEvaluatorProvider(new AmbianceExpressionEvaluatorProvider())
                                                .build()));
    install(OrchestrationStepsModule.getInstance());
    bind(CIBuildService.class).to(CIBuildServiceImpl.class);
    bind(CIPipelineExecutionService.class).to(CIPipelineExecutionServiceImpl.class);
    MapBinder<String, StepRegistrar> stepRegistrarMapBinder =
        MapBinder.newMapBinder(binder(), String.class, StepRegistrar.class);
    stepRegistrarMapBinder.addBinding(ExecutionRegistrar.class.getName()).to(ExecutionRegistrar.class);
  }
}