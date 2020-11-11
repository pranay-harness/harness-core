package io.harness.grpc.server;

import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

import io.grpc.BindableService;
import io.grpc.services.HealthStatusManager;
import io.harness.pms.sdk.creator.PlanCreatorService;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PmsSdkGrpcModule extends AbstractModule {
  private final GrpcServerConfig grpcServerConfig;

  public PmsSdkGrpcModule(GrpcServerConfig grpcServerConfig) {
    this.grpcServerConfig = grpcServerConfig;
  }

  @Override
  protected void configure() {
    Multibinder<Service> serviceBinder = Multibinder.newSetBinder(binder(), Service.class);
    serviceBinder.addBinding().to(Key.get(Service.class, Names.named("pms-grpc-service")));
  }

  @Provides
  @Singleton
  @Named("pms-grpc-service")
  public Service pmsGrpcService(HealthStatusManager healthStatusManager, PlanCreatorService planCreatorService) {
    Set<BindableService> cdServices = new HashSet<>();
    cdServices.add(healthStatusManager.getHealthService());
    cdServices.add(planCreatorService);
    return new GrpcServer(
        grpcServerConfig.getConnectors().get(0), cdServices, Collections.emptySet(), healthStatusManager);
  }
}
