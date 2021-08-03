package io.harness.pollingframework.server;

import io.harness.grpc.server.GrpcServer;
import io.harness.pollingframework.PollingFrameworkService;

import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import io.grpc.BindableService;
import io.grpc.services.HealthStatusManager;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PollingFrameworkGrpcModule extends AbstractModule {
  private static PollingFrameworkGrpcModule instance;
  private final String deployMode = System.getenv().get("DEPLOY_MODE");

  public static PollingFrameworkGrpcModule getInstance() {
    if (instance == null) {
      instance = new PollingFrameworkGrpcModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    Multibinder<Service> serviceBinder = Multibinder.newSetBinder(binder(), Service.class);
    serviceBinder.addBinding().to(Key.get(Service.class, Names.named("polling-framework-grpc-service")));
  }

  @Provides
  @Singleton
  public ServiceManager serviceManager(Set<Service> services) {
    return new ServiceManager(services);
  }

  @Provides
  @Singleton
  @Named("polling-framework-grpc-service")
  public Service pfGrpcService(PollingFrameworkServiceConfiguration configuration,
      HealthStatusManager healthStatusManager,
      @Named("polling-framework-bindable-services") Set<BindableService> services) {
    return new GrpcServer(configuration.getGrpcServerConfig().getConnectors().get(0), services, Collections.emptySet(),
        healthStatusManager);
  }

  @Provides
  @Named("polling-framework-bindable-services")
  private Set<BindableService> bindableServices(
      HealthStatusManager healthStatusManager, PollingFrameworkService pollingFrameworkService) {
    Set<BindableService> services = new HashSet<>();
    services.add(healthStatusManager.getHealthService());
    services.add(pollingFrameworkService);
    return services;
  }
}
