package io.harness.event.app;

import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;

import io.grpc.BindableService;
import io.grpc.ServerInterceptor;
import io.harness.event.MessageProcessorType;
import io.harness.event.grpc.EventPublisherServerImpl;
import io.harness.event.grpc.MessageProcessor;
import io.harness.event.service.impl.LastReceivedPublishedMessageRepositoryImpl;
import io.harness.event.service.intfc.LastReceivedPublishedMessageRepository;
import io.harness.grpc.auth.DelegateAuthServerInterceptor;
import io.harness.grpc.server.GrpcServerModule;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.persistence.HPersistence;
import io.harness.security.KeySource;
import io.harness.serializer.EventsServerRegistrars;
import io.harness.serializer.KryoRegistrar;
import software.wings.dl.WingsMongoPersistence;
import software.wings.dl.WingsPersistence;
import software.wings.security.AccountKeySource;
import software.wings.service.impl.security.NoOpSecretManagerImpl;
import software.wings.service.intfc.security.SecretManager;

import java.util.Collections;
import java.util.Set;

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
    bind(KeySource.class).to(AccountKeySource.class).in(Singleton.class);
    bind(SecretManager.class).to(NoOpSecretManagerImpl.class);
    bind(LastReceivedPublishedMessageRepository.class).to(LastReceivedPublishedMessageRepositoryImpl.class);

    Multibinder<BindableService> bindableServiceMultibinder = Multibinder.newSetBinder(binder(), BindableService.class);
    bindableServiceMultibinder.addBinding().to(EventPublisherServerImpl.class);

    Multibinder<ServerInterceptor> serverInterceptorMultibinder =
        Multibinder.newSetBinder(binder(), ServerInterceptor.class);
    serverInterceptorMultibinder.addBinding().to(DelegateAuthServerInterceptor.class);

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
  }

  @Provides
  @Singleton
  @Named("morphiaClasses")
  public Set<Class<?>> morphiaClasses() {
    return Collections.emptySet();
  }

  @Provides
  @Singleton
  public Set<Class<? extends KryoRegistrar>> kryoRegistrars() {
    return EventsServerRegistrars.kryoRegistrars;
  }

  @Provides
  @Singleton
  public Set<Class<? extends MorphiaRegistrar>> morphiaRegistrars() {
    return EventsServerRegistrars.morphiaRegistrars;
  }

  @Provides
  @Singleton
  public ServiceManager serviceManager(Set<Service> services) {
    return new ServiceManager(services);
  }
}
