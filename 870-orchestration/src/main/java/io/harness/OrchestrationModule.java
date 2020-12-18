package io.harness;

import static java.util.Arrays.asList;

import io.harness.engine.NoopTaskExecutor;
import io.harness.engine.OrchestrationService;
import io.harness.engine.OrchestrationServiceImpl;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.node.NodeExecutionServiceImpl;
import io.harness.engine.executions.node.PmsNodeExecutionServiceImpl;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.engine.executions.plan.PlanExecutionServiceImpl;
import io.harness.engine.expressions.EngineExpressionServiceImpl;
import io.harness.engine.expressions.ExpressionEvaluatorProvider;
import io.harness.engine.interrupts.InterruptService;
import io.harness.engine.interrupts.InterruptServiceImpl;
import io.harness.engine.outcomes.OutcomeServiceImpl;
import io.harness.engine.outputs.ExecutionSweepingOutputServiceImpl;
import io.harness.engine.pms.data.PmsOutcomeService;
import io.harness.engine.pms.data.PmsOutcomeServiceImpl;
import io.harness.engine.pms.data.PmsSweepingOutputService;
import io.harness.engine.pms.data.PmsSweepingOutputServiceImpl;
import io.harness.engine.pms.tasks.NgDelegate2TaskExecutor;
import io.harness.engine.pms.tasks.TaskExecutor;
import io.harness.govern.ServersModule;
import io.harness.pms.contracts.execution.tasks.TaskCategory;
import io.harness.pms.expression.EngineExpressionService;
import io.harness.pms.sdk.core.execution.EngineObtainmentHelper;
import io.harness.pms.sdk.core.execution.PmsNodeExecutionService;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeGrpcServiceImpl;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingGrpcOutputService;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.waiter.AsyncWaitEngine;
import io.harness.pms.sdk.execution.PmsNodeExecutionServiceGrpcImpl;
import io.harness.pms.sdk.registries.registrar.OrchestrationEventHandlerRegistrar;
import io.harness.pms.sdk.registries.registrar.ResolverRegistrar;
import io.harness.queue.TimerScheduledExecutorService;
import io.harness.registrars.OrchestrationModuleEventHandlerRegistrar;
import io.harness.registrars.OrchestrationResolverRegistrar;
import io.harness.service.DelegateServiceDriverModule;
import io.harness.state.inspection.StateInspectionService;
import io.harness.state.inspection.StateInspectionServiceImpl;
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

public class OrchestrationModule extends AbstractModule implements ServersModule {
  private static OrchestrationModule instance;

  public static OrchestrationModule getInstance() {
    if (instance == null) {
      instance = new OrchestrationModule();
    }
    return instance;
  }

  private OrchestrationModule() {}

  @Override
  protected void configure() {
    install(WaiterModule.getInstance());
    install(DelegateServiceDriverModule.getInstance());
    install(OrchestrationBeansModule.getInstance());
    install(OrchestrationQueueModule.getInstance());

    bind(StateInspectionService.class).to(StateInspectionServiceImpl.class);
    bind(NodeExecutionService.class).to(NodeExecutionServiceImpl.class);
    bind(PlanExecutionService.class).to(PlanExecutionServiceImpl.class);
    bind(InterruptService.class).to(InterruptServiceImpl.class);
    bind(EngineExpressionService.class).to(EngineExpressionServiceImpl.class);
    bind(OrchestrationService.class).to(OrchestrationServiceImpl.class);
    bind(EngineObtainmentHelper.class).in(Singleton.class);

    MapBinder<String, ResolverRegistrar> resolverRegistrarMapBinder =
        MapBinder.newMapBinder(binder(), String.class, ResolverRegistrar.class);
    resolverRegistrarMapBinder.addBinding(OrchestrationResolverRegistrar.class.getName())
        .to(OrchestrationResolverRegistrar.class);
    MapBinder<String, OrchestrationEventHandlerRegistrar> orchestrationEventHandlerRegistrarMapBinder =
        MapBinder.newMapBinder(binder(), String.class, OrchestrationEventHandlerRegistrar.class);
    orchestrationEventHandlerRegistrarMapBinder.addBinding(OrchestrationModuleEventHandlerRegistrar.class.getName())
        .to(OrchestrationModuleEventHandlerRegistrar.class);

    MapBinder<TaskCategory, TaskExecutor> taskExecutorMap =
        MapBinder.newMapBinder(binder(), TaskCategory.class, TaskExecutor.class);
    taskExecutorMap.addBinding(TaskCategory.UNKNOWN_CATEGORY).to(NoopTaskExecutor.class);
    taskExecutorMap.addBinding(TaskCategory.DELEGATE_TASK_V2).to(NgDelegate2TaskExecutor.class);

    // PMS Services
    bind(PmsSweepingOutputService.class).to(PmsSweepingOutputServiceImpl.class).in(Singleton.class);
    bind(PmsOutcomeService.class).to(PmsOutcomeServiceImpl.class).in(Singleton.class);
  }

  @Provides
  @Singleton
  @Named("EngineExecutorService")
  public ExecutorService engineExecutionServiceThreadPool(OrchestrationModuleConfig config) {
    return ThreadPool.create(config.getCorePoolSize(), config.getMaxPoolSize(), config.getIdleTimeInSecs(),
        TimeUnit.SECONDS, new ThreadFactoryBuilder().setNameFormat("EngineExecutorService-%d").build());
  }

  @Provides
  @Singleton
  public ExpressionEvaluatorProvider expressionEvaluatorProvider(OrchestrationModuleConfig config) {
    return config.getExpressionEvaluatorProvider();
  }

  @Provides
  @Named(OrchestrationPublisherName.PUBLISHER_NAME)
  public String publisherName(OrchestrationModuleConfig config) {
    return config.getPublisherName();
  }

  @Provides
  @Singleton
  public ExecutionSweepingOutputService executionSweepingOutputService(
      OrchestrationModuleConfig config, Injector injector) {
    if (config.isWithPMS()) {
      return injector.getInstance(ExecutionSweepingGrpcOutputService.class);
    } else {
      return injector.getInstance(ExecutionSweepingOutputServiceImpl.class);
    }
  }

  @Provides
  @Singleton
  public OutcomeService outcomeService(OrchestrationModuleConfig config, Injector injector) {
    if (config.isWithPMS()) {
      return injector.getInstance(OutcomeGrpcServiceImpl.class);
    } else {
      return injector.getInstance(OutcomeServiceImpl.class);
    }
  }

  @Provides
  @Singleton
  public PmsNodeExecutionService pmsNodeExecutionService(OrchestrationModuleConfig config, Injector injector) {
    if (config.isWithPMS()) {
      return injector.getInstance(PmsNodeExecutionServiceGrpcImpl.class);
    } else {
      return injector.getInstance(PmsNodeExecutionServiceImpl.class);
    }
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
}
