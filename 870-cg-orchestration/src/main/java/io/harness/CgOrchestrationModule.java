package io.harness;

import static java.util.Arrays.asList;

import io.harness.govern.ServersModule;
import io.harness.queue.TimerScheduledExecutorService;
import io.harness.state.inspection.StateInspectionService;
import io.harness.state.inspection.StateInspectionServiceImpl;
import io.harness.waiter.WaiterModule;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import java.io.Closeable;
import java.util.List;

public class CgOrchestrationModule extends AbstractModule implements ServersModule {
  private static CgOrchestrationModule instance;

  public static CgOrchestrationModule getInstance() {
    if (instance == null) {
      instance = new CgOrchestrationModule();
    }
    return instance;
  }

  private CgOrchestrationModule() {}

  @Override
  protected void configure() {
    install(WaiterModule.getInstance());
    install(OrchestrationDelayModule.getInstance());
    install(CgNgSharedOrchestrationModule.getInstance());
    bind(StateInspectionService.class).to(StateInspectionServiceImpl.class);
  }

  @Override
  public List<Closeable> servers(Injector injector) {
    return asList(() -> injector.getInstance(TimerScheduledExecutorService.class).shutdownNow());
  }
}
