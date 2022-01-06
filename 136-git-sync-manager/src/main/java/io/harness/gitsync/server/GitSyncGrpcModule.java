/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.server;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.sdk.HarnessToGitPushInfoGrpcService;
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
@OwnedBy(DX)
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
    Multibinder<Service> serviceBinder =
        Multibinder.newSetBinder(binder(), Service.class, Names.named("git-sync-services"));
    serviceBinder.addBinding().to(Key.get(Service.class, Names.named("gitsync-grpc-service")));
  }

  @Provides
  @Singleton
  @Named("git-sync")
  public ServiceManager serviceManager(@Named("git-sync-services") Set<Service> services) {
    return new ServiceManager(services);
  }

  @Provides
  @Singleton
  @Named("gitsync-grpc-service")
  public Service gitSyncGrpcService(GitSyncServiceConfiguration configuration, HealthStatusManager healthStatusManager,
      @Named("git-sync-bindable-services") Set<BindableService> services) {
    return new GrpcServer(configuration.getGrpcServerConfig().getConnectors().get(0), services, Collections.emptySet(),
        healthStatusManager);
  }

  @Provides
  @Named("git-sync-bindable-services")
  private Set<BindableService> bindableServices(
      HealthStatusManager healthStatusManager, HarnessToGitPushInfoGrpcService harnessToGitPushInfoGrpcService) {
    Set<BindableService> services = new HashSet<>();
    services.add(healthStatusManager.getHealthService());
    services.add(harnessToGitPushInfoGrpcService);
    return services;
  }
}
