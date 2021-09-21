package io.harness;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.service.instancesyncperpetualtaskstatus.InstanceSyncPerpetualTaskStatusService;
import io.harness.service.instancesyncperpetualtaskstatus.InstanceSyncPerpetualTaskStatusServiceImpl;

import com.google.inject.AbstractModule;
import java.util.concurrent.atomic.AtomicReference;

@OwnedBy(HarnessTeam.PL)
public class CGInstanceSyncModule extends AbstractModule {
  private static final AtomicReference<CGInstanceSyncModule> instanceRef = new AtomicReference<>();

  public static CGInstanceSyncModule getInstance() {
    if (instanceRef.get() == null) {
      instanceRef.compareAndSet(null, new CGInstanceSyncModule());
    }
    return instanceRef.get();
  }

  @Override
  protected void configure() {
    bind(InstanceSyncPerpetualTaskStatusService.class).to(InstanceSyncPerpetualTaskStatusServiceImpl.class);
  }
}
