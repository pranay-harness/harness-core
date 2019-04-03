package io.harness.app;

import static com.google.inject.matcher.Matchers.not;
import static io.harness.logging.LoggingInitializer.initializeLogging;
import static io.harness.security.VerificationTokenGenerator.VERIFICATION_SERVICE_SECRET;
import static java.time.Duration.ofSeconds;
import static software.wings.beans.ServiceSecretKey.ServiceType.LEARNING_ENGINE;
import static software.wings.common.VerificationConstants.DATA_COLLECTION_TASKS_PER_MINUTE;
import static software.wings.common.VerificationConstants.IGNORED_ERRORS_METRIC_LABELS;
import static software.wings.common.VerificationConstants.IGNORED_ERRORS_METRIC_NAME;
import static software.wings.common.VerificationConstants.LEARNING_ENGINE_TASK_QUEUED_TIME_IN_SECONDS;
import static software.wings.common.VerificationConstants.getDataAnalysisMetricHelpDocument;
import static software.wings.common.VerificationConstants.getDataCollectionMetricHelpDocument;
import static software.wings.common.VerificationConstants.getIgnoredErrorsMetricHelpDocument;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.name.Names;

import com.codahale.metrics.MetricRegistry;
import com.deftlabs.lock.mongo.DistributedLockSvc;
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
import io.harness.entities.VerificationMorphiaClasses;
import io.harness.event.model.EventsMorphiaClasses;
import io.harness.health.VerificationServiceHealthCheck;
import io.harness.jobs.VerificationJob;
import io.harness.jobs.VerificationMetricJob;
import io.harness.limits.LimitsMorphiaClasses;
import io.harness.lock.AcquiredLock;
import io.harness.lock.ManageDistributedLockSvc;
import io.harness.lock.PersistentLocker;
import io.harness.maintenance.MaintenanceController;
import io.harness.managerclient.VerificationManagerClientModule;
import io.harness.metrics.HarnessMetricRegistry;
import io.harness.metrics.MetricRegistryModule;
import io.harness.mongo.MongoModule;
import io.harness.mongo.PersistenceMorphiaClasses;
import io.harness.persistence.HPersistence;
import io.harness.resources.LogVerificationResource;
import io.harness.scheduler.PersistentScheduler;
import io.harness.scheduler.VerificationServiceExecutorService;
import io.harness.security.VerificationServiceAuthenticationFilter;
import io.harness.serializer.JsonSubtypeResolver;
import io.harness.service.intfc.LearningEngineService;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.model.Resource;
import org.hibernate.validator.parameternameprovider.ReflectionParameterNameProvider;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.guice.validator.ValidationModule;
import software.wings.app.CharsetResponseFilter;
import software.wings.app.WingsApplication;
import software.wings.beans.ManagerMorphiaClasses;
import software.wings.dl.WingsPersistence;
import software.wings.exception.ConstraintViolationExceptionMapper;
import software.wings.exception.GenericExceptionMapper;
import software.wings.exception.JsonProcessingExceptionMapper;
import software.wings.exception.WingsExceptionMapper;
import software.wings.jersey.JsonViews;
import software.wings.security.ThreadLocalUserProvider;

import java.util.Set;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import javax.ws.rs.Path;

/**
 * The main application - entry point for the entire verification service.
 *
 * @author Raghu
 */
public class VerificationServiceApplication extends Application<VerificationServiceConfiguration> {
  public static final Set<Class> morphiaClasses = ImmutableSet.<Class>builder()
                                                      .addAll(ManagerMorphiaClasses.classes)
                                                      .addAll(ManagerMorphiaClasses.dependentClasses)
                                                      .addAll(EventsMorphiaClasses.classes)
                                                      .addAll(PersistenceMorphiaClasses.classes)
                                                      .addAll(LimitsMorphiaClasses.classes)
                                                      .addAll(VerificationMorphiaClasses.classes)
                                                      .build();
  // pool interval at which the job will schedule. But here in verificationJob it will schedule at POLL_INTERVAL / 2
  private static final Logger logger = LoggerFactory.getLogger(VerificationServiceApplication.class);
  private static String APPLICATION_NAME = "Verification Service Application";
  private final MetricRegistry metricRegistry = new MetricRegistry();
  public HarnessMetricRegistry harnessMetricRegistry;
  private WingsPersistence wingsPersistence;

  /**
   * The entry point of application.
   *
   * @param args the input arguments
   * @throws Exception the exception
   */
  public static void main(String[] args) throws Exception {
    new VerificationServiceApplication().run(args);
  }

  @Override
  public String getName() {
    return APPLICATION_NAME;
  }

