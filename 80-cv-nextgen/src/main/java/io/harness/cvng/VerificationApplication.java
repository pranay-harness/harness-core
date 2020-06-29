package io.harness.cvng;

import static com.google.inject.matcher.Matchers.not;
import static io.harness.logging.LoggingInitializer.initializeLogging;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static io.harness.security.ServiceTokenGenerator.VERIFICATION_SERVICE_SECRET;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;

import com.codahale.metrics.MetricRegistry;
import com.palominolabs.metrics.guice.MetricsInstrumentationModule;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import io.harness.cvng.client.VerificationManagerClientModule;
import io.harness.cvng.core.jobs.CVConfigDataCollectionHandler;
import io.harness.cvng.core.resources.DataCollectionResource;
import io.harness.cvng.core.services.api.VerificationServiceSecretManager;
import io.harness.cvng.core.services.entities.CVConfig;
import io.harness.cvng.core.services.entities.CVConfig.CVConfigKeys;
import io.harness.cvng.statemachine.jobs.AnalysisOrchestrationJob;
import io.harness.govern.ProviderModule;
import io.harness.health.HealthService;
import io.harness.iterator.PersistenceIterator;
import io.harness.maintenance.MaintenanceController;
import io.harness.metrics.HarnessMetricRegistry;
import io.harness.metrics.MetricRegistryModule;
import io.harness.mongo.MongoConfig;
import io.harness.mongo.MongoModule;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.server.model.Resource;
import org.hibernate.validator.parameternameprovider.ReflectionParameterNameProvider;
import org.reflections.Reflections;
import ru.vyarus.guice.validator.ValidationModule;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import javax.ws.rs.Path;

@Slf4j
public class VerificationApplication extends Application<VerificationConfiguration> {
  private static String APPLICATION_NAME = "Verification NextGen Application";
  private final MetricRegistry metricRegistry = new MetricRegistry();
  private HarnessMetricRegistry harnessMetricRegistry;

  public static void main(String[] args) throws Exception {
    new VerificationApplication().run(args);
  }

  @Override
  public String getName() {
    return APPLICATION_NAME;
  }

