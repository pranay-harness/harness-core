package io.harness.app;

import static com.google.inject.matcher.Matchers.not;
import static io.harness.security.VerificationTokenGenerator.VERIFICATION_SERVICE_SECRET;
import static java.time.Duration.ofSeconds;
import static software.wings.app.LoggingInitializer.initializeLogging;
import static software.wings.beans.ServiceSecretKey.ServiceType.LEARNING_ENGINE;

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
import io.harness.health.VerificationServiceHealthCheck;
import io.harness.lock.AcquiredLock;
import io.harness.lock.ManageDistributedLockSvc;
import io.harness.lock.PersistentLocker;
import io.harness.managerclient.VerificationManagerClientModule;
import io.harness.mongo.MongoModule;
import io.harness.resources.LogVerificationResource;
import io.harness.scheduler.VerificationServiceExecutorJob;
import io.harness.security.VerificationServiceAuthenticationFilter;
import io.harness.service.intfc.LearningEngineService;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.model.Resource;
import org.hibernate.validator.parameternameprovider.ReflectionParameterNameProvider;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.pf4j.PluginManager;
import ru.vyarus.guice.validator.ValidationModule;
import software.wings.app.CharsetResponseFilter;
import software.wings.app.WingsApplication;
import software.wings.core.maintenance.MaintenanceController;
import software.wings.dl.WingsPersistence;
import software.wings.exception.ConstraintViolationExceptionMapper;
import software.wings.exception.GenericExceptionMapper;
import software.wings.exception.JsonProcessingExceptionMapper;
import software.wings.exception.WingsExceptionMapper;
import software.wings.jersey.JsonViews;
import software.wings.scheduler.QuartzScheduler;
import software.wings.service.intfc.MigrationService;
import software.wings.utils.JsonSubtypeResolver;

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
  private static final Logger logger = LoggerFactory.getLogger(VerificationServiceApplication.class);

  private static String APPLICATION_NAME = "Verification Service Application";

  private WingsPersistence wingsPersistence;
  private final MetricRegistry metricRegistry = new MetricRegistry();

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

    MongoModule databaseModule = new MongoModule(configuration.getMongoConnectionFactory());

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
        new VerificationServiceSechedulerModule(configuration),
        new VerificationManagerClientModule(configuration.getManagerUrl()));

    wingsPersistence = injector.getInstance(WingsPersistence.class);

    registerResources(environment, injector);

    registerCronJobs(injector);

    registerManagedBeans(environment, injector);

    registerJerseyProviders(environment);

    registerCharsetResponseFilter(environment, injector);

    // Authentication/Authorization filters
    registerAuthFilters(environment, injector);

    environment.healthChecks().register("Verification Service", new VerificationServiceHealthCheck());

    startPlugins(injector);

    initializeServiceSecretKeys(injector);

    runMigrations(injector);
    logger.info("Starting app done");
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

  private void startPlugins(Injector injector) {
    PluginManager pluginManager = injector.getInstance(PluginManager.class);
    pluginManager.loadPlugins();
    pluginManager.startPlugins();
  }

  private void registerCronJobs(Injector injector) {
    logger.info("Register cron jobs...");
    final QuartzScheduler jobScheduler =
        injector.getInstance(Key.get(QuartzScheduler.class, Names.named("JobScheduler")));

    PersistentLocker persistentLocker = injector.getInstance(Key.get(PersistentLocker.class));

    try (AcquiredLock acquiredLock = persistentLocker.waitToAcquireLock(
             WingsApplication.class, "Initialization", ofSeconds(5), ofSeconds(10))) {
      // If we do not get the lock, that's not critical - that's most likely because other managers took it
      // and they will initialize the jobs.
      if (acquiredLock != null) {
        VerificationServiceExecutorJob.addJob(jobScheduler);
      }
    }
  }

  private void initializeServiceSecretKeys(Injector injector) {
    injector.getInstance(LearningEngineService.class).initializeServiceSecretKeys();
    System.setProperty(VERIFICATION_SERVICE_SECRET,
        injector.getInstance(LearningEngineService.class).getServiceSecretKey(LEARNING_ENGINE));
  }

  private void runMigrations(Injector injector) {
    injector.getInstance(MigrationService.class).runMigrations();
  }
}
