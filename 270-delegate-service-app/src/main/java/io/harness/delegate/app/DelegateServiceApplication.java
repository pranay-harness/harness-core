package io.harness.delegate.app;

import static io.harness.annotations.dev.HarnessModule._420_DELEGATE_SERVICE;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.logging.LoggingInitializer.initializeLogging;

import io.harness.annotations.dev.TargetModule;
import io.harness.cache.CacheModule;
import io.harness.govern.ProviderModule;
import io.harness.grpc.client.GrpcClientConfig;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.AnnotationAwareJsonSubtypeResolver;
import io.harness.serializer.DelegateServiceDriverRegistrars;
import io.harness.serializer.JsonSubtypeResolver;
import io.harness.serializer.KryoRegistrar;
import io.harness.stream.AtmosphereBroadcaster;
import io.harness.stream.GuiceObjectFactory;
import io.harness.stream.StreamModule;
import io.harness.threading.ExecutorModule;
import io.harness.threading.ThreadPool;

import software.wings.app.InspectCommand;
import software.wings.app.MainConfiguration;
import software.wings.app.MainConfiguration.AssetsConfigurationMixin;
import software.wings.app.PortalConfig;
import software.wings.app.WingsModule;
import software.wings.app.YamlModule;
import software.wings.beans.template.Template;
import software.wings.jersey.JsonViews;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.dirkraft.dropwizard.fileassets.FileAssetsBundle;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ServiceManager;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.*;
import io.dropwizard.Application;
import io.dropwizard.bundles.assets.AssetsConfiguration;
import io.dropwizard.bundles.assets.ConfiguredAssetsBundle;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletRegistration;
import lombok.extern.slf4j.Slf4j;
import org.atmosphere.cpr.AtmosphereServlet;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.cpr.MetaBroadcaster;
import org.mongodb.morphia.converters.TypeConverter;

@Slf4j
@TargetModule(_420_DELEGATE_SERVICE)
public class DelegateServiceApplication extends Application<MainConfiguration> {
  private Bootstrap<MainConfiguration> bootstrap;
  public static void main(String... args) throws Exception {
    new DelegateServiceApplication().run(args);
  }

  @Override
  public void initialize(Bootstrap<MainConfiguration> bootstrap) {
    initializeLogging();
    log.info("bootstrapping ...");
    bootstrap.addCommand(new InspectCommand<>(this));

    // Enable variable substitution with environment variables
    bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
        bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));
    bootstrap.addBundle(new ConfiguredAssetsBundle("/static", "/", "index.html"));
    bootstrap.addBundle(new SwaggerBundle<MainConfiguration>() {
      @Override
      protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(MainConfiguration mainConfiguration) {
        return mainConfiguration.getSwaggerBundleConfiguration();
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

  @Override
  public void run(MainConfiguration mainConfiguration, Environment environment) throws Exception {
    ExecutorModule.getInstance().setExecutorService(ThreadPool.create(
        20, 1000, 500L, TimeUnit.MILLISECONDS, new ThreadFactoryBuilder().setNameFormat("main-app-pool-%d").build()));

    List<Module> modules = new ArrayList<>();
    //    MainConfiguration mainConfiguration = new MainConfiguration();
    //    PortalConfig portalConfig = new PortalConfig();
    //    mainConfiguration.setPortal(portalConfig);
    //    GrpcClientConfig grpcClientConfig = GrpcClientConfig.builder().build();
    //    mainConfiguration.setGrpcDelegateServiceClientConfig(grpcClientConfig);
    //    modules.add(new YamlModule());

    CacheModule cacheModule = new CacheModule(mainConfiguration.getCacheConfig());
    modules.add(cacheModule);

    //    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    ObjectMapper mapper = bootstrap.getObjectMapper();
    //    mapper.findAndRegisterModules();
    //    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    DelegateServiceConfig delegateServiceConfig =
        mapper.readValue(new File("/Users/rktummala/IdeaProjects/portal/270-delegate-service-app/config.yml"),
            DelegateServiceConfig.class);
    modules.add(new DelegateServiceModule(delegateServiceConfig));
    modules.add(new io.harness.service.DelegateServiceModule());
    modules.add(new WingsModule(mainConfiguration));

    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      AtmosphereBroadcaster atmosphereBroadcaster() {
        return delegateServiceConfig.getAtmosphereBroadcaster();
      }
    });
    modules.add(StreamModule.getInstance());
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      Set<Class<? extends KryoRegistrar>> kryoRegistrars() {
        return ImmutableSet.<Class<? extends KryoRegistrar>>builder()
            .addAll(DelegateServiceDriverRegistrars.kryoRegistrars)
            .build();
      }
      @Provides
      @Singleton
      Set<Class<? extends MorphiaRegistrar>> morphiaRegistrars() {
        return ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
            .addAll(DelegateServiceDriverRegistrars.morphiaRegistrars)
            .build();
      }

      @Provides
      @Singleton
      Set<Class<? extends TypeConverter>> morphiaConverters() {
        return ImmutableSet.<Class<? extends TypeConverter>>builder().build();
      }
    });
    modules.add(new io.harness.mongo.AbstractMongoModule() {
      @Override
      public io.harness.persistence.UserProvider userProvider() {
        return new io.harness.persistence.NoopUserProvider();
      }

      @Provides
      @Singleton
      io.harness.mongo.MongoConfig mongoConfig() {
        return io.harness.mongo.MongoConfig.builder().build();
      }
    });

    Injector injector = Guice.createInjector(modules);

    registerAtmosphereStreams(environment, injector);
    initializegRPCServer(injector);
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