  @Override
  public void initialize(Bootstrap<VerificationConfiguration> bootstrap) {
    initializeLogging();
    logger.info("bootstrapping ...");
    // Enable variable substitution with environment variables
    bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
        bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));
    bootstrap.addBundle(new SwaggerBundle<VerificationConfiguration>() {
      @Override
      protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(
          VerificationConfiguration verificationServiceConfiguration) {
        return verificationServiceConfiguration.getSwaggerBundleConfiguration();
      }
    });
    bootstrap.setMetricRegistry(metricRegistry);
    logger.info("bootstrapping done.");
  }

  @Override
  public void run(VerificationConfiguration configuration, Environment environment) {
    logger.info("Starting app ...");
    MaintenanceController.forceMaintenance(true);
    ValidatorFactory validatorFactory = Validation.byDefaultProvider()
                                            .configure()
                                            .parameterNameProvider(new ReflectionParameterNameProvider())
                                            .buildValidatorFactory();

    List<Module> modules = new ArrayList<>();
    modules.add(MetricsInstrumentationModule.builder()
                    .withMetricRegistry(metricRegistry)
                    .withMatcher(not(new AbstractMatcher<TypeLiteral<?>>() {
                      @Override
                      public boolean matches(TypeLiteral<?> typeLiteral) {
                        return typeLiteral.getRawType().isAnnotationPresent(Path.class);
                      }
                    }))
                    .build());
    modules.add(new ValidationModule(validatorFactory));
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      MongoConfig mongoConfig() {
        return configuration.getMongoConnectionFactory();
      }
    });
    modules.addAll(new MongoModule().cumulativeDependencies());
    modules.add(new CVServiceModule());
    modules.add(new MetricRegistryModule(metricRegistry));
    modules.add(new VerificationManagerClientModule(configuration.getManagerUrl()));
    modules.add(new CVNextGenCommonsServiceModule());
    Injector injector = Guice.createInjector(modules);
    initializeServiceSecretKeys(injector);
    harnessMetricRegistry = injector.getInstance(HarnessMetricRegistry.class);
    registerAuthFilters(environment, injector);
    registerManagedBeans(environment, injector);
    registerResources(environment, injector);
    ScheduledThreadPoolExecutor serviceGuardExecutor =
        new ScheduledThreadPoolExecutor(15, new ThreadFactoryBuilder().setNameFormat("Iterator-Analysis").build());
    registerOrchestrationIterator(injector, serviceGuardExecutor, ofMinutes(1), 7, new AnalysisOrchestrationJob(),
        CVConfigKeys.analysisOrchestrationIteration);

    registerCVConfigIterator(injector);
    registerHealthChecks(environment, injector);
    logger.info("Leaving startup maintenance mode");
    MaintenanceController.forceMaintenance(false);

    logger.info("Starting app done");
  }

  private void registerOrchestrationIterator(Injector injector,
      ScheduledThreadPoolExecutor workflowVerificationExecutor, Duration interval, int maxAllowedThreads,
      Handler<CVConfig> handler, String fieldName) {
    injector.injectMembers(handler);
    PersistenceIterator analysisOrchestrationIterator =
        MongoPersistenceIterator.<CVConfig>builder()
            .mode(PersistenceIterator.ProcessMode.PUMP)
            .clazz(CVConfig.class)
            .fieldName(fieldName)
            .targetInterval(interval)
            .acceptableNoAlertDelay(ofSeconds(30))
            .executorService(workflowVerificationExecutor)
            .semaphore(new Semaphore(maxAllowedThreads))
            .handler(handler)
            .schedulingType(REGULAR)
            .filterExpander(query -> query.field(CVConfigKeys.createdAt).lessThanOrEq(Instant.now().toEpochMilli()))
            .redistribute(true)
            .build();
    injector.injectMembers(analysisOrchestrationIterator);
    workflowVerificationExecutor.scheduleAtFixedRate(
        () -> analysisOrchestrationIterator.process(), 0, 20, TimeUnit.SECONDS);
  }

  private void registerCVConfigIterator(Injector injector) {
    ScheduledThreadPoolExecutor dataCollectionExecutor = new ScheduledThreadPoolExecutor(
        5, new ThreadFactoryBuilder().setNameFormat("cv-config-data-collection-iterator").build());
    CVConfigDataCollectionHandler cvConfigDataCollectionHandler =
        injector.getInstance(CVConfigDataCollectionHandler.class);
    // TODO: setup alert if this goes above acceptable threshold.
    PersistenceIterator dataCollectionIterator =
        MongoPersistenceIterator.<CVConfig>builder()
            .mode(PersistenceIterator.ProcessMode.PUMP)
            .clazz(CVConfig.class)
            .fieldName(CVConfigKeys.dataCollectionTaskIteration)
            .targetInterval(ofMinutes(1))
            .acceptableNoAlertDelay(ofMinutes(1))
            .executorService(dataCollectionExecutor)
            .semaphore(new Semaphore(5))
            .handler(cvConfigDataCollectionHandler)
            .schedulingType(REGULAR)
            .filterExpander(query -> query.filter(CVConfigKeys.dataCollectionTaskId, null))
            .redistribute(true)
            .build();
    injector.injectMembers(dataCollectionIterator);
    dataCollectionExecutor.scheduleAtFixedRate(() -> dataCollectionIterator.process(), 0, 30, TimeUnit.SECONDS);
  }

  private void registerHealthChecks(Environment environment, Injector injector) {
    HealthService healthService = injector.getInstance(HealthService.class);
    environment.healthChecks().register("CV nextgen", healthService);
    healthService.registerMonitor(injector.getInstance(HPersistence.class));
  }

  private void registerAuthFilters(Environment environment, Injector injector) {
    environment.jersey().register(injector.getInstance(CVNGAuthenticationFilter.class));
  }

  private void initializeServiceSecretKeys(Injector injector) {
    injector.getInstance(VerificationServiceSecretManager.class).initializeServiceSecretKeys();
    VERIFICATION_SERVICE_SECRET.set(
        injector.getInstance(VerificationServiceSecretManager.class).getVerificationServiceSecretKey());
  }

  private void registerManagedBeans(Environment environment, Injector injector) {
    environment.lifecycle().manage(injector.getInstance(MaintenanceController.class));
  }

  private void registerResources(Environment environment, Injector injector) {
    Reflections reflections = new Reflections(DataCollectionResource.class.getPackage().getName());

    Set<Class<? extends Object>> resourceClasses = reflections.getTypesAnnotatedWith(Path.class);
    for (Class<?> resource : resourceClasses) {
      if (Resource.isAcceptable(resource)) {
        environment.jersey().register(injector.getInstance(resource));
      }
    }
  }
}
