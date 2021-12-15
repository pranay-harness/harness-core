package io.harness.event.app;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.event.MessageProcessorType;
import io.harness.event.grpc.DelegateTokenEventServerAuthenticatorImpl;
import io.harness.event.grpc.EventPublisherServerImpl;
import io.harness.event.grpc.MessageProcessor;
import io.harness.event.metrics.EventServiceMetricsPublisher;
import io.harness.event.service.impl.LastReceivedPublishedMessageRepositoryImpl;
import io.harness.event.service.intfc.LastReceivedPublishedMessageRepository;
import io.harness.grpc.auth.DelegateAuthServerInterceptor;
import io.harness.grpc.exception.GrpcExceptionMapper;
import io.harness.grpc.exception.WingsExceptionGrpcMapper;
import io.harness.grpc.server.GrpcServerExceptionHandler;
import io.harness.grpc.server.GrpcServerModule;
import io.harness.lock.PersistentLockModule;
import io.harness.metrics.modules.MetricsModule;
import io.harness.metrics.service.api.MetricsPublisher;
import io.harness.outbox.api.OutboxDao;
import io.harness.outbox.api.OutboxService;
import io.harness.outbox.api.impl.OutboxDaoImpl;
import io.harness.outbox.api.impl.OutboxServiceImpl;
import io.harness.persistence.HPersistence;
import io.harness.repositories.outbox.OutboxEventRepository;
import io.harness.security.DelegateTokenAuthenticator;
import io.harness.service.DelegateServiceModule;
import io.harness.service.impl.DelegateNgTokenServiceImpl;
import io.harness.service.impl.DelegateTokenServiceImpl;
import io.harness.service.intfc.DelegateNgTokenService;
import io.harness.service.intfc.DelegateTokenService;

import software.wings.dl.WingsMongoPersistence;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.security.NoOpSecretManagerImpl;
import software.wings.service.intfc.security.EncryptedSettingAttributes;
import software.wings.service.intfc.security.SecretManager;

import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import io.grpc.BindableService;
import io.grpc.ServerInterceptor;
import java.util.Collections;
import java.util.Set;

@OwnedBy(PL)
public class EventServiceModule extends AbstractModule {
  private final EventServiceConfig eventServiceConfig;

  public EventServiceModule(EventServiceConfig eventServiceConfig) {
    this.eventServiceConfig = eventServiceConfig;
  }

  @Override
  protected void configure() {
    bind(EventServiceConfig.class).toInstance(eventServiceConfig);
    bind(HPersistence.class).to(WingsMongoPersistence.class).in(Singleton.class);
    bind(WingsPersistence.class).to(WingsMongoPersistence.class).in(Singleton.class);
    bind(SecretManager.class).to(NoOpSecretManagerImpl.class);
    bind(EncryptedSettingAttributes.class).to(NoOpSecretManagerImpl.class);
    bind(LastReceivedPublishedMessageRepository.class).to(LastReceivedPublishedMessageRepositoryImpl.class);

    Multibinder<BindableService> bindableServiceMultibinder = Multibinder.newSetBinder(binder(), BindableService.class);
    bindableServiceMultibinder.addBinding().to(EventPublisherServerImpl.class);

    Multibinder<ServerInterceptor> serverInterceptorMultibinder =
        Multibinder.newSetBinder(binder(), ServerInterceptor.class);
    serverInterceptorMultibinder.addBinding().to(DelegateAuthServerInterceptor.class);

    Multibinder<GrpcExceptionMapper> expectionMapperMultibinder =
        Multibinder.newSetBinder(binder(), GrpcExceptionMapper.class);
    expectionMapperMultibinder.addBinding().to(WingsExceptionGrpcMapper.class);

    Provider<Set<GrpcExceptionMapper>> grpcExceptionMappersProvider =
        getProvider(Key.get(new TypeLiteral<Set<GrpcExceptionMapper>>() {}));
    serverInterceptorMultibinder.addBinding().toProvider(
        () -> new GrpcServerExceptionHandler(grpcExceptionMappersProvider));

    MapBinder<MessageProcessorType, MessageProcessor> mapBinder =
        MapBinder.newMapBinder(binder(), MessageProcessorType.class, MessageProcessor.class);
    for (MessageProcessorType messageProcessorType : MessageProcessorType.values()) {
      mapBinder.addBinding(messageProcessorType)
          .to(messageProcessorType.getMessageProcessorClass())
          .in(Singleton.class);
    }

    install(new GrpcServerModule(eventServiceConfig.getConnectors(), //
        getProvider(Key.get(new TypeLiteral<Set<BindableService>>() {})),
        getProvider(Key.get(new TypeLiteral<Set<ServerInterceptor>>() {}))));

    install(new RegistrarsModule());

    install(new MetricsModule());
    bind(MetricsPublisher.class).to(EventServiceMetricsPublisher.class).in(Scopes.SINGLETON);
    bind(DelegateTokenAuthenticator.class).to(DelegateTokenEventServerAuthenticatorImpl.class).in(Scopes.SINGLETON);
  }

  @Provides
  @Singleton
  @Named("morphiaClasses")
  public Set<Class<?>> morphiaClasses() {
    return Collections.emptySet();
  }

  @Provides
  @Singleton
  public ServiceManager serviceManager(Set<Service> services) {
    return new ServiceManager(services);
  }
}
