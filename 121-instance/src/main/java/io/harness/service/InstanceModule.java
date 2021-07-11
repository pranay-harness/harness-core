package io.harness.service;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.HPersistence;
import io.harness.service.infrastructuremapping.InfrastructureMappingService;
import io.harness.service.infrastructuremapping.InfrastructureMappingServiceImpl;
import io.harness.service.instanceService.InstanceService;
import io.harness.service.instanceService.InstanceServiceImpl;
import io.harness.service.instancedashboardservice.InstanceDashboardService;
import io.harness.service.instancedashboardservice.InstanceDashboardServiceImpl;
import io.harness.service.instancesynchandlerfactory.InstanceSyncHandlerFactoryService;
import io.harness.service.instancesynchandlerfactory.InstanceSyncHandlerFactoryServiceImpl;
import io.harness.service.instancesyncperpetualtaskinfo.InstanceSyncPerpetualTaskInfoService;
import io.harness.service.instancesyncperpetualtaskinfo.InstanceSyncPerpetualTaskInfoServiceImpl;

import com.google.inject.AbstractModule;
import java.util.concurrent.atomic.AtomicReference;

@OwnedBy(DX)
public class InstanceModule extends AbstractModule {
  private static final AtomicReference<InstanceModule> instanceRef = new AtomicReference<>();

  public static InstanceModule getInstance() {
    if (instanceRef.get() == null) {
      instanceRef.compareAndSet(null, new InstanceModule());
    }
    return instanceRef.get();
  }

  @Override
  protected void configure() {
    registerRequiredBindings();
    bind(InstanceDashboardService.class).to(InstanceDashboardServiceImpl.class);
    bind(InstanceService.class).to(InstanceServiceImpl.class);
    bind(InstanceSyncPerpetualTaskInfoService.class).to(InstanceSyncPerpetualTaskInfoServiceImpl.class);
    bind(InfrastructureMappingService.class).to(InfrastructureMappingServiceImpl.class);
    bind(InstanceSyncHandlerFactoryService.class).to(InstanceSyncHandlerFactoryServiceImpl.class);
  }

  private void registerRequiredBindings() {
    requireBinding(HPersistence.class);
  }
}
