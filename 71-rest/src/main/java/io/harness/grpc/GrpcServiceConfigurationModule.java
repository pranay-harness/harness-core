package io.harness.grpc;

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
import io.harness.grpc.auth.AuthService;
import io.harness.grpc.auth.DelegateAuthServerInterceptor;
import io.harness.grpc.server.GrpcServerModule;
import io.harness.perpetualtask.grpc.PerpetualTaskServiceGrpc;

import java.util.Set;

public class GrpcServiceConfigurationModule extends AbstractModule {
  private final GrpcServerConfig grpcServerConfig;

  public GrpcServiceConfigurationModule(GrpcServerConfig grpcServerConfig) {
    this.grpcServerConfig = grpcServerConfig;
  }

  @Override
  protected void configure() {
    // TODO(avmohan): Remove this once [CCM-47] is done.
    bind(AuthService.class).toProvider(() -> token -> {}).in(Singleton.class);

    Multibinder<BindableService> bindableServiceMultibinder = Multibinder.newSetBinder(binder(), BindableService.class);
    bindableServiceMultibinder.addBinding().to(PerpetualTaskServiceGrpc.class);

    Multibinder<ServerInterceptor> serverInterceptorMultibinder =
        Multibinder.newSetBinder(binder(), ServerInterceptor.class);
    serverInterceptorMultibinder.addBinding().to(DelegateAuthServerInterceptor.class);

    install(new GrpcServerModule(grpcServerConfig.getConnectors(), //
        getProvider(Key.get(new TypeLiteral<Set<BindableService>>() {})),
        getProvider(Key.get(new TypeLiteral<Set<ServerInterceptor>>() {}))));
  }

  @Provides
  @Singleton
  public ServiceManager serviceManager(Set<Service> services) {
    return new ServiceManager(services);
  }
}
