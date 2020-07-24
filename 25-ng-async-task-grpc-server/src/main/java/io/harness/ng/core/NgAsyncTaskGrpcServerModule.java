package io.harness.ng.core;

import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;

import io.grpc.BindableService;
import io.grpc.ServerInterceptor;
import io.harness.delegate.NgDelegateTaskResponseServiceGrpc;
import io.harness.grpc.auth.ServiceInfo;
import io.harness.grpc.server.GrpcServerConfig;
import io.harness.grpc.server.GrpcServerModule;
import io.harness.ng.core.remote.server.grpc.NgDelegateTaskResponseGrpcServer;

import java.util.Set;

public final class NgAsyncTaskGrpcServerModule extends AbstractModule {
  private final GrpcServerConfig grpcServerConfig;
  private final String serviceName;
  private final String serviceSecret;

  @Inject
  public NgAsyncTaskGrpcServerModule(GrpcServerConfig grpcServerConfig, String serviceName, String serviceSecret) {
    this.grpcServerConfig = grpcServerConfig;
    this.serviceName = serviceName;
    this.serviceSecret = serviceSecret;
  }

  @Override
  protected void configure() {
    Multibinder<BindableService> bindableServiceMultibinder = Multibinder.newSetBinder(binder(), BindableService.class);
    bindableServiceMultibinder.addBinding().to(NgDelegateTaskResponseGrpcServer.class);

    MapBinder<String, ServiceInfo> stringServiceInfoMapBinder =
        MapBinder.newMapBinder(binder(), String.class, ServiceInfo.class);

    stringServiceInfoMapBinder.addBinding(NgDelegateTaskResponseServiceGrpc.SERVICE_NAME)
        .toInstance(ServiceInfo.builder().id(serviceName).secret(serviceSecret).build());

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
