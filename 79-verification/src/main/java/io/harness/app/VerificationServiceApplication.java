package io.harness.app;

import static com.google.inject.matcher.Matchers.not;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.logging.LoggingInitializer.initializeLogging;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static io.harness.security.ServiceTokenGenerator.VERIFICATION_SERVICE_SECRET;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;
import static software.wings.beans.alert.Alert.AlertKeys;
import static software.wings.common.VerificationConstants.CV_TASK_CRON_POLL_INTERVAL_SEC;
import static software.wings.common.VerificationConstants.DATA_COLLECTION_TASKS_PER_MINUTE;
import static software.wings.common.VerificationConstants.IGNORED_ERRORS_METRIC_LABELS;
import static software.wings.common.VerificationConstants.IGNORED_ERRORS_METRIC_NAME;
import static software.wings.common.VerificationConstants.getDataAnalysisMetricHelpDocument;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
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
import io.harness.beans.ExecutionStatus;
import io.harness.govern.ProviderModule;
import io.harness.health.HealthService;
import io.harness.iterator.PersistenceIterator;
import io.harness.iterator.PersistenceIterator.ProcessMode;
import io.harness.jobs.sg247.collection.ServiceGuardDataCollectionJob;
import io.harness.jobs.sg247.logs.ServiceGuardCleanUpAlertsJob;
import io.harness.jobs.sg247.logs.ServiceGuardLogAnalysisJob;
import io.harness.jobs.workflow.WorkflowCVTaskCreationHandler;
import io.harness.jobs.workflow.collection.CVDataCollectionJob;
import io.harness.jobs.workflow.logs.WorkflowFeedbackAnalysisJob;
import io.harness.jobs.workflow.logs.WorkflowLogAnalysisJob;
import io.harness.jobs.workflow.logs.WorkflowLogClusterJob;
import io.harness.jobs.workflow.timeseries.WorkflowTimeSeriesAnalysisJob;
import io.harness.maintenance.MaintenanceController;
import io.harness.managerclient.VerificationManagerClientModule;
import io.harness.metrics.HarnessMetricRegistry;
import io.harness.metrics.MetricRegistryModule;
import io.harness.mongo.MongoConfig;
import io.harness.mongo.MongoModule;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.persistence.HPersistence;
import io.harness.resources.LogVerificationResource;
import io.harness.scheduler.ServiceGuardAccountPoller;
import io.harness.scheduler.WorkflowVerificationTaskPoller;
import io.harness.security.VerificationServiceAuthenticationFilter;
import io.harness.serializer.JsonSubtypeResolver;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.model.Resource;
import org.hibernate.validator.parameternameprovider.ReflectionParameterNameProvider;
import org.reflections.Reflections;
import ru.vyarus.guice.validator.ValidationModule;
import software.wings.app.CharsetResponseFilter;
import software.wings.beans.Account;
import software.wings.beans.Account.AccountKeys;
import software.wings.beans.AccountStatus;
import software.wings.beans.AccountType;
import software.wings.beans.LicenseInfo.LicenseInfoKeys;
import software.wings.beans.alert.Alert;
import software.wings.beans.alert.AlertType;
import software.wings.common.VerificationConstants;
import software.wings.dl.WingsPersistence;
import software.wings.exception.ConstraintViolationExceptionMapper;
import software.wings.exception.GenericExceptionMapper;
import software.wings.exception.JsonProcessingExceptionMapper;
import software.wings.exception.WingsExceptionMapper;
import software.wings.jersey.JsonViews;
import software.wings.security.ThreadLocalUserProvider;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.AnalysisContext.AnalysisContextKeys;
import software.wings.service.impl.analysis.MLAnalysisType;
import software.wings.service.intfc.VerificationService;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import javax.ws.rs.Path;

/**
 * The main application - entry point for the entire verification service.
 *
 * @author Raghu
 */
@Slf4j
public class VerificationServiceApplication extends Application<VerificationServiceConfiguration> {
  // pool interval at which the job will schedule. But here in verificationJob it will schedule at POLL_INTERVAL / 2

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
  public void run(VerificationServiceConfiguration configuration, Environment environment) {
    logger.info("Starting app ...");

    logger.info("Entering startup maintenance mode");
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
    modules.add(new VerificationServiceModule(configuration));
    modules.add(new VerificationServiceSchedulerModule(configuration));
    modules.add(new VerificationManagerClientModule(configuration.getManagerUrl()));
    modules.add(new MetricRegistryModule(metricRegistry));

    Injector injector = Guice.createInjector(modules);

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

    registerHealthChecks(environment, injector);

    registerWorkflowIterators(injector);

    registerServiceGuardIterators(injector);

    initializeServiceTaskPoll(injector);

    logger.info("Leaving startup maintenance mode");
    MaintenanceController.resetForceMaintenance();

    logger.info("Starting app done");
  }