  @Override
  public void initialize(Bootstrap<VerificationServiceConfiguration> bootstrap) {
    initializeLogging();
    logger.info("bootstrapping ...");
    // Enable variable substitution with environment variables
    bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
        bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));
    bootstrap.addBundle(new ConfiguredAssetsBundle("/static", "/", "index.html"));
    bootstrap.addBundle(new SwaggerBundle<VerificationServiceConfiguration>() {
      @Override
      protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(
          VerificationServiceConfiguration verificationServiceConfiguration) {
        return verificationServiceConfiguration.getSwaggerBundleConfiguration();
      }
    });
    bootstrap.addBundle(new FileAssetsBundle("/.well-known"));
    //    bootstrap.getObjectMapper().addMixIn(AssetsConfiguration.class, AssetsConfigurationMixin.class);
    bootstrap.getObjectMapper().setSubtypeResolver(
        new JsonSubtypeResolver(bootstrap.getObjectMapper().getSubtypeResolver()));
    bootstrap.getObjectMapper().setConfig(
        bootstrap.getObjectMapper().getSerializationConfig().withView(JsonViews.Public.class));
    bootstrap.setMetricRegistry(metricRegistry);

    logger.info("bootstrapping done.");
  }

  @Override
  public void run(final VerificationServiceConfiguration configuration, Environment environment) {
    logger.info("Starting app ...");

    logger.info("Entering startup maintenance mode");
    MaintenanceController.forceMaintenance(true);

    MongoModule databaseModule = new MongoModule(configuration.getMongoConnectionFactory(), morphiaClasses);

    ValidatorFactory validatorFactory = Validation.byDefaultProvider()
                                            .configure()
                                            .parameterNameProvider(new ReflectionParameterNameProvider())
                                            .buildValidatorFactory();

    Injector injector = Guice.createInjector(MetricsInstrumentationModule.builder()
                                                 .withMetricRegistry(metricRegistry)
                                                 .withMatcher(not(new AbstractMatcher<TypeLiteral<?>>() {
                                                   @Override
                                                   public boolean matches(TypeLiteral<?> typeLiteral) {
                                                     return typeLiteral.getRawType().isAnnotationPresent(Path.class);
                                                   }
                                                 }))
                                                 .build(),
        new ValidationModule(validatorFactory), databaseModule, new VerificationServiceModule(configuration),
        new VerificationServiceSchedulerModule(configuration),
        new VerificationManagerClientModule(configuration.getManagerUrl()), new MetricRegistryModule(metricRegistry));

    wingsPersistence = injector.getInstance(WingsPersistence.class);

    harnessMetricRegistry = injector.getInstance(HarnessMetricRegistry.class);

    initMetrics();

    registerStores(configuration, injector);

    registerResources(environment, injector);

    registerManagedBeans(environment, injector);

    initializeServiceSecretKeys(injector);

    registerJerseyProviders(environment);

    registerCharsetResponseFilter(environment, injector);

    // Authentication/Authorization filters
    registerAuthFilters(environment, injector);

    environment.healthChecks().register("Verification Service", new VerificationServiceHealthCheck());

    registerCronJobs(injector);

    initializeServiceTaskPoll(injector);

    logger.info("Starting app done");
  }

  private void initMetrics() {
    harnessMetricRegistry.registerGaugeMetric(
        LEARNING_ENGINE_TASK_QUEUED_TIME_IN_SECONDS, null, getDataAnalysisMetricHelpDocument());
    harnessMetricRegistry.registerGaugeMetric(
        DATA_COLLECTION_TASKS_PER_MINUTE, null, getDataCollectionMetricHelpDocument());
    harnessMetricRegistry.registerGaugeMetric(
        IGNORED_ERRORS_METRIC_NAME, IGNORED_ERRORS_METRIC_LABELS, getIgnoredErrorsMetricHelpDocument());
  }

  private void registerStores(VerificationServiceConfiguration configuration, Injector injector) {
    final HPersistence persistence = injector.getInstance(HPersistence.class);
    persistence.registerUserProvider(new ThreadLocalUserProvider());
  }

  private void registerResources(Environment environment, Injector injector) {
    Reflections reflections = new Reflections(LogVerificationResource.class.getPackage().getName());

    Set<Class<? extends Object>> resourceClasses = reflections.getTypesAnnotatedWith(Path.class);
    for (Class<?> resource : resourceClasses) {
      if (Resource.isAcceptable(resource)) {
        environment.jersey().register(injector.getInstance(resource));
      }
    }
  }

  private void registerManagedBeans(Environment environment, Injector injector) {
    environment.lifecycle().manage((Managed) injector.getInstance(WingsPersistence.class));
    environment.lifecycle().manage(new ManageDistributedLockSvc(injector.getInstance(DistributedLockSvc.class)));
  }

  private void registerJerseyProviders(Environment environment) {
    environment.jersey().register(EarlyEofExceptionMapper.class);
    environment.jersey().register(JsonProcessingExceptionMapper.class);
    environment.jersey().register(ConstraintViolationExceptionMapper.class);
    environment.jersey().register(WingsExceptionMapper.class);
    environment.jersey().register(GenericExceptionMapper.class);
    environment.jersey().register(MultiPartFeature.class);
  }

  private void registerAuthFilters(Environment environment, Injector injector) {
    environment.jersey().register(injector.getInstance(VerificationServiceAuthenticationFilter.class));
  }

  private void registerCharsetResponseFilter(Environment environment, Injector injector) {
    environment.jersey().register(injector.getInstance(CharsetResponseFilter.class));
  }

  private void registerCronJobs(Injector injector) {
    logger.info("Register cron jobs...");
    final PersistentScheduler jobScheduler =
        injector.getInstance(Key.get(PersistentScheduler.class, Names.named("BackgroundJobScheduler")));

    PersistentLocker persistentLocker = injector.getInstance(Key.get(PersistentLocker.class));

    try (AcquiredLock acquiredLock = persistentLocker.waitToAcquireLock(
             WingsApplication.class, "Initialization", ofSeconds(5), ofSeconds(10))) {
      // If we do not get the lock, that's not critical - that's most likely because other managers took it
      // and they will initialize the jobs.
      if (acquiredLock != null) {
        VerificationServiceExecutorService.addJob(jobScheduler);
        VerificationJob.addJob(jobScheduler);
        VerificationMetricJob.addJob(jobScheduler);
      }
    }
  }

  private void initializeServiceSecretKeys(Injector injector) {
    injector.getInstance(LearningEngineService.class).initializeServiceSecretKeys();
    VERIFICATION_SERVICE_SECRET.set(
        injector.getInstance(LearningEngineService.class).getServiceSecretKey(LEARNING_ENGINE));
  }

  private void initializeServiceTaskPoll(Injector injector) {
    injector.getInstance(VerificationServiceExecutorService.class).scheduleTaskPoll();
  }
}
