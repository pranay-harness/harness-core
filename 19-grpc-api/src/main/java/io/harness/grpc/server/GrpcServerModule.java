package io.harness.grpc.server;

import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

import io.grpc.BindableService;
import io.grpc.ServerInterceptor;
import io.grpc.protobuf.services.ProtoReflectionService;
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

    Multibinder<Service> serviceBinder = Multibinder.newSetBinder(binder(), Service.class);
    connectors.forEach(connector
        -> serviceBinder.addBinding().toProvider(
            ()
                -> new GrpcServer(connector, bindableServicesProvider.get(), serverInterceptorsProvider.get(),
                    healthStatusManagerProvider.get())));
  }
}
