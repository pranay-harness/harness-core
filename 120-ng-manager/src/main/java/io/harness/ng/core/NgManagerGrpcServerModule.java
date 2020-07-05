package io.harness.ng.core;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;

import io.grpc.BindableService;
import io.grpc.ServerInterceptor;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.reflection.v1alpha.ServerReflectionGrpc;
import io.harness.delegate.NgDelegateTaskResponseServiceGrpc;
import io.harness.grpc.GrpcServerConfig;
import io.harness.grpc.auth.ServiceAuthServerInterceptor;
import io.harness.grpc.auth.SkippedAuthServerInterceptor;
import io.harness.grpc.server.GrpcServerModule;
import io.harness.ng.core.remote.server.grpc.NgDelegateTaskResponseGrpcServer;

import java.util.Set;

public final class NgManagerGrpcServerModule extends AbstractModule {
  private final GrpcServerConfig grpcServerConfig;
  private final String serviceSecret;

  @Inject
  public NgManagerGrpcServerModule(GrpcServerConfig grpcServerConfig, String serviceSecret) {
    this.grpcServerConfig = grpcServerConfig;
    this.serviceSecret = serviceSecret;
  }

  @Override
  protected void configure() {
    Multibinder<BindableService> bindableServiceMultibinder = Multibinder.newSetBinder(binder(), BindableService.class);
    bindableServiceMultibinder.addBinding().to(NgDelegateTaskResponseGrpcServer.class);

    Multibinder<ServerInterceptor> serverInterceptorMultibinder =
        Multibinder.newSetBinder(binder(), ServerInterceptor.class);
    serverInterceptorMultibinder.addBinding().toProvider(
        ()
            -> new ServiceAuthServerInterceptor(ImmutableMap.of("manager", serviceSecret),
                ImmutableSet.of(NgDelegateTaskResponseServiceGrpc.SERVICE_NAME)));
    serverInterceptorMultibinder.addBinding().toProvider(
        ()
            -> new SkippedAuthServerInterceptor(
                ImmutableSet.of(HealthGrpc.SERVICE_NAME, ServerReflectionGrpc.SERVICE_NAME)));

    install(new GrpcServerModule(grpcServerConfig.getConnectors(),
        getProvider(Key.get(new TypeLiteral<Set<BindableService>>() {})),
        getProvider(Key.get(new TypeLiteral<Set<ServerInterceptor>>() {}))));
  }

  @Provides
  @Singleton
  public ServiceManager serviceManager(Set<Service> services) {
    return new ServiceManager(services);
  }
}
