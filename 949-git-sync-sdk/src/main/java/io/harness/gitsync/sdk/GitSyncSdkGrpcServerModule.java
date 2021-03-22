package io.harness.gitsync.sdk;

import static io.harness.gitsync.GitSyncSdkConfiguration.DeployMode.REMOTE;
import static io.harness.gitsync.sdk.GitSyncGrpcConstants.GitSyncSdkInternalService;

import io.harness.gitsync.GitSyncSdkConfiguration;
import io.harness.gitsync.gittoharness.GitToHarnessGrpcService;
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

public class GitSyncSdkGrpcServerModule extends AbstractModule {
  private static GitSyncSdkGrpcServerModule instance;
  private final String deployMode = System.getenv().get("DEPLOY_MODE");

  public static GitSyncSdkGrpcServerModule getInstance() {
    if (instance == null) {
      instance = new GitSyncSdkGrpcServerModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    Multibinder<Service> serviceBinder = Multibinder.newSetBinder(binder(), Service.class);
    serviceBinder.addBinding().to(Key.get(Service.class, Names.named("gitsync-sdk-grpc-service")));
  }

  @Provides
  @Singleton
  public ServiceManager serviceManager(Set<Service> services) {
    return new ServiceManager(services);
  }

  @Provides
  @Singleton
  @Named("gitsync-sdk-grpc-service")
  public Service gitSyncGrpcService(HealthStatusManager healthStatusManager, Set<BindableService> services,
      GitSyncSdkConfiguration gitSyncSdkConfiguration) {
    if (gitSyncSdkConfiguration.getDeployMode() == REMOTE) {
      return new GrpcServer(gitSyncSdkConfiguration.getGrpcServerConfig().getConnectors().get(0), services,
          Collections.emptySet(), healthStatusManager);
    }
    return new GrpcInProcessServer(GitSyncSdkInternalService, services, Collections.emptySet(), healthStatusManager);
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
