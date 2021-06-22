package io.harness.delegate.app;

import static io.harness.annotations.dev.HarnessModule._420_DELEGATE_SERVICE;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.delegate.beans.TaskGroup.GCB;
import static io.harness.logging.LoggingInitializer.initializeLogging;

import static com.google.inject.matcher.Matchers.not;

import io.harness.annotations.dev.TargetModule;
import io.harness.beans.DelegateTask;
import io.harness.cache.CacheModule;
import io.harness.capability.CapabilityModule;
// import io.harness.controller.PrimaryVersionChangeScheduler;
import io.harness.cf.AbstractCfModule;
import io.harness.cf.CfClientConfig;
import io.harness.cf.CfMigrationConfig;
import io.harness.commandlibrary.client.CommandLibraryServiceClientModule;
import io.harness.config.PublisherConfiguration;
import io.harness.config.WorkersConfiguration;
import io.harness.configuration.DeployMode;
import io.harness.cvng.client.CVNGClientModule;
import io.harness.delegate.beans.DelegateAsyncTaskResponse;
import io.harness.delegate.beans.DelegateSyncTaskResponse;
import io.harness.delegate.beans.DelegateTaskProgressResponse;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.resources.DelegateTaskResource;
import io.harness.event.EventsModule;
import io.harness.event.usagemetrics.EventsModuleHelper;
import io.harness.exception.ConstraintViolationExceptionMapper;
import io.harness.exception.WingsException;
import io.harness.ff.FeatureFlagConfig;
import io.harness.govern.ProviderModule;
import io.harness.grpc.GrpcServiceConfigurationModule;
import io.harness.grpc.server.GrpcServerConfig;
import io.harness.health.HealthService;
import io.harness.maintenance.MaintenanceController;
import io.harness.metrics.HarnessMetricRegistry;
import io.harness.metrics.MetricRegistryModule;
import io.harness.migrations.MigrationModule;
import io.harness.mongo.AbstractMongoModule;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.persistence.HPersistence;
import io.harness.persistence.UserProvider;
import io.harness.serializer.AnnotationAwareJsonSubtypeResolver;
import io.harness.serializer.CurrentGenRegistrars;
import io.harness.serializer.DelegateServiceDriverRegistrars;
import io.harness.serializer.KryoRegistrar;
import io.harness.service.DelegateServiceModule;
import io.harness.springdata.SpringPersistenceModule;
import io.harness.stream.AtmosphereBroadcaster;
import io.harness.stream.GuiceObjectFactory;
import io.harness.stream.StreamModule;
import io.harness.threading.ExecutorModule;
import io.harness.threading.ThreadPool;

