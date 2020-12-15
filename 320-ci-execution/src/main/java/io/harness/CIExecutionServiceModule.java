package io.harness;

import io.harness.ci.config.CIExecutionServiceConfig;
import io.harness.core.ci.services.CIBuildService;
import io.harness.core.ci.services.CIBuildServiceImpl;
import io.harness.engine.expressions.AmbianceExpressionEvaluatorProvider;
import io.harness.engine.pms.tasks.TaskExecutor;
import io.harness.executionplan.ExecutionPlanModule;
import io.harness.impl.CIPipelineExecutionService;
import io.harness.impl.CIPipelineExecutionServiceImpl;
import io.harness.pms.sdk.registries.registrar.OrchestrationEventHandlerRegistrar;
import io.harness.pms.sdk.registries.registrar.StepRegistrar;
import io.harness.registrars.ExecutionRegistrar;
import io.harness.states.CIDelegateTaskExecutor;
import io.harness.tasks.TaskMode;
import io.harness.waiter.OrchestrationNotifyEventListener;

import ci.pipeline.execution.OrchestrationExecutionEventHandlerRegistrar;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;

public class CIExecutionServiceModule extends AbstractModule {
  private CIExecutionServiceConfig ciExecutionServiceConfig;

  @Inject
  public CIExecutionServiceModule(CIExecutionServiceConfig ciExecutionServiceConfig) {
    this.ciExecutionServiceConfig = ciExecutionServiceConfig;
  }

  @Override
  protected void configure() {
    install(CIBeansModule.getInstance());
    install(OrchestrationStepsModule.getInstance());
    install(OrchestrationVisualizationModule.getInstance());
    install(ExecutionPlanModule.getInstance());

    MapBinder<String, OrchestrationEventHandlerRegistrar> orchestrationEventHandlerRegistrarMapBinder =
        MapBinder.newMapBinder(binder(), String.class, OrchestrationEventHandlerRegistrar.class);
    orchestrationEventHandlerRegistrarMapBinder.addBinding(OrchestrationExecutionEventHandlerRegistrar.class.getName())
        .to(OrchestrationExecutionEventHandlerRegistrar.class);

    install(NGPipelineCommonsModule.getInstance());
    bind(CIBuildService.class).to(CIBuildServiceImpl.class);
    this.bind(CIExecutionServiceConfig.class).toInstance(this.ciExecutionServiceConfig);
    bind(CIPipelineExecutionService.class).to(CIPipelineExecutionServiceImpl.class);
    MapBinder<String, StepRegistrar> stepRegistrarMapBinder =
        MapBinder.newMapBinder(binder(), String.class, StepRegistrar.class);
    stepRegistrarMapBinder.addBinding(ExecutionRegistrar.class.getName()).to(ExecutionRegistrar.class);
    MapBinder<String, TaskExecutor> taskExecutorMap =
        MapBinder.newMapBinder(binder(), String.class, TaskExecutor.class);
    taskExecutorMap.addBinding(TaskMode.DELEGATE_TASK_V3.name()).to(CIDelegateTaskExecutor.class);
  }

  @Provides
  @Singleton
  public OrchestrationModuleConfig orchestrationModuleConfig() {
    return OrchestrationModuleConfig.builder()
        .serviceName("CI")
        .expressionEvaluatorProvider(new AmbianceExpressionEvaluatorProvider())
        .publisherName(OrchestrationNotifyEventListener.ORCHESTRATION)
        .maxPoolSize(10)
        .build();
  }
}
