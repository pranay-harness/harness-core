package io.harness.event.app;

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
import io.harness.event.grpc.EventPublisherServerImpl;
import io.harness.event.service.impl.LastReceivedPublishedMessageRepositoryImpl;
import io.harness.event.service.intfc.LastReceivedPublishedMessageRepository;
import io.harness.grpc.auth.DelegateAuthServerInterceptor;
import io.harness.grpc.server.GrpcServerModule;
import io.harness.persistence.HPersistence;
import io.harness.security.KeySource;
import software.wings.dl.WingsMongoPersistence;
import software.wings.dl.WingsPersistence;
import software.wings.security.AccountKeySource;
import software.wings.service.impl.security.NoOpSecretManagerImpl;
import software.wings.service.intfc.security.SecretManager;

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

    install(new GrpcServerModule(eventServiceConfig.getConnectors(), //
        getProvider(Key.get(new TypeLiteral<Set<BindableService>>() {})),
        getProvider(Key.get(new TypeLiteral<Set<ServerInterceptor>>() {}))));
  }

  @Provides
  @Singleton
  public ServiceManager serviceManager(Set<Service> services) {
    return new ServiceManager(services);
  }
}
