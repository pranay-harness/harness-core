package io.harness.delegate.app;

import io.harness.delegate.DelegateServiceGrpc;
import io.harness.grpc.DelegateServiceGrpcImpl;
import io.harness.grpc.auth.DelegateAuthServerInterceptor;
import io.harness.grpc.auth.ServiceInfo;
import io.harness.grpc.auth.ValidateAuthServerInterceptor;
import io.harness.grpc.exception.GrpcExceptionMapper;
import io.harness.grpc.exception.WingsExceptionGrpcMapper;
import io.harness.grpc.server.Connector;
import io.harness.grpc.server.GrpcServer;
import io.harness.grpc.server.GrpcServerExceptionHandler;
import io.harness.pingpong.DelegateServicePingPongGrpc;
import io.harness.security.KeySource;
import io.harness.service.DelegateServicePingPongService;

import software.wings.security.AccountKeySource;

import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
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

public class DelegateServiceGrpcServerModule extends AbstractModule {
  private DelegateServiceConfig delegateServiceConfig;

  public DelegateServiceGrpcServerModule(DelegateServiceConfig delegateServiceConfig) {
    this.delegateServiceConfig = delegateServiceConfig;
  }

  @Override
  protected void configure() {
    bind(KeySource.class).to(AccountKeySource.class).in(Singleton.class);
    Provider<Set<BindableService>> bindableServicesProvider =
        getProvider(Key.get(new TypeLiteral<Set<BindableService>>() {}));
    Multibinder<BindableService> bindableServiceMultibinder = Multibinder.newSetBinder(binder(), BindableService.class);
    bindableServiceMultibinder.addBinding().toProvider(ProtoReflectionService::newInstance).in(Singleton.class);
    Provider<HealthStatusManager> healthStatusManagerProvider = getProvider(HealthStatusManager.class);
    bindableServiceMultibinder.addBinding().toProvider(() -> healthStatusManagerProvider.get().getHealthService());
    bindableServiceMultibinder.addBinding().to(DelegateServiceGrpcImpl.class);
    bindableServiceMultibinder.addBinding().to(DelegateServicePingPongService.class);

    MapBinder<String, ServiceInfo> stringServiceInfoMapBinder =
        MapBinder.newMapBinder(binder(), String.class, ServiceInfo.class);
    stringServiceInfoMapBinder.addBinding(DelegateServiceGrpc.SERVICE_NAME)
        .toInstance(ServiceInfo.builder()
                        .id("delegate-service-management")
                        .secret(delegateServiceConfig.getDelegateServiceSecret())
                        .build());
    stringServiceInfoMapBinder.addBinding(DelegateServicePingPongGrpc.SERVICE_NAME)
        .toInstance(ServiceInfo.builder()
                        .id("delegate-service-ping-pong")
                        .secret(delegateServiceConfig.getDelegateServiceSecret())
                        .build());
    Multibinder<GrpcExceptionMapper> expectionMapperMultibinder =
        Multibinder.newSetBinder(binder(), GrpcExceptionMapper.class);
    expectionMapperMultibinder.addBinding().to(WingsExceptionGrpcMapper.class);

    Provider<Set<GrpcExceptionMapper>> grpcExceptionMappersProvider =
        getProvider(Key.get(new TypeLiteral<Set<GrpcExceptionMapper>>() {}));

    Multibinder<ServerInterceptor> serverInterceptorMultibinder =
        Multibinder.newSetBinder(binder(), ServerInterceptor.class);
    serverInterceptorMultibinder.addBinding().to(DelegateAuthServerInterceptor.class);

    serverInterceptorMultibinder.addBinding().toProvider(
        () -> new GrpcServerExceptionHandler(grpcExceptionMappersProvider));

    Multibinder<String> nonAuthServices =
        Multibinder.newSetBinder(binder(), String.class, Names.named("excludedGrpcAuthValidationServices"));
    nonAuthServices.addBinding().toInstance(HealthGrpc.SERVICE_NAME);
    nonAuthServices.addBinding().toInstance(ServerReflectionGrpc.SERVICE_NAME);
    serverInterceptorMultibinder.addBinding().to(ValidateAuthServerInterceptor.class);

    // Service Interceptors
    Provider<Set<ServerInterceptor>> serverInterceptorsProvider =
        getProvider(Key.get(new TypeLiteral<Set<ServerInterceptor>>() {}));

    Multibinder<Service> serviceBinder = Multibinder.newSetBinder(binder(), Service.class);
    List<Connector> connectors = delegateServiceConfig.getGrpcServerConfig().getConnectors();
    connectors.forEach(connector
        -> serviceBinder.addBinding().toProvider(
            ()
                -> new GrpcServer(connector, bindableServicesProvider.get(), serverInterceptorsProvider.get(),
                    healthStatusManagerProvider.get())));
  }
}
