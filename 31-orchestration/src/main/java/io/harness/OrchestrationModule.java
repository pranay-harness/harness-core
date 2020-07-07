package io.harness;

import static java.util.Arrays.asList;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Injector;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;

import io.harness.engine.EngineObtainmentHelper;
import io.harness.engine.OrchestrationService;
import io.harness.engine.OrchestrationServiceImpl;
import io.harness.engine.barriers.BarrierService;
import io.harness.engine.barriers.BarrierServiceImpl;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.node.NodeExecutionServiceImpl;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.engine.executions.plan.PlanExecutionServiceImpl;
import io.harness.engine.expressions.EngineExpressionService;
import io.harness.engine.expressions.EngineExpressionServiceImpl;
import io.harness.engine.expressions.ExpressionEvaluatorProvider;
import io.harness.engine.graph.GraphGenerationService;
import io.harness.engine.graph.GraphGenerationServiceImpl;
import io.harness.engine.interrupts.InterruptService;
import io.harness.engine.interrupts.InterruptServiceImpl;
import io.harness.engine.outcomes.OutcomeService;
import io.harness.engine.outcomes.OutcomeServiceImpl;
import io.harness.engine.outputs.ExecutionSweepingOutputService;
import io.harness.engine.outputs.ExecutionSweepingOutputServiceImpl;
import io.harness.govern.DependencyModule;
import io.harness.govern.ServersModule;
import io.harness.queue.TimerScheduledExecutorService;
import io.harness.registrars.OrchestrationAdviserRegistrar;
import io.harness.registrars.OrchestrationFacilitatorRegistrar;
import io.harness.registrars.OrchestrationModuleEventHandlerRegistrar;
import io.harness.registrars.OrchestrationResolverRegistrar;
import io.harness.registrars.OrchestrationStepRegistrar;
import io.harness.registries.registrar.AdviserRegistrar;
import io.harness.registries.registrar.FacilitatorRegistrar;
import io.harness.registries.registrar.OrchestrationEventHandlerRegistrar;
import io.harness.registries.registrar.ResolverRegistrar;
import io.harness.registries.registrar.StepRegistrar;
import io.harness.state.inspection.StateInspectionService;
import io.harness.state.inspection.StateInspectionServiceImpl;
import io.harness.threading.ThreadPool;
import io.harness.waiter.WaiterModule;

import java.io.Closeable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class OrchestrationModule extends DependencyModule implements ServersModule {
  private static OrchestrationModule instance;

  private final OrchestrationModuleConfig config;

  public OrchestrationModule(OrchestrationModuleConfig config) {
    this.config = Preconditions.checkNotNull(config);
  }

  public static OrchestrationModule getInstance(OrchestrationModuleConfig config) {
    if (instance == null) {
      instance = new OrchestrationModule(config);
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(StateInspectionService.class).to(StateInspectionServiceImpl.class);
    bind(NodeExecutionService.class).to(NodeExecutionServiceImpl.class);
    bind(PlanExecutionService.class).to(PlanExecutionServiceImpl.class);
    bind(InterruptService.class).to(InterruptServiceImpl.class);
    bind(EngineExpressionService.class).to(EngineExpressionServiceImpl.class);
    bind(OutcomeService.class).to(OutcomeServiceImpl.class);
    bind(ExecutionSweepingOutputService.class).to(ExecutionSweepingOutputServiceImpl.class);
    bind(OrchestrationService.class).to(OrchestrationServiceImpl.class);
    bind(GraphGenerationService.class).to(GraphGenerationServiceImpl.class);
    bind(BarrierService.class).to(BarrierServiceImpl.class);
    bind(EngineObtainmentHelper.class).toInstance(new EngineObtainmentHelper());
    bind(ExecutorService.class)
        .annotatedWith(Names.named("EngineExecutorService"))
        .toInstance(ThreadPool.create(config.getCorePoolSize(), config.getMaxPoolSize(), config.getIdleTimeInSecs(),
            TimeUnit.SECONDS, new ThreadFactoryBuilder().setNameFormat("EngineExecutorService-%d").build()));
    bind(ExpressionEvaluatorProvider.class).toInstance(config.getExpressionEvaluatorProvider());

    MapBinder<String, StepRegistrar> stepRegistrarMapBinder =
        MapBinder.newMapBinder(binder(), String.class, StepRegistrar.class);
    stepRegistrarMapBinder.addBinding(OrchestrationStepRegistrar.class.getName()).to(OrchestrationStepRegistrar.class);
    MapBinder<String, AdviserRegistrar> adviserRegistrarMapBinder =
        MapBinder.newMapBinder(binder(), String.class, AdviserRegistrar.class);
    adviserRegistrarMapBinder.addBinding(OrchestrationAdviserRegistrar.class.getName())
        .to(OrchestrationAdviserRegistrar.class);
    MapBinder<String, ResolverRegistrar> resolverRegistrarMapBinder =
        MapBinder.newMapBinder(binder(), String.class, ResolverRegistrar.class);
    resolverRegistrarMapBinder.addBinding(OrchestrationResolverRegistrar.class.getName())
        .to(OrchestrationResolverRegistrar.class);
    MapBinder<String, FacilitatorRegistrar> facilitatorRegistrarMapBinder =
        MapBinder.newMapBinder(binder(), String.class, FacilitatorRegistrar.class);
    facilitatorRegistrarMapBinder.addBinding(OrchestrationFacilitatorRegistrar.class.getName())
        .to(OrchestrationFacilitatorRegistrar.class);
    MapBinder<String, OrchestrationEventHandlerRegistrar> orchestrationEventHandlerRegistrarMapBinder =
        MapBinder.newMapBinder(binder(), String.class, OrchestrationEventHandlerRegistrar.class);
    orchestrationEventHandlerRegistrarMapBinder.addBinding(OrchestrationModuleEventHandlerRegistrar.class.getName())
        .to(OrchestrationModuleEventHandlerRegistrar.class);
    bind(String.class)
        .annotatedWith(Names.named(OrchestrationPublisherName.PUBLISHER_NAME))
        .toInstance(config.getPublisherName());
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return ImmutableSet.<DependencyModule>of(
        WaiterModule.getInstance(), OrchestrationBeansModule.getInstance(), OrchestrationQueueModule.getInstance());
  }

  @Override
  public List<Closeable> servers(Injector injector) {
    return asList(() -> injector.getInstance(TimerScheduledExecutorService.class).shutdownNow());
  }
}
