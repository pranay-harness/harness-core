package io.harness.ccm;

import static io.harness.AuthorizationServiceHeader.CE_NEXT_GEN;
import static io.harness.AuthorizationServiceHeader.MANAGER;
import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.eventsframework.EventsFrameworkConstants.ENTITY_CRUD;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.CONNECTOR_ENTITY;
import static io.harness.lock.DistributedLockImplementation.MONGO;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.retry.MethodExecutionHelper;
import io.harness.annotations.retry.RetryOnException;
import io.harness.annotations.retry.RetryOnExceptionInterceptor;
import io.harness.app.PrimaryVersionManagerModule;
import io.harness.ccm.bigQuery.BigQueryService;
import io.harness.ccm.bigQuery.BigQueryServiceImpl;
import io.harness.ccm.commons.beans.config.GcpConfig;
import io.harness.ccm.commons.service.impl.ClusterRecordServiceImpl;
import io.harness.ccm.commons.service.intf.ClusterRecordService;
import io.harness.ccm.eventframework.ConnectorEntityCRUDStreamListener;
import io.harness.ccm.perpetualtask.K8sWatchTaskResourceClientModule;
import io.harness.ccm.service.impl.CEYamlServiceImpl;
import io.harness.ccm.service.intf.CEYamlService;
import io.harness.ccm.views.service.CEReportScheduleService;
import io.harness.ccm.views.service.CEViewService;
import io.harness.ccm.views.service.ViewCustomFieldService;
import io.harness.ccm.views.service.ViewsBillingService;
import io.harness.ccm.views.service.impl.CEReportScheduleServiceImpl;
import io.harness.ccm.views.service.impl.CEViewServiceImpl;
import io.harness.ccm.views.service.impl.ViewCustomFieldServiceImpl;
import io.harness.ccm.views.service.impl.ViewsBillingServiceImpl;
import io.harness.connector.ConnectorResourceClientModule;
import io.harness.ff.FeatureFlagModule;
import io.harness.govern.ProviderMethodInterceptor;
import io.harness.govern.ProviderModule;
import io.harness.lock.DistributedLockImplementation;
import io.harness.mongo.AbstractMongoModule;
import io.harness.mongo.MongoConfig;
import io.harness.mongo.MongoPersistence;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.ng.core.event.MessageListener;
import io.harness.persistence.HPersistence;
import io.harness.persistence.NoopUserProvider;
import io.harness.persistence.UserProvider;
import io.harness.queryconverter.SQLConverter;
import io.harness.queryconverter.SQLConverterImpl;
import io.harness.redis.RedisConfig;
import io.harness.serializer.CENextGenModuleRegistrars;
import io.harness.serializer.KryoRegistrar;
import io.harness.threading.ExecutorModule;
import io.harness.time.TimeModule;
import io.harness.timescaledb.JooqModule;
import io.harness.timescaledb.TimeScaleDBConfig;
import io.harness.timescaledb.metrics.HExecuteListener;
import io.harness.timescaledb.metrics.QueryStatsPrinter;
import io.harness.version.VersionModule;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import org.hibernate.validator.parameternameprovider.ReflectionParameterNameProvider;
import org.jooq.ExecuteListener;
import org.mongodb.morphia.converters.TypeConverter;
import org.springframework.core.convert.converter.Converter;
import ru.vyarus.guice.validator.ValidationModule;

@OwnedBy(CE)
public class CENextGenModule extends AbstractModule {
  private final CENextGenConfiguration configuration;