  private void registerHealthChecks(Environment environment, Injector injector) {
    HealthService healthService = injector.getInstance(HealthService.class);
    environment.healthChecks().register("Verification Service", healthService);
    healthService.registerMonitor(injector.getInstance(HPersistence.class));
  }

  private void initMetrics() {
    VerificationConstants.LEARNING_ENGINE_TASKS_METRIC_LIST.forEach(
        metricName -> registerGaugeMetric(metricName, null));
    registerGaugeMetric(DATA_COLLECTION_TASKS_PER_MINUTE, null);
    registerGaugeMetric(IGNORED_ERRORS_METRIC_NAME, IGNORED_ERRORS_METRIC_LABELS);
  }

  private void registerGaugeMetric(String metricName, String[] labels) {
    harnessMetricRegistry.registerGaugeMetric(metricName, labels, getDataAnalysisMetricHelpDocument());
    String env = System.getenv("ENV");
    if (isNotEmpty(env)) {
      env = env.replaceAll("-", "_").toLowerCase();
      harnessMetricRegistry.registerGaugeMetric(env + "_" + metricName, labels, getDataAnalysisMetricHelpDocument());
    }
  }

  private void registerStores(VerificationServiceConfiguration configuration, Injector injector) {
    HPersistence persistence = injector.getInstance(HPersistence.class);
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
    environment.lifecycle().manage(injector.getInstance(MaintenanceController.class));
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

  private void registerWorkflowIterators(Injector injector) {
    ScheduledThreadPoolExecutor workflowVerificationExecutor = new ScheduledThreadPoolExecutor(
        5, new ThreadFactoryBuilder().setNameFormat("Iterator-workflow-verification").build());
    registerWorkflowIterator(injector, workflowVerificationExecutor, new WorkflowTimeSeriesAnalysisJob(),
        AnalysisContextKeys.timeSeriesAnalysisIteration, MLAnalysisType.TIME_SERIES, ofMinutes(1), 4);
    registerWorkflowIterator(injector, workflowVerificationExecutor, new WorkflowLogAnalysisJob(),
        AnalysisContextKeys.logAnalysisIteration, MLAnalysisType.LOG_ML, ofSeconds(30), 4);
    registerWorkflowIterator(injector, workflowVerificationExecutor, new WorkflowLogClusterJob(),
        AnalysisContextKeys.logClusterIteration, MLAnalysisType.LOG_ML, ofSeconds(30), 4);
    registerWorkflowIterator(injector, workflowVerificationExecutor, new WorkflowFeedbackAnalysisJob(),
        AnalysisContextKeys.feedbackIteration, MLAnalysisType.LOG_ML, ofSeconds(30), 4);
    registerCreateCVTaskIterator(injector, workflowVerificationExecutor, ofSeconds(30), 4);
  }

  private void registerWorkflowIterator(Injector injector, ScheduledThreadPoolExecutor workflowVerificationExecutor,
      Handler<AnalysisContext> handler, String iteratorFieldName, MLAnalysisType analysisType, Duration interval,
      int maxAllowedThreads) {
    injector.injectMembers(handler);
    PersistenceIterator dataCollectionIterator =
        MongoPersistenceIterator.<AnalysisContext>builder()
            .mode(ProcessMode.PUMP)
            .clazz(AnalysisContext.class)
            .fieldName(iteratorFieldName)
            .targetInterval(interval)
            .acceptableNoAlertDelay(ofSeconds(30))
            .executorService(workflowVerificationExecutor)
            .semaphore(new Semaphore(maxAllowedThreads))
            .handler(handler)
            .schedulingType(REGULAR)
            .filterExpander(query
                -> query.filter(AnalysisContextKeys.analysisType, analysisType)
                       .field(AnalysisContextKeys.executionStatus)
                       .in(Lists.newArrayList(ExecutionStatus.QUEUED, ExecutionStatus.RUNNING)))
            .redistribute(true)
            .build();
    injector.injectMembers(dataCollectionIterator);
    workflowVerificationExecutor.scheduleAtFixedRate(() -> dataCollectionIterator.process(), 0, 5, TimeUnit.SECONDS);
  }

  private void registerAlertsCleanupIterator(Injector injector,
      ScheduledThreadPoolExecutor workflowVerificationExecutor, Duration interval, int maxAllowedThreads) {
    Handler<Alert> handler = new ServiceGuardCleanUpAlertsJob();
    injector.injectMembers(handler);
    PersistenceIterator alertsCleanupIterator =
        MongoPersistenceIterator.<Alert>builder()
            .mode(ProcessMode.PUMP)
            .clazz(Alert.class)
            .fieldName(AlertKeys.cvCleanUpIteration)
            .targetInterval(interval)
            .acceptableNoAlertDelay(ofSeconds(30))
            .executorService(workflowVerificationExecutor)
            .semaphore(new Semaphore(maxAllowedThreads))
            .handler(handler)
            .schedulingType(REGULAR)
            .filterExpander(query
                -> query.filter(AlertKeys.type, AlertType.CONTINUOUS_VERIFICATION_ALERT)
                       .field(AlertKeys.createdAt)
                       .lessThanOrEq(Instant.now().minus(6, ChronoUnit.HOURS).toEpochMilli()))
            .redistribute(true)
            .build();
    injector.injectMembers(alertsCleanupIterator);
    workflowVerificationExecutor.scheduleAtFixedRate(() -> alertsCleanupIterator.process(), 0, 10, TimeUnit.MINUTES);
  }

  private void registerCreateCVTaskIterator(Injector injector, ScheduledThreadPoolExecutor workflowVerificationExecutor,
      Duration interval, int maxAllowedThreads) {
    Handler<AnalysisContext> handler = new WorkflowCVTaskCreationHandler();
    injector.injectMembers(handler);
    PersistenceIterator dataCollectionIterator =
        MongoPersistenceIterator.<AnalysisContext>builder()
            .mode(ProcessMode.PUMP)
            .clazz(AnalysisContext.class)
            .fieldName(AnalysisContextKeys.cvTaskCreationIteration)
            .targetInterval(interval)
            .acceptableNoAlertDelay(ofSeconds(30))
            .executorService(workflowVerificationExecutor)
            .semaphore(new Semaphore(maxAllowedThreads))
            .handler(handler)
            .schedulingType(REGULAR)
            .filterExpander(query -> query.filter(AnalysisContextKeys.cvTasksCreated, false))
            .redistribute(true)
            .build();
    injector.injectMembers(dataCollectionIterator);
    workflowVerificationExecutor.scheduleAtFixedRate(() -> dataCollectionIterator.process(), 0, 5, TimeUnit.SECONDS);
  }

  private void registerServiceGuardIterators(Injector injector) {
    ScheduledThreadPoolExecutor serviceGuardExecutor =
        new ScheduledThreadPoolExecutor(15, new ThreadFactoryBuilder().setNameFormat("Iterator-ServiceGuard").build());
    registerIterator(injector, serviceGuardExecutor, new ServiceGuardDataCollectionJob(),
        AccountKeys.serviceGuardDataCollectionIteration, ofMinutes(1), 7);
    registerIterator(injector, serviceGuardExecutor, new ServiceGuardLogAnalysisJob(),
        AccountKeys.serviceGuardDataAnalysisIteration, ofMinutes(1), 7);
    registerIterator(injector, serviceGuardExecutor, new CVDataCollectionJob(),
        AccountKeys.workflowDataCollectionIteration, ofSeconds(CV_TASK_CRON_POLL_INTERVAL_SEC), 7);
    registerAlertsCleanupIterator(injector, serviceGuardExecutor, ofMinutes(2), 7);
  }

  private void registerIterator(Injector injector, ScheduledThreadPoolExecutor serviceGuardExecutor,
      Handler<Account> handler, String iteratorFieldName, Duration interval, int maxAllowedThreads) {
    injector.injectMembers(handler);
    PersistenceIterator dataCollectionIterator =
        MongoPersistenceIterator.<Account>builder()
            .mode(ProcessMode.PUMP)
            .clazz(Account.class)
            .fieldName(iteratorFieldName)
            .targetInterval(interval)
            .acceptableNoAlertDelay(ofSeconds(30))
            .executorService(serviceGuardExecutor)
            .semaphore(new Semaphore(maxAllowedThreads))
            .handler(handler)
            .schedulingType(REGULAR)
            .filterExpander(query
                -> query.or(query.criteria(AccountKeys.licenseInfo).doesNotExist(),
                    query.and(query.criteria(AccountKeys.licenseInfo + "." + LicenseInfoKeys.accountStatus)
                                  .equal(AccountStatus.ACTIVE)),
                    query.criteria(AccountKeys.licenseInfo + "." + LicenseInfoKeys.accountType)
                        .in(Sets.newHashSet(AccountType.TRIAL, AccountType.PAID))))
            .redistribute(true)
            .build();
    injector.injectMembers(dataCollectionIterator);
    serviceGuardExecutor.scheduleAtFixedRate(() -> dataCollectionIterator.process(), 0, 10, TimeUnit.SECONDS);
  }

  private void initializeServiceSecretKeys(Injector injector) {
    injector.getInstance(VerificationService.class).initializeServiceSecretKeys();
    VERIFICATION_SERVICE_SECRET.set(injector.getInstance(VerificationService.class).getVerificationServiceSecretKey());
  }

  private void initializeServiceTaskPoll(Injector injector) {
    injector.getInstance(WorkflowVerificationTaskPoller.class).scheduleTaskPoll();
    injector.getInstance(ServiceGuardAccountPoller.class).scheduleAdministrativeTasks();
  }
}
