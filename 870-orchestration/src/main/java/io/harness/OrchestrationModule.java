package io.harness;

import static java.util.Arrays.asList;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.NoopTaskExecutor;
import io.harness.engine.OrchestrationService;
import io.harness.engine.OrchestrationServiceImpl;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.node.NodeExecutionServiceImpl;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.engine.executions.plan.PlanExecutionServiceImpl;
import io.harness.engine.expressions.EngineExpressionServiceImpl;
import io.harness.engine.expressions.ExpressionEvaluatorProvider;
import io.harness.engine.interrupts.InterruptService;
import io.harness.engine.interrupts.InterruptServiceImpl;
import io.harness.engine.outcomes.OutcomeServiceImpl;
import io.harness.engine.outputs.ExecutionSweepingOutputServiceImpl;
import io.harness.engine.pms.data.PmsEngineExpressionServiceImpl;
import io.harness.engine.pms.data.PmsOutcomeService;
import io.harness.engine.pms.data.PmsOutcomeServiceImpl;
import io.harness.engine.pms.data.PmsSweepingOutputService;
import io.harness.engine.pms.data.PmsSweepingOutputServiceImpl;
import io.harness.engine.pms.tasks.NgDelegate2TaskExecutor;
import io.harness.engine.pms.tasks.TaskExecutor;
import io.harness.govern.ServersModule;
import io.harness.pms.contracts.execution.tasks.TaskCategory;
import io.harness.pms.expression.EngineExpressionService;
import io.harness.pms.expression.PmsEngineExpressionService;
import io.harness.pms.sdk.core.execution.EngineObtainmentHelper;
import io.harness.pms.sdk.core.registries.registrar.ResolverRegistrar;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.waiter.AsyncWaitEngine;
import io.harness.queue.TimerScheduledExecutorService;
import io.harness.registrars.OrchestrationResolverRegistrar;
import io.harness.threading.ThreadPool;
import io.harness.waiter.AsyncWaitEngineImpl;
import io.harness.waiter.WaitNotifyEngine;
import io.harness.waiter.WaiterModule;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Named;
import java.io.Closeable;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@OwnedBy(HarnessTeam.PIPELINE)
public class OrchestrationModule extends AbstractModule implements ServersModule {
  private static OrchestrationModule instance;
  private final OrchestrationModuleConfig config;

  public static OrchestrationModule getInstance(OrchestrationModuleConfig orchestrationModuleConfig) {
    if (instance == null) {
      instance = new OrchestrationModule(orchestrationModuleConfig);
    }
    return instance;
  }

  private OrchestrationModule(OrchestrationModuleConfig config) {
    this.config = config;
  }

  @Override
  protected void configure() {
    install(WaiterModule.getInstance());
    install(OrchestrationDelayModule.getInstance());
    install(OrchestrationBeansModule.getInstance());
    install(OrchestrationQueueModule.getInstance(config));

    bind(NodeExecutionService.class).to(NodeExecutionServiceImpl.class).in(Singleton.class);
    bind(PlanExecutionService.class).to(PlanExecutionServiceImpl.class).in(Singleton.class);
    bind(InterruptService.class).to(InterruptServiceImpl.class);
    bind(OrchestrationService.class).to(OrchestrationServiceImpl.class);
    bind(EngineObtainmentHelper.class).in(Singleton.class);

    MapBinder<String, ResolverRegistrar> resolverRegistrarMapBinder =
        MapBinder.newMapBinder(binder(), String.class, ResolverRegistrar.class);
    resolverRegistrarMapBinder.addBinding(OrchestrationResolverRegistrar.class.getName())
        .to(OrchestrationResolverRegistrar.class);

    MapBinder<TaskCategory, TaskExecutor> taskExecutorMap =
        MapBinder.newMapBinder(binder(), TaskCategory.class, TaskExecutor.class);
    taskExecutorMap.addBinding(TaskCategory.UNKNOWN_CATEGORY).to(NoopTaskExecutor.class);
    taskExecutorMap.addBinding(TaskCategory.DELEGATE_TASK_V2).to(NgDelegate2TaskExecutor.class);

    // PMS Services
    bind(PmsSweepingOutputService.class).to(PmsSweepingOutputServiceImpl.class).in(Singleton.class);
    bind(PmsOutcomeService.class).to(PmsOutcomeServiceImpl.class).in(Singleton.class);
    bind(PmsEngineExpressionService.class).to(PmsEngineExpressionServiceImpl.class).in(Singleton.class);

    if (!config.isWithPMS()) {
      bind(EngineExpressionService.class).to(EngineExpressionServiceImpl.class);
      if (!config.isPipelineService()) {
        bind(ExecutionSweepingOutputService.class).to(ExecutionSweepingOutputServiceImpl.class).in(Singleton.class);
        bind(OutcomeService.class).to(OutcomeServiceImpl.class).in(Singleton.class);
      }
    }
  }

  @Provides
  @Singleton
  @Named("EngineExecutorService")
  public ExecutorService engineExecutionServiceThreadPool() {
    return ThreadPool.create(config.getCorePoolSize(), config.getMaxPoolSize(), config.getIdleTimeInSecs(),
        TimeUnit.SECONDS, new ThreadFactoryBuilder().setNameFormat("EngineExecutorService-%d").build());
  }

  @Provides
  @Singleton
  public ExpressionEvaluatorProvider expressionEvaluatorProvider() {
    return config.getExpressionEvaluatorProvider();
  }

  @Provides
  @Named(OrchestrationPublisherName.PUBLISHER_NAME)
  public String publisherName() {
    return config.getPublisherName();
  }

  @Provides
  @Singleton
  public AsyncWaitEngine asyncWaitEngine(
      WaitNotifyEngine waitNotifyEngine, @Named(OrchestrationPublisherName.PUBLISHER_NAME) String publisherName) {
    return new AsyncWaitEngineImpl(waitNotifyEngine, publisherName);
  }

  @Override
  public List<Closeable> servers(Injector injector) {
    return asList(() -> injector.getInstance(TimerScheduledExecutorService.class).shutdownNow());
  }

  @Provides
  @Singleton
  public OrchestrationModuleConfig orchestrationModuleConfig() {
    return config;
  }
}