  public CENextGenModule(CENextGenConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  protected void configure() {
    install(new ProviderModule() {
      @Provides
      @Singleton
      Set<Class<? extends KryoRegistrar>> kryoRegistrars() {
        return ImmutableSet.<Class<? extends KryoRegistrar>>builder()
            .addAll(CENextGenModuleRegistrars.kryoRegistrars)
            .build();
      }

      @Provides
      @Singleton
      Set<Class<? extends MorphiaRegistrar>> morphiaRegistrars() {
        return ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
            .addAll(CENextGenModuleRegistrars.morphiaRegistrars)
            .build();
      }

      @Provides
      @Singleton
      Set<Class<? extends TypeConverter>> morphiaConverters() {
        return ImmutableSet.<Class<? extends TypeConverter>>builder()
            .addAll(CENextGenModuleRegistrars.morphiaConverters)
            .build();
      }

      @Provides
      @Singleton
      List<Class<? extends Converter<?, ?>>> springConverters() {
        return ImmutableList.<Class<? extends Converter<?, ?>>>builder()
            .addAll(CENextGenModuleRegistrars.springConverters)
            .build();
      }

      @Provides
      @Singleton
      MongoConfig eventsMongoConfig() {
        return configuration.getEventsMongoConfig();
      }

      @Provides
      @Singleton
      @Named("TimeScaleDBConfig")
      TimeScaleDBConfig timeScaleDBConfig() {
        return configuration.getTimeScaleDBConfig();
      }

      @Provides
      @Singleton
      @Named("PSQLExecuteListener")
      ExecuteListener executeListener() {
        return HExecuteListener.getInstance();
      }

      @Provides
      @Singleton
      @Named("gcpConfig")
      GcpConfig gcpConfig() {
        return configuration.getGcpConfig();
      }
    });

    // Bind Services
    bind(CEYamlService.class).to(CEYamlServiceImpl.class);

    install(new CENextGenPersistenceModule());
    install(ExecutorModule.getInstance());
    install(new AbstractMongoModule() {
      @Override
      public UserProvider userProvider() {
        return new NoopUserProvider();
      }
    });
    install(new ConnectorResourceClientModule(
        configuration.getNgManagerClientConfig(), configuration.getNgManagerServiceSecret(), MANAGER.getServiceId()));
    install(new K8sWatchTaskResourceClientModule(
        configuration.getManagerClientConfig(), configuration.getNgManagerServiceSecret(), CE_NEXT_GEN.getServiceId()));
    install(VersionModule.getInstance());
    install(PrimaryVersionManagerModule.getInstance());
    install(new ValidationModule(getValidatorFactory()));
    install(TimeModule.getInstance());
    install(FeatureFlagModule.getInstance());
    install(new EventsFrameworkModule(configuration.getEventsFrameworkConfiguration()));
    install(JooqModule.getInstance());
    bind(HPersistence.class).to(MongoPersistence.class);
    bind(CENextGenConfiguration.class).toInstance(configuration);
    bind(SQLConverter.class).to(SQLConverterImpl.class);
    bind(BigQueryService.class).to(BigQueryServiceImpl.class);
    bind(ViewsBillingService.class).to(ViewsBillingServiceImpl.class);
    bind(CEViewService.class).to(CEViewServiceImpl.class);
    bind(ClusterRecordService.class).to(ClusterRecordServiceImpl.class);
    bind(ViewCustomFieldService.class).to(ViewCustomFieldServiceImpl.class);
    bind(CEReportScheduleService.class).to(CEReportScheduleServiceImpl.class);
    bind(QueryStatsPrinter.class).toInstance(HExecuteListener.getInstance());

    registerEventsFrameworkMessageListeners();

    bindRetryOnExceptionInterceptor();
  }

  private void bindRetryOnExceptionInterceptor() {
    bind(MethodExecutionHelper.class).asEagerSingleton();
    ProviderMethodInterceptor retryOnExceptionInterceptor =
        new ProviderMethodInterceptor(getProvider(RetryOnExceptionInterceptor.class));
    bindInterceptor(Matchers.any(), Matchers.annotatedWith(RetryOnException.class), retryOnExceptionInterceptor);
  }

  @Provides
  @Singleton
  @Named("morphiaClasses")
  Map<Class, String> morphiaCustomCollectionNames() {
    return ImmutableMap.<Class, String>builder().build();
  }

  @Provides
  @Singleton
  DistributedLockImplementation distributedLockImplementation() {
    return MONGO;
  }

  @Provides
  @Named("lock")
  @Singleton
  RedisConfig redisConfig() {
    return RedisConfig.builder().build();
  }

  private ValidatorFactory getValidatorFactory() {
    return Validation.byDefaultProvider()
        .configure()
        .parameterNameProvider(new ReflectionParameterNameProvider())
        .buildValidatorFactory();
  }

  private void registerEventsFrameworkMessageListeners() {
    bind(MessageListener.class)
        .annotatedWith(Names.named(CONNECTOR_ENTITY + ENTITY_CRUD))
        .to(ConnectorEntityCRUDStreamListener.class);
  }
}
