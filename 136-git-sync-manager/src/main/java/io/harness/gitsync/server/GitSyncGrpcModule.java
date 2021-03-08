package io.harness.gitsync.server;

import io.harness.gitsync.common.GitSyncConstants;
import io.harness.grpc.server.GrpcInProcessServer;
import io.harness.grpc.server.GrpcServer;

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
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GitSyncGrpcModule extends AbstractModule {
  private static GitSyncGrpcModule instance;
  private final String deployMode = System.getenv().get("DEPLOY_MODE");

  public static GitSyncGrpcModule getInstance() {
    if (instance == null) {
      instance = new GitSyncGrpcModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    Multibinder<Service> serviceBinder = Multibinder.newSetBinder(binder(), Service.class);
    serviceBinder.addBinding().to(Key.get(Service.class, Names.named("gitsync-grpc-service")));
    serviceBinder.addBinding().to(Key.get(Service.class, Names.named("gitsync-grpc-internal-service")));
  }

  @Provides
  @Singleton
  public ServiceManager serviceManager(Set<Service> services) {
    return new ServiceManager(services);
  }

  @Provides
  @Singleton
  @Named("gitsync-grpc-service")
  public Service gitSyncGrpcService(GitSyncServiceConfiguration configuration, HealthStatusManager healthStatusManager,
      Set<BindableService> services) {
    return new GrpcServer(configuration.getGrpcServerConfig().getConnectors().get(0), services, Collections.emptySet(),
        healthStatusManager);
  }

  @Provides
  @Singleton
  @Named("gitsync-grpc-internal-service")
  public Service gitSyncGrpcInternalService(HealthStatusManager healthStatusManager, Set<BindableService> services) {
    return new GrpcInProcessServer(
        GitSyncConstants.INTERNAL_SERVICE_NAME, services, Collections.emptySet(), healthStatusManager);
  }

  @Provides
  private Set<BindableService> bindableServices(
      HealthStatusManager healthStatusManager, GitToHarnessGrpcService gitToHarnessGrpcService) {
    Set<BindableService> services = new HashSet<>();
    services.add(healthStatusManager.getHealthService());
    services.add(gitToHarnessGrpcService);
    return services;
  }
}
