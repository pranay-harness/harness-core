package io.harness.commandlibrary.server.app;

import static com.google.inject.matcher.Matchers.not;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.logging.LoggingInitializer.initializeLogging;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;

import com.codahale.metrics.MetricRegistry;
import com.github.dirkraft.dropwizard.fileassets.FileAssetsBundle;
import com.palominolabs.metrics.guice.MetricsInstrumentationModule;
import io.dropwizard.Application;
import io.dropwizard.bundles.assets.ConfiguredAssetsBundle;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.jersey.errors.EarlyEofExceptionMapper;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import io.harness.commandlibrary.server.resources.CommandStoreResource;
import io.harness.commandlibrary.server.security.CommandLibraryServerAuthenticationFilter;
import io.harness.govern.ProviderModule;
import io.harness.health.HealthService;
import io.harness.maintenance.MaintenanceController;
import io.harness.metrics.HarnessMetricRegistry;
import io.harness.metrics.MetricRegistryModule;
import io.harness.mongo.MongoConfig;
import io.harness.mongo.MongoModule;
import io.harness.persistence.HPersistence;
import io.harness.serializer.JsonSubtypeResolver;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.model.Resource;
import org.hibernate.validator.parameternameprovider.ReflectionParameterNameProvider;
import org.reflections.Reflections;
import ru.vyarus.guice.validator.ValidationModule;
import software.wings.app.CharsetResponseFilter;
import software.wings.dl.WingsPersistence;
import software.wings.exception.ConstraintViolationExceptionMapper;
import software.wings.exception.GenericExceptionMapper;
import software.wings.exception.JsonProcessingExceptionMapper;
import software.wings.exception.WingsExceptionMapper;
import software.wings.jersey.JsonViews;
import software.wings.security.ThreadLocalUserProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import javax.ws.rs.Path;

@Slf4j
public class CommandLibraryServerApplication extends Application<CommandLibraryServerConfig> {
  private static final String APPLICATION_NAME = "Command Library Service Application";
  private final MetricRegistry metricRegistry = new MetricRegistry();
  private HarnessMetricRegistry harnessMetricRegistry;

  public static void main(String[] args) throws Exception {
    new CommandLibraryServerApplication().run(args);
  }

  @Override
  public String getName() {
    return APPLICATION_NAME;
  }