import software.wings.app.AuthModule;
import software.wings.app.CharsetResponseFilter;
import software.wings.app.GcpMarketplaceIntegrationModule;
import software.wings.app.GraphQLModule;
import software.wings.app.IndexMigratorModule;
import software.wings.app.InspectCommand;
import software.wings.app.MainConfiguration;
import software.wings.app.MainConfiguration.AssetsConfigurationMixin;
import software.wings.app.ManagerExecutorModule;
import software.wings.app.ManagerQueueModule;
import software.wings.app.SSOModule;
import software.wings.app.SearchModule;
import software.wings.app.SignupModule;
import software.wings.app.TemplateModule;
import software.wings.app.WingsModule;
import software.wings.app.YamlModule;
import software.wings.beans.GcpConfig;
import software.wings.beans.User;
import software.wings.beans.command.GcbTaskParams;
import software.wings.beans.command.GcbTaskParams.GcbTaskType;
import software.wings.dl.WingsPersistence;
import software.wings.exception.GenericExceptionMapper;
import software.wings.exception.JsonProcessingExceptionMapper;
import software.wings.exception.WingsExceptionMapper;
import software.wings.jersey.JsonViews;
import software.wings.jersey.KryoFeature;
import software.wings.licensing.LicenseService;
import software.wings.resources.AppResource;
import software.wings.security.AuthResponseFilter;
import software.wings.security.AuthRuleFilter;
import software.wings.security.AuthenticationFilter;
import software.wings.security.LoginRateLimitFilter;
import software.wings.security.ThreadLocalUserProvider;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.intfc.DelegateTaskServiceClassic;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.dirkraft.dropwizard.fileassets.FileAssetsBundle;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ServiceManager;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.name.Named;
import com.palominolabs.metrics.guice.MetricsInstrumentationModule;
import io.dropwizard.Application;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.bundles.assets.AssetsConfiguration;
import io.dropwizard.bundles.assets.ConfiguredAssetsBundle;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.errors.EarlyEofExceptionMapper;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.cache.Cache;
import javax.servlet.ServletRegistration;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import javax.ws.rs.Path;
import lombok.extern.slf4j.Slf4j;
import org.atmosphere.cpr.AtmosphereServlet;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.cpr.MetaBroadcaster;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ServerConnector;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.model.Resource;
import org.hibernate.validator.parameternameprovider.ReflectionParameterNameProvider;
import org.mongodb.morphia.converters.TypeConverter;
import org.reflections.Reflections;
import org.springframework.core.convert.converter.Converter;
import ru.vyarus.guice.validator.ValidationModule;

@Slf4j
@TargetModule(_420_DELEGATE_SERVICE)
public class DelegateServiceApplication extends Application<DelegateServiceConfig> {
  private Bootstrap<DelegateServiceConfig> bootstrap;

  public static void main(String... args) throws Exception {
    new DelegateServiceApplication().run(args);
  }

