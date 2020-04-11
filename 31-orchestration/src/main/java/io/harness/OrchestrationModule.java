package io.harness;

import static java.util.Arrays.asList;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Injector;
import com.google.inject.matcher.Matchers;

import io.harness.govern.DependencyModule;
import io.harness.govern.ServersModule;
import io.harness.queue.TimerScheduledExecutorService;
import io.harness.state.inspection.StateInspectionService;
import io.harness.state.inspection.StateInspectionServiceImpl;
import io.harness.waiter.WaiterModule;

import java.io.Closeable;
import java.util.List;
import java.util.Set;

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
    bindListener(Matchers.any(), new RegistryListener());
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return ImmutableSet.<DependencyModule>of(WaiterModule.getInstance(), OrchestrationBeansModule.getInstance());
  }

  @Override
  public List<Closeable> servers(Injector injector) {
    return asList(() -> injector.getInstance(TimerScheduledExecutorService.class).shutdownNow());
  }
}
