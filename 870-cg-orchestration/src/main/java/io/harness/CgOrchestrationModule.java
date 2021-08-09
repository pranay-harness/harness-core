package io.harness;

import static java.util.Arrays.asList;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delay.AbstractOrchestrationDelayModule;
import io.harness.govern.ServersModule;
import io.harness.queue.TimerScheduledExecutorService;
import io.harness.state.inspection.StateInspectionService;
import io.harness.state.inspection.StateInspectionServiceImpl;
import io.harness.waiter.AbstractWaiterModule;
import io.harness.waiter.WaiterConfiguration;
import io.harness.waiter.WaiterConfiguration.PersistenceLayer;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import java.io.Closeable;
import java.util.List;

@OwnedBy(HarnessTeam.CDC)
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
    install(new AbstractWaiterModule() {
      @Override
      public WaiterConfiguration waiterConfiguration() {
        return WaiterConfiguration.builder().persistenceLayer(PersistenceLayer.MORPHIA).build();
      }
    });
    install(new AbstractOrchestrationDelayModule() {
      @Override
      public boolean forNG() {
        return false;
      }
    });
    bind(StateInspectionService.class).to(StateInspectionServiceImpl.class);
  }

  @Override
  public List<Closeable> servers(Injector injector) {
    return asList(() -> injector.getInstance(TimerScheduledExecutorService.class).shutdownNow());
  }
}
