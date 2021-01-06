package io.harness.notification;

import io.harness.callback.DelegateCallback;
import io.harness.callback.DelegateCallbackToken;
import io.harness.callback.MongoDatabase;
import io.harness.govern.ProviderModule;
import io.harness.grpc.DelegateServiceDriverGrpcClientModule;
import io.harness.grpc.DelegateServiceGrpcClient;
import io.harness.manage.ManagedScheduledExecutorService;
import io.harness.mongo.MongoConfig;
import io.harness.mongo.MongoModule;
import io.harness.mongo.MongoPersistence;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.notification.modules.NotificationCoreModule;
import io.harness.notification.modules.NotificationPersistenceModule;
import io.harness.persistence.HPersistence;
import io.harness.queue.QueueController;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.NotificationSenderRegistrars;
import io.harness.service.DelegateServiceDriverModule;
import io.harness.version.VersionModule;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.parameternameprovider.ReflectionParameterNameProvider;
import org.mongodb.morphia.converters.TypeConverter;
import ru.vyarus.guice.validator.ValidationModule;

@Slf4j
public class NotificationModule extends AbstractModule {
  private final NotificationConfiguration appConfig;

  public NotificationModule(NotificationConfiguration appConfig) {
    this.appConfig = appConfig;
  }

  @Provides
  @Singleton
  Supplier<DelegateCallbackToken> getDelegateCallbackTokenSupplier(
      DelegateServiceGrpcClient delegateServiceGrpcClient) {
    return Suppliers.memoize(() -> getDelegateCallbackToken(delegateServiceGrpcClient, appConfig));
  }

  private DelegateCallbackToken getDelegateCallbackToken(
      DelegateServiceGrpcClient delegateServiceClient, NotificationConfiguration appConfig) {
    log.info("Generating Delegate callback token");
    final DelegateCallbackToken delegateCallbackToken = delegateServiceClient.registerCallback(
        DelegateCallback.newBuilder()
            .setMongoDatabase(MongoDatabase.newBuilder()
                                  .setCollectionNamePrefix("!!!custom")
                                  .setConnection(appConfig.getMongoConfig().getUri())
                                  .build())
            .build());
    log.info("delegate callback token generated =[{}]", delegateCallbackToken.getToken());
    return delegateCallbackToken;
  }

  @Override
  protected void configure() {
    install(new ProviderModule() {
      @Provides
      @Singleton
      Set<Class<? extends KryoRegistrar>> kryoRegistrars() {
        return ImmutableSet.<Class<? extends KryoRegistrar>>builder()
            .addAll(NotificationSenderRegistrars.kryoRegistrars)
            .build();
      }

      @Provides
      @Singleton
      @Named("notification-channel")
      MongoBackendConfiguration getMongoBackendConfiguration(NotificationConfiguration notificationConfiguration) {
        return (MongoBackendConfiguration) notificationConfiguration.getNotificationClientConfiguration()
            .getNotificationClientBackendConfiguration();
      }

      @Provides
      @Singleton
      Set<Class<? extends MorphiaRegistrar>> morphiaRegistrars() {
        return ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
            .addAll(NotificationSenderRegistrars.morphiaRegistrars)
            .build();
      }

      @Provides
      @Singleton
      Set<Class<? extends TypeConverter>> morphiaConverters() {
        return ImmutableSet.<Class<? extends TypeConverter>>builder().build();
      }

      @Provides
      @Singleton
      MongoConfig mongoConfig() {
        return appConfig.getMongoConfig();
      }
    });
    bind(ManagedScheduledExecutorService.class)
        .annotatedWith(Names.named("delegate-response"))
        .toInstance(new ManagedScheduledExecutorService("delegate-response"));
    bind(NotificationConfiguration.class).toInstance(appConfig);
    install(MongoModule.getInstance());
    bind(HPersistence.class).to(MongoPersistence.class);
    install(DelegateServiceDriverModule.getInstance());
    install(new DelegateServiceDriverGrpcClientModule(appConfig.getNotificationSecrets().getManagerServiceSecret(),
        this.appConfig.getGrpcClientConfig().getTarget(), this.appConfig.getGrpcClientConfig().getAuthority()));

    install(VersionModule.getInstance());
    install(new ValidationModule(getValidatorFactory()));
    install(new NotificationPersistenceModule());
    install(new NotificationCoreModule(appConfig));
    install(new AbstractModule() {
      @Override
      protected void configure() {
        bind(QueueController.class).toInstance(new QueueController() {
          @Override
          public boolean isPrimary() {
            return true;
          }

          @Override
          public boolean isNotPrimary() {
            return false;
          }
        });
      }
    });
  }

  @Provides
  @Singleton
  @Named("morphiaClasses")
  Map<Class, String> morphiaCustomCollectionNames() {
    return ImmutableMap.<Class, String>builder().build();
  }

  private ValidatorFactory getValidatorFactory() {
    return Validation.byDefaultProvider()
        .configure()
        .parameterNameProvider(new ReflectionParameterNameProvider())
        .buildValidatorFactory();
  }
}
