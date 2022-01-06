/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.sdk;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.gitsync.GitSyncSdkConfiguration.DeployMode.REMOTE;
import static io.harness.gitsync.sdk.GitSyncGrpcConstants.GitSyncSdkInternalService;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.GitSyncSdkConfiguration;
import io.harness.gitsync.fullsync.FullSyncGrpcService;
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

@OwnedBy(DX)
public class GitSyncSdkGrpcServerModule extends AbstractModule {
  private static GitSyncSdkGrpcServerModule instance;

  public static GitSyncSdkGrpcServerModule getInstance() {
    if (instance == null) {
      instance = new GitSyncSdkGrpcServerModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    Multibinder<Service> serviceBinder =
        Multibinder.newSetBinder(binder(), Service.class, Names.named("git-sync-sdk-services"));
    serviceBinder.addBinding().to(Key.get(Service.class, Names.named("gitsync-sdk-grpc-service")));
  }

  @Provides
  @Singleton
  @Named("gitsync-sdk-service-manager")
  public ServiceManager serviceManager(@Named("git-sync-sdk-services") Set<Service> services) {
    return new ServiceManager(services);
  }

  @Provides
  @Singleton
  @Named("gitsync-sdk-grpc-service")
  public Service gitSyncGrpcService(HealthStatusManager healthStatusManager,
      @Named("git-sync-sdk-bindable-services") Set<BindableService> services,
      GitSyncSdkConfiguration gitSyncSdkConfiguration) {
    if (gitSyncSdkConfiguration.getDeployMode() == REMOTE) {
      return new GrpcServer(gitSyncSdkConfiguration.getGrpcServerConfig().getConnectors().get(0), services,
          Collections.emptySet(), healthStatusManager);
    }
    return new GrpcInProcessServer(GitSyncSdkInternalService, services, Collections.emptySet(), healthStatusManager);
  }

  @Provides
  @Named("git-sync-sdk-bindable-services")
  private Set<BindableService> bindableServices(HealthStatusManager healthStatusManager,
      GitToHarnessGrpcService gitToHarnessGrpcService, FullSyncGrpcService fullSyncGrpcService) {
    Set<BindableService> services = new HashSet<>();
    services.add(healthStatusManager.getHealthService());
    services.add(gitToHarnessGrpcService);
    services.add(fullSyncGrpcService);
    return services;
  }
}
