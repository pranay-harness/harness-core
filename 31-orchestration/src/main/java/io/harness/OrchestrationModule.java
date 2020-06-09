package io.harness;

import static java.util.Arrays.asList;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Injector;
import com.google.inject.name.Names;

import io.harness.engine.EngineObtainmentHelper;
import io.harness.engine.EngineService;
import io.harness.engine.EngineServiceImpl;
import io.harness.engine.GraphGenerator;
import io.harness.engine.expressions.EngineExpressionService;
import io.harness.engine.expressions.EngineExpressionServiceImpl;
import io.harness.engine.interrupts.InterruptService;
import io.harness.engine.interrupts.InterruptServiceImpl;
import io.harness.engine.services.NodeExecutionService;
import io.harness.engine.services.OutcomeService;
import io.harness.engine.services.PlanExecutionService;
import io.harness.engine.services.impl.NodeExecutionServiceImpl;
import io.harness.engine.services.impl.OutcomeServiceImpl;
import io.harness.engine.services.impl.PlanExecutionServiceImpl;
import io.harness.govern.DependencyModule;
import io.harness.govern.ServersModule;
import io.harness.mongo.MongoPersistence;
import io.harness.persistence.HPersistence;
import io.harness.queue.TimerScheduledExecutorService;
import io.harness.resolver.sweepingoutput.ExecutionSweepingOutputService;
import io.harness.resolver.sweepingoutput.ExecutionSweepingOutputServiceImpl;
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

  public static OrchestrationModule getInstance() {
    if (instance == null) {
      instance = new OrchestrationModule();
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
    bind(HPersistence.class).annotatedWith(Names.named("enginePersistence")).to(MongoPersistence.class);
    bind(EngineService.class).to(EngineServiceImpl.class);
    bind(GraphGenerator.class).toInstance(new GraphGenerator());
    bind(EngineObtainmentHelper.class).toInstance(new EngineObtainmentHelper());
    bind(ExecutorService.class)
        .annotatedWith(Names.named("EngineExecutorService"))
        .toInstance(ThreadPool.create(
            1, 5, 10, TimeUnit.SECONDS, new ThreadFactoryBuilder().setNameFormat("EngineExecutorService-%d").build()));
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
