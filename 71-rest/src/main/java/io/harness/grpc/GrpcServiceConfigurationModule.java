package io.harness.grpc;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;

import io.grpc.BindableService;
import io.grpc.ServerInterceptor;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.reflection.v1alpha.ServerReflectionGrpc;
import io.harness.delegate.NgDelegateTaskServiceGrpc;
import io.harness.grpc.auth.DelegateAuthServerInterceptor;
import io.harness.grpc.auth.ServiceAuthServerInterceptor;
import io.harness.grpc.auth.SkippedAuthServerInterceptor;
import io.harness.grpc.ng.manager.DelegateTaskGrpcServer;
import io.harness.grpc.pingpong.PingPongService;
import io.harness.grpc.server.GrpcServerModule;
import io.harness.perpetualtask.grpc.PerpetualTaskServiceGrpc;
import io.harness.security.KeySource;
import software.wings.security.AccountKeySource;

import java.util.Set;

public class GrpcServiceConfigurationModule extends AbstractModule {
  private final GrpcServerConfig grpcServerConfig;
  private final String serviceSecret;

  public GrpcServiceConfigurationModule(GrpcServerConfig grpcServerConfig, String serviceSecret) {
    this.grpcServerConfig = grpcServerConfig;
    this.serviceSecret = serviceSecret;
  }

  @Override
  protected void configure() {
    bind(KeySource.class).to(AccountKeySource.class).in(Singleton.class);
    Multibinder<BindableService> bindableServiceMultibinder = Multibinder.newSetBinder(binder(), BindableService.class);
    bindableServiceMultibinder.addBinding().to(DelegateServiceGrpc.class);
    bindableServiceMultibinder.addBinding().to(PerpetualTaskServiceGrpc.class);
    bindableServiceMultibinder.addBinding().to(PingPongService.class);
    bindableServiceMultibinder.addBinding().to(DelegateTaskGrpcServer.class);

    Multibinder<ServerInterceptor> serverInterceptorMultibinder =
        Multibinder.newSetBinder(binder(), ServerInterceptor.class);
    serverInterceptorMultibinder.addBinding().to(DelegateAuthServerInterceptor.class);
    serverInterceptorMultibinder.addBinding().toProvider(
        ()
            -> new ServiceAuthServerInterceptor(
                ImmutableMap.of("ng-manager", serviceSecret), ImmutableSet.of(NgDelegateTaskServiceGrpc.SERVICE_NAME)));
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
