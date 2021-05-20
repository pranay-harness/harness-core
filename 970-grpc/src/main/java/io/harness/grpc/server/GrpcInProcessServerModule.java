package io.harness.grpc.server;

import io.harness.data.structure.EmptyPredicate;
import io.harness.grpc.auth.ServiceInfo;

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
import java.util.Set;

public class GrpcInProcessServerModule extends AbstractModule {
  private final Provider<Set<ServerInterceptor>> serverInterceptorsProvider;
  private final Provider<Set<BindableService>> bindableServicesProvider;
  private final String serviceBinderName;
  private final String serverName;

  public GrpcInProcessServerModule(Provider<Set<BindableService>> bindableServicesProvider,
      Provider<Set<ServerInterceptor>> serverInterceptorsProvider, String serviceBinderName, String serverName) {
    this.bindableServicesProvider = bindableServicesProvider;
    this.serverInterceptorsProvider = serverInterceptorsProvider;
    this.serviceBinderName = serviceBinderName;
    this.serverName = serverName;
  }

  @Override
  protected void configure() {
    bind(HealthStatusManager.class).in(Singleton.class);
    Multibinder<BindableService> bindableServiceMultibinder;
    bindableServiceMultibinder = Multibinder.newSetBinder(binder(), BindableService.class, Names.named("internal"));
    bindableServiceMultibinder.addBinding().toProvider(ProtoReflectionService::newInstance).in(Singleton.class);
    Provider<HealthStatusManager> healthStatusManagerProvider = getProvider(HealthStatusManager.class);
    bindableServiceMultibinder.addBinding().toProvider(() -> healthStatusManagerProvider.get().getHealthService());

    Multibinder<String> nonAuthServices =
        Multibinder.newSetBinder(binder(), String.class, Names.named("excludedGrpcAuthValidationServices"));
    nonAuthServices.addBinding().toInstance(HealthGrpc.SERVICE_NAME);
    nonAuthServices.addBinding().toInstance(ServerReflectionGrpc.SERVICE_NAME);

    MapBinder.newMapBinder(binder(), String.class, ServiceInfo.class);
    Multibinder<Service> serviceBinder;
    if (EmptyPredicate.isEmpty(serviceBinderName)) {
      serviceBinder = Multibinder.newSetBinder(binder(), Service.class);
    } else {
      serviceBinder = Multibinder.newSetBinder(binder(), Service.class, Names.named(serviceBinderName));
    }
    serviceBinder.addBinding().toProvider(()
                                              -> new GrpcInProcessServer(serverName, bindableServicesProvider.get(),
                                                  serverInterceptorsProvider.get(), healthStatusManagerProvider.get()));
  }
}