  @Override
  public void initialize(Bootstrap<DelegateServiceConfig> bootstrap) {
    initializeLogging();
    log.info("bootstrapping ...");
    bootstrap.addCommand(new InspectCommand<>(this));

    // Enable variable substitution with environment variables
    bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
        bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));
    bootstrap.addBundle(new ConfiguredAssetsBundle("/static", "/", "index.html"));
    bootstrap.addBundle(new SwaggerBundle<DelegateServiceConfig>() {
      @Override
      protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(DelegateServiceConfig delegateServiceConfig) {
        return delegateServiceConfig.getSwaggerBundleConfiguration();
      }
    });
    bootstrap.addBundle(new FileAssetsBundle("/.well-known"));
    ObjectMapper mapper = Jackson.newObjectMapper(new YAMLFactory());
    //    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    //    mapper.findAndRegisterModules();
    //    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    bootstrap.setObjectMapper(mapper);
    configureObjectMapper(bootstrap.getObjectMapper());
    //    bootstrap.setMetricRegistry(metricRegistry);
    this.bootstrap = bootstrap;
    log.info("bootstrapping done.");
  }

  public static void configureObjectMapper(final ObjectMapper mapper) {
    mapper.addMixIn(AssetsConfiguration.class, AssetsConfigurationMixin.class);
    final AnnotationAwareJsonSubtypeResolver subtypeResolver =
        AnnotationAwareJsonSubtypeResolver.newInstance(mapper.getSubtypeResolver());
    mapper.setSubtypeResolver(subtypeResolver);
    mapper.setConfig(mapper.getSerializationConfig().withView(JsonViews.Public.class));
    mapper.setAnnotationIntrospector(new JacksonAnnotationIntrospector() {
      // defining a different serialVersionUID then base. We don't care about serializing it.
      private static final long serialVersionUID = 7777451630128399020L;
      @Override
      public List<NamedType> findSubtypes(Annotated a) {
        final List<NamedType> subtypesFromSuper = super.findSubtypes(a);
        if (isNotEmpty(subtypesFromSuper)) {
          return subtypesFromSuper;
        }
        return emptyIfNull(subtypeResolver.findSubtypes(a));
      }
    });
  }

  //  @Override
  //  public void run(DelegateServiceConfig delegateServiceConfig, Environment environment) throws Exception {
  //    log.info("Starting Delegate Service App");
  //    ExecutorModule.getInstance().setExecutorService(ThreadPool.create(
  //        20, 1000, 500L, TimeUnit.MILLISECONDS, new
  //        ThreadFactoryBuilder().setNameFormat("main-app-pool-%d").build()));
  //
  //    ObjectMapper mapper = bootstrap.getObjectMapper();
  //    configureObjectMapper(mapper);
  //    //    mapper.findAndRegisterModules();
  //    //    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  //    MainConfiguration mainConfiguration = mapper.readValue(
  //        new File("/Users/rktummala/IdeaProjects/portal/360-cg-manager/config.yml"), MainConfiguration.class);
  //    List<Module> modules = new ArrayList<>();
  //    //    MainConfiguration mainConfiguration = new MainConfiguration();
  //    //    PortalConfig portalConfig = new PortalConfig();
  //    //    mainConfiguration.setPortal(portalConfig);
  //    //    GrpcClientConfig grpcClientConfig = GrpcClientConfig.builder().build();
  //    //    mainConfiguration.setGrpcDelegateServiceClientConfig(grpcClientConfig);
  //    //    modules.add(new YamlModule());
  //
  //    modules.add(new ProviderModule() {
  //      @Provides
  //      @Singleton
  //      Set<Class<? extends KryoRegistrar>> kryoRegistrars() {
  //        return ImmutableSet.<Class<? extends KryoRegistrar>>builder()
  //            .addAll(CurrentGenRegistrars.kryoRegistrars)
  //            .build();
  //      }
  //      @Provides
  //      @Singleton
  //      Set<Class<? extends MorphiaRegistrar>> morphiaRegistrars() {
  //        return ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
  //            .addAll(CurrentGenRegistrars.morphiaRegistrars)
  //            .build();
  //      }
  //
  //      @Provides
  //      @Singleton
  //      Set<Class<? extends org.mongodb.morphia.converters.TypeConverter>> morphiaConverters() {
  //        return ImmutableSet.<Class<? extends org.mongodb.morphia.converters.TypeConverter>>builder()
  //            .addAll(CurrentGenRegistrars.morphiaConverters)
  //            .build();
  //      }
  //
  //      @Provides
  //      @Singleton
  //      List<Class<? extends Converter<?, ?>>> springConverters() {
  //        return ImmutableList.<Class<? extends Converter<?, ?>>>builder()
  //            .addAll(CurrentGenRegistrars.springConverters)
  //            .build();
  //      }
  //    });
  //
  ////    modules.add(new AbstractMongoModule() {
  ////      @Override
  ////      public UserProvider userProvider() {
  ////        return new ThreadLocalUserProvider();
  ////      }
  ////    });
  //
  //    modules.add(new SpringPersistenceModule());
  //
  //    ValidatorFactory validatorFactory = Validation.byDefaultProvider()
  //        .configure()
  //        .parameterNameProvider(new ReflectionParameterNameProvider())
  //        .buildValidatorFactory();
  //
  //    CacheModule cacheModule = new CacheModule(mainConfiguration.getCacheConfig());
  //    modules.add(cacheModule);
  //    modules.add(new ProviderModule() {
  //      @Provides
  //      @Singleton
  //      AtmosphereBroadcaster atmosphereBroadcaster() {
  //        return mainConfiguration.getAtmosphereBroadcaster();
  //      }
  //    });
  //    modules.add(StreamModule.getInstance());
  //
  //    modules.add(new ValidationModule(validatorFactory));
  //    modules.add(new DelegateServiceModule());
  //    modules.add(new CapabilityModule());
  //    modules.add(MigrationModule.getInstance());
  //    modules.add(new WingsModule(mainConfiguration));
  //    modules.add(YamlModule.getInstance());
  //
  ////    CacheModule cacheModule = new CacheModule(mainConfiguration.getCacheConfig());
  ////    modules.add(cacheModule);
  ////    modules.add(YamlLiteModule.getInstance());
  //    //    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
  //
  //    modules.add(new ProviderModule() {
  //      @Provides
  //      @Singleton
  //      YamlHandlerFactory yamlHandlerFactory(Injector injector) {
  //        YamlHandlerFactory yamlHandlerFactory = new YamlHandlerFactory();
  //        injector.injectMembers(yamlHandlerFactory);
  //        return yamlHandlerFactory;
  //      }
  //    });
  //
  //    modules.add(new io.harness.delegate.app.DelegateServiceModule(delegateServiceConfig, mainConfiguration));
  //    //    modules.add(new io.harness.service.DelegateServiceModule());
  //
  ////    modules.add(new DelegateServiceModule());
  ////    modules.add(new CapabilityModule());
  //
  //    modules.add(new ProviderModule() {
  //      @Provides
  //      @Singleton
  //      AtmosphereBroadcaster atmosphereBroadcaster() {
  //        return delegateServiceConfig.getAtmosphereBroadcaster();
  //      }
  //    });
  //    modules.add(StreamModule.getInstance());
  //    modules.add(new ProviderModule() {
  //      @Provides
  //      @Singleton
  //      Set<Class<? extends KryoRegistrar>> kryoRegistrars() {
  //        return ImmutableSet.<Class<? extends KryoRegistrar>>builder()
  //            .addAll(DelegateServiceDriverRegistrars.kryoRegistrars)
  //            .build();
  //      }
  //      @Provides
  //      @Singleton
  //      Set<Class<? extends MorphiaRegistrar>> morphiaRegistrars() {
  //        return ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
  //            .addAll(DelegateServiceDriverRegistrars.morphiaRegistrars)
  //            .build();
  //      }
  //
  ////      @Provides
  ////      @Singleton
  ////      Set<Class<? extends TypeConverter>> morphiaConverters() {
  ////        return ImmutableSet.<Class<? extends TypeConverter>>builder().build();
  ////      }
  ////    });
  ////    modules.add(new io.harness.mongo.AbstractMongoModule() {
  ////      @Override
  ////      public io.harness.persistence.UserProvider userProvider() {
  ////        return new io.harness.persistence.NoopUserProvider();
  ////      }
  ////
  ////            @Provides
  ////            @Singleton
  ////            io.harness.mongo.MongoConfig mongoConfig() {
  ////              return io.harness.mongo.MongoConfig.builder().build();
  ////            }
  //    });
  //    //    modules.add(new WingsModule(mainConfiguration));
  //
  //    modules.add(new IndexMigratorModule());
  ////    modules.add(YamlModule.getInstance());
  //    modules.add(new ManagerQueueModule());
  //
  //    modules.add(new ManagerExecutorModule());
  //    modules.add(new TemplateModule());
  ////    modules.add(new MetricRegistryModule(metricRegistry));
  //    modules.add(new EventsModule(mainConfiguration));
  //    modules.add(new GraphQLModule());
  //    modules.add(new SSOModule());
  //    modules.add(new SignupModule());
  //    modules.add(new AuthModule());
  //    modules.add(new GcpMarketplaceIntegrationModule());
  //    modules.add(new SearchModule());
  //    modules.add(new ProviderModule() {
  //      @Provides
  //      public GrpcServerConfig getGrpcServerConfig() {
  //        return mainConfiguration.getGrpcServerConfig();
  //      }
  //    });
  //    modules.add(new GrpcServiceConfigurationModule(
  //        mainConfiguration.getGrpcServerConfig(), mainConfiguration.getPortal().getJwtNextGenManagerSecret()));
  //    modules.add(new ProviderModule() {
  //      @Provides
  //      @Singleton
  //      WorkersConfiguration workersConfig() {
  //        return mainConfiguration.getWorkers();
  //      }
  //
  //      @Provides
  //      @Singleton
  //      PublisherConfiguration publisherConfiguration() {
  //        return mainConfiguration.getPublisherConfiguration();
  //      }
  //    });
  //
  //    modules.add(new CommandLibraryServiceClientModule(mainConfiguration.getCommandLibraryServiceConfig()));
  //    modules.add(new AbstractCfModule() {
  //      @Override
  //      public CfClientConfig cfClientConfig() {
  //        return mainConfiguration.getCfClientConfig();
  //      }
  //
  //      @Override
  //      public CfMigrationConfig cfMigrationConfig() {
  //        return mainConfiguration.getCfMigrationConfig();
  //      }
  //
  //      @Override
  //      public FeatureFlagConfig featureFlagConfig() {
  //        return mainConfiguration.getFeatureFlagConfig();
  //      }
  //    });
  //
  //    Injector injector = Guice.createInjector(modules);
  //
  //    registerAtmosphereStreams(environment, injector);
  //    initializegRPCServer(injector);
  //
  //    registerResources(environment, injector);
  //
  //    registerManagedBeans(environment, injector);
  //
  //    registerJerseyProviders(environment);
  //
  //    registerCharsetResponseFilter(environment, injector);
  //
  //    // Authentication/Authorization filters
  //    registerAuthFilters(environment, injector);
  //
  //    registerHealthChecks(environment, injector);
  //
  ////    injector.getInstance(PrimaryVersionChangeScheduler.class).registerExecutors();
  //
  //    log.info("Started Delegate Service App");
  //  }

  @Override
  public void run(DelegateServiceConfig delegateServiceConfig, Environment environment) throws Exception {
    log.info("Starting Delegate Service App");
    ExecutorModule.getInstance().setExecutorService(ThreadPool.create(
        20, 1000, 500L, TimeUnit.MILLISECONDS, new ThreadFactoryBuilder().setNameFormat("main-app-pool-%d").build()));

    ObjectMapper mapper = bootstrap.getObjectMapper();
    configureObjectMapper(mapper);
    //    mapper.findAndRegisterModules();
    //    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    MainConfiguration configuration = mapper.readValue(
        new File("/Users/rktummala/IdeaProjects/portal/360-cg-manager/config1.yml"), MainConfiguration.class);

    List<Module> modules = new ArrayList<>();
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      Set<Class<? extends KryoRegistrar>> kryoRegistrars() {
        return ImmutableSet.<Class<? extends KryoRegistrar>>builder()
            .addAll(CurrentGenRegistrars.kryoRegistrars)
            .build();
      }
      @Provides
      @Singleton
      Set<Class<? extends MorphiaRegistrar>> morphiaRegistrars() {
        return ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
            .addAll(CurrentGenRegistrars.morphiaRegistrars)
            .build();
      }

      @Provides
      @Singleton
      Set<Class<? extends org.mongodb.morphia.converters.TypeConverter>> morphiaConverters() {
        return ImmutableSet.<Class<? extends TypeConverter>>builder()
            .addAll(CurrentGenRegistrars.morphiaConverters)
            .build();
      }

      @Provides
      @Singleton
      List<Class<? extends Converter<?, ?>>> springConverters() {
        return ImmutableList.<Class<? extends Converter<?, ?>>>builder()
            .addAll(CurrentGenRegistrars.springConverters)
            .build();
      }
    });

    modules.add(new AbstractMongoModule() {
      @Override
      public UserProvider userProvider() {
        return new ThreadLocalUserProvider();
      }
    });

    modules.add(new SpringPersistenceModule());

    ValidatorFactory validatorFactory = Validation.byDefaultProvider()
                                            .configure()
                                            .parameterNameProvider(new ReflectionParameterNameProvider())
                                            .buildValidatorFactory();

    CacheModule cacheModule = new CacheModule(configuration.getCacheConfig());
    modules.add(cacheModule);

    //    modules.add(new ProviderModule() {
    //      @Provides
    //      @Singleton
    //      YamlHandlerFactory yamlHandlerFactory(Injector injector) {
    //        YamlHandlerFactory yamlHandlerFactory = new YamlHandlerFactory();
    //        injector.injectMembers(yamlHandlerFactory);
    //        return yamlHandlerFactory;
    //      }
    //    });
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      AtmosphereBroadcaster atmosphereBroadcaster() {
        return configuration.getAtmosphereBroadcaster();
      }
    });
    modules.add(StreamModule.getInstance());

    //    modules.add(new AbstractModule() {
    //      @Override
    //      protected void configure() {
    //        bind(MetricRegistry.class).toInstance(metricRegistry);
    //      }
    //    });

    //    modules.add(MetricsInstrumentationModule.builder()
    //        .withMetricRegistry(metricRegistry)
    //        .withMatcher(not(new AbstractMatcher<TypeLiteral<?>>() {
    //          @Override
    //          public boolean matches(TypeLiteral<?> typeLiteral) {
    //            return typeLiteral.getRawType().isAnnotationPresent(Path.class);
    //          }
    //        }))
    //        .build());

    modules.add(new ValidationModule(validatorFactory));
    modules.add(new DelegateServiceModule());
    modules.add(new CapabilityModule());
    modules.add(MigrationModule.getInstance());
    modules.add(new WingsModule(configuration));
    modules.add(new CVNGClientModule(configuration.getCvngClientConfig()));
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      @Named("morphiaClasses")
      Map<Class, String> morphiaCustomCollectionNames() {
        return ImmutableMap.<Class, String>builder()
            .put(DelegateSyncTaskResponse.class, "delegateSyncTaskResponses")
            .put(DelegateAsyncTaskResponse.class, "delegateAsyncTaskResponses")
            .put(DelegateTaskProgressResponse.class, "delegateTaskProgressResponses")
            .build();
      }
    });

    modules.add(new IndexMigratorModule());
    modules.add(YamlModule.getInstance());
    modules.add(new ManagerQueueModule());

    modules.add(new ManagerExecutorModule());
    modules.add(new TemplateModule());
    //    modules.add(new MetricRegistryModule(metricRegistry));
    modules.add(new EventsModule(configuration));
    modules.add(new GraphQLModule());
    modules.add(new SSOModule());
    modules.add(new SignupModule());
    modules.add(new AuthModule());
    modules.add(new GcpMarketplaceIntegrationModule());
    modules.add(new SearchModule());
    modules.add(new ProviderModule() {
      @Provides
      public GrpcServerConfig getGrpcServerConfig() {
        return configuration.getGrpcServerConfig();
      }
    });
    modules.add(new GrpcServiceConfigurationModule(
        configuration.getGrpcServerConfig(), configuration.getPortal().getJwtNextGenManagerSecret()));
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      WorkersConfiguration workersConfig() {
        return configuration.getWorkers();
      }

      @Provides
      @Singleton
      PublisherConfiguration publisherConfiguration() {
        return configuration.getPublisherConfiguration();
      }
    });

    modules.add(new CommandLibraryServiceClientModule(configuration.getCommandLibraryServiceConfig()));
    modules.add(new AbstractCfModule() {
      @Override
      public CfClientConfig cfClientConfig() {
        return configuration.getCfClientConfig();
      }

      @Override
      public CfMigrationConfig cfMigrationConfig() {
        return configuration.getCfMigrationConfig();
      }

      @Override
      public FeatureFlagConfig featureFlagConfig() {
        return configuration.getFeatureFlagConfig();
      }
    });
    Injector injector = Guice.createInjector(modules);

    // Access all caches before coming out of maintenance
    injector.getInstance(new Key<Map<String, Cache<?, ?>>>() {});

    registerAtmosphereStreams(environment, injector);

    //    initializeFeatureFlags(configuration, injector);

    registerHealthChecks(environment, injector);

    //    registerStores(configuration, injector);

    registerResources(environment, injector);

    //    registerManagedBeans(configuration, environment, injector);

    //    registerQueueListeners(injector);

    //    scheduleJobs(injector, configuration);

    //    registerEventConsumers(injector);
    //
    //    registerObservers(injector);
    //
    //    registerInprocPerpetualTaskServiceClients(injector);
    //
    //    registerCronJobs(injector);
    //
    //    registerCorsFilter(configuration, environment);
    //
    //    registerAuditResponseFilter(environment, injector);

    registerJerseyProviders(environment, injector);

    registerCharsetResponseFilter(environment, injector);

    // Authentication/Authorization filters
    registerAuthFilters(configuration, environment, injector);

    //    registerCorrelationFilter(environment, injector);

    //    // Register collection iterators
    //    if (configuration.isEnableIterators()) {
    //      registerIterators(injector);
    //    }
    //    registerCVNGVerificationTaskIterator(injector);

    environment.lifecycle().addServerLifecycleListener(server -> {
      for (Connector connector : server.getConnectors()) {
        if (connector instanceof ServerConnector) {
          ServerConnector serverConnector = (ServerConnector) connector;
          if (serverConnector.getName().equalsIgnoreCase("application")) {
            configuration.setSslEnabled(
                serverConnector.getDefaultConnectionFactory().getProtocol().equalsIgnoreCase("ssl"));
            configuration.setApplicationPort(serverConnector.getLocalPort());
            return;
          }
        }
      }
    });

    //    harnessMetricRegistry = injector.getInstance(HarnessMetricRegistry.class);

    //    initMetrics();
    //
    //    initializeServiceSecretKeys(injector);
    //
    //    runMigrations(injector);

    String deployMode = configuration.getDeployMode().name();

    if (DeployMode.isOnPrem(deployMode)) {
      LicenseService licenseService = injector.getInstance(LicenseService.class);
      String encryptedLicenseInfoBase64String = System.getenv(LicenseService.LICENSE_INFO);
      log.info("Encrypted license info read from environment {}", encryptedLicenseInfoBase64String);
      if (isEmpty(encryptedLicenseInfoBase64String)) {
        log.error("No license info is provided");
      } else {
        try {
          log.info("Updating license info read from environment {}", encryptedLicenseInfoBase64String);
          licenseService.updateAccountLicenseForOnPrem(encryptedLicenseInfoBase64String);
          log.info("Updated license info read from environment {}", encryptedLicenseInfoBase64String);
        } catch (WingsException ex) {
          log.error("Error while updating license info", ex);
        }
      }
    }

    injector.getInstance(EventsModuleHelper.class).initialize();
    log.info("Initializing gRPC server...");
    ServiceManager serviceManager = injector.getInstance(ServiceManager.class).startAsync();
    serviceManager.awaitHealthy();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> serviceManager.stopAsync().awaitStopped()));

    //    registerDatadogPublisherIfEnabled(configuration);

    //    log.info("Leaving startup maintenance mode");
    //    MaintenanceController.resetForceMaintenance();
    Thread thread = new Thread(() -> {
      try {
        Thread.sleep(240000L);
        DelegateTaskServiceClassic delegateTaskServiceClassic = injector.getInstance(DelegateTaskServiceClassic.class);
        delegateTaskServiceClassic.executeTask(
            DelegateTask.builder()
                .accountId("kmpySmUISimoRrJL6NL73w")
                .data(TaskData.builder()
                          .async(true)
                          .taskType(GCB.name())
                          .parameters(new Object[] {GcbTaskParams.builder()
                                                        .gcpConfig(new GcpConfig())
                                                        .encryptedDataDetails(null)
                                                        .type(GcbTaskType.FETCH_TRIGGERS)
                                                        .build()})
                          .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                          .build())
                .build());
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
    thread.start();
    log.info("Starting Delegate Service App done");
  }

  private void registerResources(Environment environment, Injector injector) {
    Reflections reflections =
        new Reflections(AppResource.class.getPackage().getName(), DelegateTaskResource.class.getPackage().getName());

    final Set<Class<? extends Object>> resourceClasses = reflections.getTypesAnnotatedWith(Path.class);
    for (Class<?> resource : resourceClasses) {
      if (Resource.isAcceptable(resource)) {
        environment.jersey().register(injector.getInstance(resource));
      }
    }
  }

  private void registerJerseyProviders(Environment environment, Injector injector) {
    environment.jersey().register(injector.getInstance(KryoFeature.class));
    environment.jersey().register(EarlyEofExceptionMapper.class);
    environment.jersey().register(JsonProcessingExceptionMapper.class);
    environment.jersey().register(ConstraintViolationExceptionMapper.class);
    environment.jersey().register(WingsExceptionMapper.class);
    environment.jersey().register(GenericExceptionMapper.class);
    environment.jersey().register(MultiPartFeature.class);
  }

  private void registerManagedBeans(Environment environment, Injector injector) {
    environment.lifecycle().manage((Managed) injector.getInstance(WingsPersistence.class));
    environment.lifecycle().manage(injector.getInstance(MaintenanceController.class));
  }

  private void registerAuthFilters(MainConfiguration configuration, Environment environment, Injector injector) {
    if (configuration.isEnableAuth()) {
      environment.jersey().register(new AuthValueFactoryProvider.Binder<>(User.class));
      environment.jersey().register(injector.getInstance(LoginRateLimitFilter.class));
      environment.jersey().register(injector.getInstance(AuthRuleFilter.class));
      environment.jersey().register(injector.getInstance(AuthResponseFilter.class));
      environment.jersey().register(injector.getInstance(AuthenticationFilter.class));
    }
  }

  private void registerCharsetResponseFilter(Environment environment, Injector injector) {
    environment.jersey().register(injector.getInstance(CharsetResponseFilter.class));
  }

  private void registerHealthChecks(Environment environment, Injector injector) {
    final HealthService healthService = injector.getInstance(HealthService.class);
    environment.healthChecks().register("Delegate Service Manager", healthService);
    healthService.registerMonitor(injector.getInstance(HPersistence.class));
  }

  @Override
  public String getName() {
    return "Delegate Service Application";
  }

  //  @Override
  //  public void initialize(Bootstrap<MainConfiguration> bootstrap) {
  //    log.info("Initialize - start bootstrap ");
  //    // Enable variable substitution with environment variables
  //    bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
  //        bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));
  //    bootstrap.getObjectMapper().setSubtypeResolver(
  //        new JsonSubtypeResolver(bootstrap.getObjectMapper().getSubtypeResolver()));
  //    bootstrap.getObjectMapper().setConfig(
  //        bootstrap.getObjectMapper().getSerializationConfig().withView(JsonViews.Public.class));
  //  }

  private void registerAtmosphereStreams(Environment environment, Injector injector) {
    AtmosphereServlet atmosphereServlet = injector.getInstance(AtmosphereServlet.class);
    atmosphereServlet.framework().objectFactory(new GuiceObjectFactory(injector));
    injector.getInstance(BroadcasterFactory.class);
    injector.getInstance(MetaBroadcaster.class);
    ServletRegistration.Dynamic dynamic = environment.servlets().addServlet("StreamServlet", atmosphereServlet);
    dynamic.setAsyncSupported(true);
    dynamic.setLoadOnStartup(0);
    dynamic.addMapping("/stream/*");
  }

  private void initializegRPCServer(Injector injector) {
    log.info("Initializing gRPC Server on Delegate Service application");
    injector.getInstance(ServiceManager.class);
    ServiceManager serviceManager = injector.getInstance(ServiceManager.class).startAsync();
    serviceManager.awaitHealthy();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> serviceManager.stopAsync().awaitStopped()));
  }
}
