/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.grpc.server;

import io.harness.grpc.auth.ServiceAuthServerInterceptor;
import io.harness.grpc.auth.ServiceInfo;
import io.harness.grpc.auth.ValidateAuthServerInterceptor;

import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import io.grpc.BindableService;
import io.grpc.ServerInterceptor;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.reflection.v1alpha.ServerReflectionGrpc;
import io.grpc.services.HealthStatusManager;
import java.util.List;
import java.util.Set;

public class GrpcServerModule extends AbstractModule {
  private final List<Connector> connectors;
  private final Provider<Set<ServerInterceptor>> serverInterceptorsProvider;
  private final Provider<Set<BindableService>> bindableServicesProvider;

  public GrpcServerModule(List<Connector> connectors, Provider<Set<BindableService>> bindableServicesProvider,
      Provider<Set<ServerInterceptor>> serverInterceptorsProvider) {
    this.connectors = connectors;
    this.bindableServicesProvider = bindableServicesProvider;
    this.serverInterceptorsProvider = serverInterceptorsProvider;
  }

  @Override
  protected void configure() {
    bind(HealthStatusManager.class).in(Singleton.class);
    Multibinder<BindableService> bindableServiceMultibinder = Multibinder.newSetBinder(binder(), BindableService.class);
    bindableServiceMultibinder.addBinding().toProvider(ProtoReflectionService::newInstance).in(Singleton.class);
    Provider<HealthStatusManager> healthStatusManagerProvider = getProvider(HealthStatusManager.class);
    bindableServiceMultibinder.addBinding().toProvider(() -> healthStatusManagerProvider.get().getHealthService());

    Multibinder<ServerInterceptor> serverInterceptorMultibinder =
        Multibinder.newSetBinder(binder(), ServerInterceptor.class);

    Multibinder<String> nonAuthServices =
        Multibinder.newSetBinder(binder(), String.class, Names.named("excludedGrpcAuthValidationServices"));
    nonAuthServices.addBinding().toInstance(HealthGrpc.SERVICE_NAME);
    nonAuthServices.addBinding().toInstance(ServerReflectionGrpc.SERVICE_NAME);
    serverInterceptorMultibinder.addBinding().to(ValidateAuthServerInterceptor.class);

    MapBinder.newMapBinder(binder(), String.class, ServiceInfo.class);
    serverInterceptorMultibinder.addBinding().to(ServiceAuthServerInterceptor.class);

    Multibinder<Service> serviceBinder = Multibinder.newSetBinder(binder(), Service.class);
    connectors.forEach(connector
        -> serviceBinder.addBinding().toProvider(
            ()
                -> new GrpcServer(connector, bindableServicesProvider.get(), serverInterceptorsProvider.get(),
                    healthStatusManagerProvider.get())));
  }
}