  @Override
  public void initialize(Bootstrap<CommandLibraryServerConfig> bootstrap) {
    initializeLogging();
    logger.info("Bootstrapping Command Library Service ...");
    // Enable variable substitution with environment variables
    bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
        bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));
    bootstrap.addBundle(new ConfiguredAssetsBundle("/static", "/", "index.html"));
    bootstrap.addBundle(new SwaggerBundle<CommandLibraryServerConfig>() {
      @Override
      protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(
          CommandLibraryServerConfig verificationServiceConfiguration) {
        return verificationServiceConfiguration.getSwaggerBundleConfiguration();
      }
    });
    bootstrap.addBundle(new FileAssetsBundle("/.well-known"));
    bootstrap.getObjectMapper().setSubtypeResolver(
        new JsonSubtypeResolver(bootstrap.getObjectMapper().getSubtypeResolver()));
    bootstrap.getObjectMapper().setConfig(
        bootstrap.getObjectMapper().getSerializationConfig().withView(JsonViews.Public.class));
    bootstrap.setMetricRegistry(metricRegistry);

    logger.info("Bootstrapping done.");
  }

  @Override
  public void run(CommandLibraryServerConfig configuration, Environment environment) throws Exception {
    logger.info("Starting Command library Servicee ...");

    logger.info("Entering startup maintenance mode");
    MaintenanceController.forceMaintenance(true);

    final List<Module> modules = new ArrayList<>();
    modules.add(MetricsInstrumentationModule.builder()
                    .withMetricRegistry(metricRegistry)
                    .withMatcher(not(new AbstractMatcher<TypeLiteral<?>>() {
                      @Override
                      public boolean matches(TypeLiteral<?> typeLiteral) {
                        return typeLiteral.getRawType().isAnnotationPresent(Path.class);
                      }
                    }))
                    .build());
    final ValidatorFactory validatorFactory = Validation.byDefaultProvider()
                                                  .configure()
                                                  .parameterNameProvider(new ReflectionParameterNameProvider())
                                                  .buildValidatorFactory();

    modules.add(new ValidationModule(validatorFactory));

    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      MongoConfig mongoConfig() {
        return configuration.getMongoConnectionFactory();
      }
    });
    modules.addAll(new MongoModule().cumulativeDependencies());

    modules.add(new CommandLibraryServerModule(configuration));
    modules.add(new MetricRegistryModule(metricRegistry));

    Injector injector = Guice.createInjector(modules);

    injector.getInstance(WingsPersistence.class);

    harnessMetricRegistry = injector.getInstance(HarnessMetricRegistry.class);

    initMetrics();

    registerStores(configuration, injector);

    registerResources(environment, injector);

    registerManagedBeans(environment, injector);

    registerJerseyProviders(environment);

    registerCharsetResponseFilter(environment, injector);

    // Authentication/Authorization filters
    registerAuthFilters(environment, injector);

    registerHealthChecks(environment, injector);

    logger.info("Leaving startup maintenance mode");
    MaintenanceController.resetForceMaintenance();

    logger.info("Starting app done");
  }

  private void initMetrics() {
    registerGaugeMetric("CLS_UPLOAD_COUNT", null);
    registerGaugeMetric("CLS_UPLOAD_LATENCY", null);
  }

  private void registerGaugeMetric(String metricName, String[] labels) {
    final String description = "This metric is used to track the Command library service analytics data";
    harnessMetricRegistry.registerGaugeMetric(metricName, labels, description);
    String env = System.getenv("ENV");
    if (isNotEmpty(env)) {
      env = env.replaceAll("-", "_").toLowerCase();
      harnessMetricRegistry.registerGaugeMetric(env + "_" + metricName, labels, description);
    }
  }

  private void registerStores(CommandLibraryServerConfig configuration, Injector injector) {
    final HPersistence persistence = injector.getInstance(HPersistence.class);
    persistence.registerUserProvider(new ThreadLocalUserProvider());
  }

  private void registerResources(Environment environment, Injector injector) {
    Reflections reflections = new Reflections(CommandStoreResource.class.getPackage().getName());

    final Set<Class<? extends Object>> resourceClasses = reflections.getTypesAnnotatedWith(Path.class);
    for (Class<?> resource : resourceClasses) {
      if (Resource.isAcceptable(resource)) {
        environment.jersey().register(injector.getInstance(resource));
      }
    }
  }
  private void registerJerseyProviders(Environment environment) {
    environment.jersey().register(EarlyEofExceptionMapper.class);
    environment.jersey().register(ConstraintViolationExceptionMapper.class);
    environment.jersey().register(WingsExceptionMapper.class);
    environment.jersey().register(JsonProcessingExceptionMapper.class);
    environment.jersey().register(MultiPartFeature.class);
    environment.jersey().register(GenericExceptionMapper.class);
  }

  private void registerManagedBeans(Environment environment, Injector injector) {
    environment.lifecycle().manage((Managed) injector.getInstance(WingsPersistence.class));
    environment.lifecycle().manage(injector.getInstance(MaintenanceController.class));
  }

  private void registerAuthFilters(Environment environment, Injector injector) {
    environment.jersey().register(injector.getInstance(CommandLibraryServerAuthenticationFilter.class));
  }

  private void registerCharsetResponseFilter(Environment environment, Injector injector) {
    environment.jersey().register(injector.getInstance(CharsetResponseFilter.class));
  }

  private void registerHealthChecks(Environment environment, Injector injector) {
    final HealthService healthService = injector.getInstance(HealthService.class);
    environment.healthChecks().register("Command Library Service", healthService);
    healthService.registerMonitor(injector.getInstance(HPersistence.class));
  }
}
