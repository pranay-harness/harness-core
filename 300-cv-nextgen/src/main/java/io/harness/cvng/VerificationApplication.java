package io.harness.cvng;

import static com.google.inject.matcher.Matchers.not;
import static io.harness.logging.LoggingInitializer.initializeLogging;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static io.harness.security.ServiceTokenGenerator.VERIFICATION_SERVICE_SECRET;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.name.Named;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.palominolabs.metrics.guice.MetricsInstrumentationModule;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import io.harness.cvng.activity.entities.KubernetesActivitySource;
import io.harness.cvng.activity.entities.KubernetesActivitySource.KubernetesActivitySourceKeys;
import io.harness.cvng.activity.jobs.K8ActivityCollectionHandler;
import io.harness.cvng.client.NextGenClientModule;
import io.harness.cvng.client.VerificationManagerClientModule;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.CVConfig.CVConfigKeys;
import io.harness.cvng.core.entities.DeletedCVConfig;
import io.harness.cvng.core.entities.DeletedCVConfig.DeletedCVConfigKeys;
import io.harness.cvng.core.jobs.CVConfigCleanupHandler;
import io.harness.cvng.core.jobs.CVConfigDataCollectionHandler;
import io.harness.cvng.core.services.CVNextGenConstants;
import io.harness.cvng.exception.BadRequestExceptionMapper;
import io.harness.cvng.exception.ConstraintViolationExceptionMapper;
import io.harness.cvng.exception.GenericExceptionMapper;
import io.harness.cvng.exception.NotFoundExceptionMapper;
import io.harness.cvng.statemachine.jobs.AnalysisOrchestrationJob;
import io.harness.cvng.statemachine.jobs.DeploymentVerificationJobInstanceOrchestrationJob;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance.ExecutionStatus;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance.VerificationJobInstanceKeys;
import io.harness.cvng.verificationjob.jobs.DeletePerpetualTasksHandler;
import io.harness.cvng.verificationjob.jobs.ProcessQueuedVerificationJobInstanceHandler;
import io.harness.delegate.beans.DelegateAsyncTaskResponse;
import io.harness.delegate.beans.DelegateSyncTaskResponse;
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
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.mongo.iterator.provider.MorphiaPersistenceProvider;
import io.harness.morphia.MorphiaModule;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.persistence.HPersistence;
import io.harness.secretmanagerclient.SecretManagementClientModule;
import io.harness.serializer.CvNextGenRegistrars;
import io.harness.serializer.JsonSubtypeResolver;
import io.harness.serializer.KryoRegistrar;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.server.model.Resource;
import org.hibernate.validator.parameternameprovider.ReflectionParameterNameProvider;
import org.mongodb.morphia.converters.TypeConverter;
import org.reflections.Reflections;
import ru.vyarus.guice.validator.ValidationModule;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
  private HPersistence hPersistence;

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
    log.info("bootstrapping ...");
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
    configureObjectMapper(bootstrap.getObjectMapper());
    log.info("bootstrapping done.");
  }

  public static void configureObjectMapper(final ObjectMapper mapper) {
    mapper.setSubtypeResolver(new JsonSubtypeResolver(mapper.getSubtypeResolver()));
  }

  @Override
  public void run(VerificationConfiguration configuration, Environment environment) {
    log.info("Starting app ...");
    MaintenanceController.forceMaintenance(true);
    ValidatorFactory validatorFactory = Validation.byDefaultProvider()
                                            .configure()
                                            .parameterNameProvider(new ReflectionParameterNameProvider())
                                            .buildValidatorFactory();
    List<Module> modules = new ArrayList<>();
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      Set<Class<? extends KryoRegistrar>> kryoRegistrars() {
        return ImmutableSet.<Class<? extends KryoRegistrar>>builder()
            .addAll(CvNextGenRegistrars.kryoRegistrars)
            .build();
      }

      @Provides
      @Singleton
      Set<Class<? extends MorphiaRegistrar>> morphiaRegistrars() {
        return ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
            .addAll(CvNextGenRegistrars.morphiaRegistrars)
            .build();
      }

      @Provides
      @Singleton
      Set<Class<? extends TypeConverter>> morphiaConverters() {
        return ImmutableSet.<Class<? extends TypeConverter>>builder()
            .addAll(CvNextGenRegistrars.morphiaConverters)
            .build();
      }
    });
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
    modules.add(MongoModule.getInstance());
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      @Named("morphiaClasses")
      Map<Class, String> morphiaCustomCollectionNames() {
        return ImmutableMap.<Class, String>builder()
            .put(DelegateSyncTaskResponse.class, "cvng_delegateSyncTaskResponses")
            .put(DelegateAsyncTaskResponse.class, "cvng_delegateAsyncTaskResponses")
            .build();
      }
    });

    modules.add(MorphiaModule.getInstance());
    modules.add(new CVServiceModule());
    modules.add(new MetricRegistryModule(metricRegistry));
    modules.add(new VerificationManagerClientModule(configuration.getManagerClientConfig().getBaseUrl()));
    modules.add(new NextGenClientModule(configuration.getNgManagerServiceConfig()));
    modules.add(new SecretManagementClientModule(
        configuration.getManagerClientConfig(), configuration.getNgManagerServiceConfig().getManagerServiceSecret()));
    modules.add(new CVNextGenCommonsServiceModule());
    Injector injector = Guice.createInjector(modules);
    initializeServiceSecretKeys();
    harnessMetricRegistry = injector.getInstance(HarnessMetricRegistry.class);
    autoCreateCollectionsAndIndexes(injector);
    registerAuthFilters(environment, injector);
    registerManagedBeans(environment, injector);
    registerResources(environment, injector);
    registerOrchestrationIterator(injector);
    registerVerificationTaskOrchestrationIterator(injector);
    registerVerificationJobInstanceDataCollectionTaskIterator(injector);
    registerDataCollectionTaskIterator(injector);
    registerDeleteDataCollectionWorkersIterator(injector);
    registerExceptionMappers(environment.jersey());
    registerCVConfigCleanupIterator(injector);
    registerHealthChecks(environment, injector);
    log.info("Leaving startup maintenance mode");
    MaintenanceController.forceMaintenance(false);

    log.info("Starting app done");
  }

  private void autoCreateCollectionsAndIndexes(Injector injector) {
    hPersistence = injector.getInstance(HPersistence.class);
  }

  private void registerOrchestrationIterator(Injector injector) {
    ScheduledThreadPoolExecutor workflowVerificationExecutor =
        new ScheduledThreadPoolExecutor(15, new ThreadFactoryBuilder().setNameFormat("Iterator-Analysis").build());
    Handler<CVConfig> handler = injector.getInstance(AnalysisOrchestrationJob.class);
    PersistenceIterator analysisOrchestrationIterator =
        MongoPersistenceIterator.<CVConfig, MorphiaFilterExpander<CVConfig>>builder()
            .mode(PersistenceIterator.ProcessMode.PUMP)
            .clazz(CVConfig.class)
            .fieldName(CVConfigKeys.analysisOrchestrationIteration)
            .targetInterval(ofSeconds(5))
            .acceptableNoAlertDelay(ofSeconds(15))
            .executorService(workflowVerificationExecutor)
            .semaphore(new Semaphore(7))
            .handler(handler)
            .schedulingType(REGULAR)
            .filterExpander(query -> query.field(CVConfigKeys.createdAt).lessThanOrEq(Instant.now().toEpochMilli()))
            .redistribute(true)
            .persistenceProvider(injector.getInstance(MorphiaPersistenceProvider.class))
            .build();
    injector.injectMembers(analysisOrchestrationIterator);
    workflowVerificationExecutor.scheduleAtFixedRate(
        () -> analysisOrchestrationIterator.process(), 0, 20, TimeUnit.SECONDS);
  }

  private void registerVerificationTaskOrchestrationIterator(Injector injector) {
    ScheduledThreadPoolExecutor workflowVerificationExecutor =
        new ScheduledThreadPoolExecutor(5, new ThreadFactoryBuilder().setNameFormat("Iterator-Analysis").build());
    Handler<VerificationJobInstance> handler =
        injector.getInstance(DeploymentVerificationJobInstanceOrchestrationJob.class);
    PersistenceIterator analysisOrchestrationIterator =
        MongoPersistenceIterator.<VerificationJobInstance, MorphiaFilterExpander<VerificationJobInstance>>builder()
            .mode(PersistenceIterator.ProcessMode.PUMP)
            .clazz(VerificationJobInstance.class)
            .fieldName(VerificationJobInstanceKeys.analysisOrchestrationIteration)
            .targetInterval(ofSeconds(30))
            .acceptableNoAlertDelay(ofSeconds(30))
            .executorService(workflowVerificationExecutor)
            .semaphore(new Semaphore(3))
            .handler(handler)
            .schedulingType(REGULAR)
            .filterExpander(query
                -> query.field(VerificationJobInstanceKeys.executionStatus)
                       .in(Lists.newArrayList(ExecutionStatus.QUEUED, ExecutionStatus.RUNNING)))
            .redistribute(true)
            .persistenceProvider(injector.getInstance(MorphiaPersistenceProvider.class))
            .build();
    injector.injectMembers(analysisOrchestrationIterator);
    workflowVerificationExecutor.scheduleAtFixedRate(
        () -> analysisOrchestrationIterator.process(), 0, 20, TimeUnit.SECONDS);
  }

  private void registerDataCollectionTaskIterator(Injector injector) {
    ScheduledThreadPoolExecutor dataCollectionExecutor = new ScheduledThreadPoolExecutor(
        5, new ThreadFactoryBuilder().setNameFormat("cv-config-data-collection-iterator").build());
    CVConfigDataCollectionHandler cvConfigDataCollectionHandler =
        injector.getInstance(CVConfigDataCollectionHandler.class);
    // TODO: setup alert if this goes above acceptable threshold.
    PersistenceIterator dataCollectionIterator =
        MongoPersistenceIterator.<CVConfig, MorphiaFilterExpander<CVConfig>>builder()
            .mode(PersistenceIterator.ProcessMode.PUMP)
            .clazz(CVConfig.class)
            .fieldName(CVConfigKeys.dataCollectionTaskIteration)
            .targetInterval(ofMinutes(1))
            .acceptableNoAlertDelay(ofMinutes(1))
            .executorService(dataCollectionExecutor)
            .semaphore(new Semaphore(5))
            .handler(cvConfigDataCollectionHandler)
            .schedulingType(REGULAR)
            .filterExpander(query -> query.criteria(CVConfigKeys.perpetualTaskId).doesNotExist())
            .persistenceProvider(injector.getInstance(MorphiaPersistenceProvider.class))
            .redistribute(true)
            .build();
    injector.injectMembers(dataCollectionIterator);
    dataCollectionExecutor.scheduleAtFixedRate(() -> dataCollectionIterator.process(), 0, 30, TimeUnit.SECONDS);

    K8ActivityCollectionHandler k8ActivityCollectionHandler = injector.getInstance(K8ActivityCollectionHandler.class);
    PersistenceIterator activityCollectionIterator =
        MongoPersistenceIterator.<KubernetesActivitySource, MorphiaFilterExpander<KubernetesActivitySource>>builder()
            .mode(PersistenceIterator.ProcessMode.PUMP)
            .clazz(KubernetesActivitySource.class)
            .fieldName(KubernetesActivitySourceKeys.dataCollectionTaskIteration)
            .targetInterval(ofMinutes(1))
            .acceptableNoAlertDelay(ofMinutes(1))
            .executorService(dataCollectionExecutor)
            .semaphore(new Semaphore(5))
            .handler(k8ActivityCollectionHandler)
            .schedulingType(REGULAR)
            .filterExpander(query -> query.criteria(KubernetesActivitySourceKeys.dataCollectionTaskId).doesNotExist())
            .persistenceProvider(injector.getInstance(MorphiaPersistenceProvider.class))
            .redistribute(true)
            .build();
    injector.injectMembers(activityCollectionIterator);
    dataCollectionExecutor.scheduleAtFixedRate(() -> activityCollectionIterator.process(), 0, 30, TimeUnit.SECONDS);
  }

  private void registerDeleteDataCollectionWorkersIterator(Injector injector) {
    ScheduledThreadPoolExecutor verificationTaskExecutor = new ScheduledThreadPoolExecutor(
        5, new ThreadFactoryBuilder().setNameFormat("delete-data-collection-workers-iterator").build());
    DeletePerpetualTasksHandler handler = injector.getInstance(DeletePerpetualTasksHandler.class);
    // TODO: setup alert if this goes above acceptable threshold.
    PersistenceIterator dataCollectionIterator =
        MongoPersistenceIterator.<VerificationJobInstance, MorphiaFilterExpander<VerificationJobInstance>>builder()
            .mode(PersistenceIterator.ProcessMode.PUMP)
            .clazz(VerificationJobInstance.class)
            .fieldName(VerificationJobInstanceKeys.deletePerpetualTaskIteration)
            .targetInterval(ofMinutes(1))
            .acceptableNoAlertDelay(ofMinutes(10))
            .executorService(verificationTaskExecutor)
            .semaphore(new Semaphore(4))
            .handler(handler)
            .schedulingType(REGULAR)
            .filterExpander(query
                -> query.field(VerificationJobInstanceKeys.executionStatus)
                       .in(ExecutionStatus.finalStatuses())
                       .criteria(VerificationJobInstanceKeys.perpetualTaskIds)
                       .exists())
            .persistenceProvider(injector.getInstance(MorphiaPersistenceProvider.class))
            .redistribute(true)
            .build();
    injector.injectMembers(dataCollectionIterator);
    verificationTaskExecutor.scheduleAtFixedRate(() -> dataCollectionIterator.process(), 0, 30, TimeUnit.SECONDS);
  }

  private void registerVerificationJobInstanceDataCollectionTaskIterator(Injector injector) {
    ScheduledThreadPoolExecutor verificationTaskExecutor = new ScheduledThreadPoolExecutor(
        5, new ThreadFactoryBuilder().setNameFormat("verification-job-instance-data-collection-iterator").build());
    ProcessQueuedVerificationJobInstanceHandler handler =
        injector.getInstance(ProcessQueuedVerificationJobInstanceHandler.class);
    // TODO: setup alert if this goes above acceptable threshold.
    PersistenceIterator dataCollectionIterator =
        MongoPersistenceIterator.<VerificationJobInstance, MorphiaFilterExpander<VerificationJobInstance>>builder()
            .mode(PersistenceIterator.ProcessMode.PUMP)
            .clazz(VerificationJobInstance.class)
            .fieldName(VerificationJobInstanceKeys.dataCollectionTaskIteration)
            .targetInterval(ofSeconds(30))
            .acceptableNoAlertDelay(ofMinutes(1))
            .executorService(verificationTaskExecutor)
            .semaphore(new Semaphore(5))
            .handler(handler)
            .schedulingType(REGULAR)
            // TODO: find a way to implement retry logic.
            .filterExpander(query -> query.filter(VerificationJobInstanceKeys.executionStatus, ExecutionStatus.QUEUED))
            .persistenceProvider(injector.getInstance(MorphiaPersistenceProvider.class))
            .redistribute(true)
            .build();
    injector.injectMembers(dataCollectionIterator);
    verificationTaskExecutor.scheduleAtFixedRate(() -> dataCollectionIterator.process(), 0, 30, TimeUnit.SECONDS);
  }
  private void registerCVConfigCleanupIterator(Injector injector) {
    ScheduledThreadPoolExecutor dataCollectionExecutor = new ScheduledThreadPoolExecutor(
        5, new ThreadFactoryBuilder().setNameFormat("cv-config-cleanup-iterator").build());
    CVConfigCleanupHandler cvConfigCleanupHandler = injector.getInstance(CVConfigCleanupHandler.class);
    // TODO: setup alert if this goes above acceptable threshold.
    PersistenceIterator dataCollectionIterator =
        MongoPersistenceIterator.<DeletedCVConfig, MorphiaFilterExpander<DeletedCVConfig>>builder()
            .mode(PersistenceIterator.ProcessMode.PUMP)
            .clazz(DeletedCVConfig.class)
            .fieldName(DeletedCVConfigKeys.dataCollectionTaskIteration)
            .targetInterval(ofMinutes(1))
            .acceptableNoAlertDelay(ofMinutes(1))
            .executorService(dataCollectionExecutor)
            .semaphore(new Semaphore(5))
            .handler(cvConfigCleanupHandler)
            .schedulingType(REGULAR)
            .persistenceProvider(injector.getInstance(MorphiaPersistenceProvider.class))
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

  private void initializeServiceSecretKeys() {
    // TODO: using env variable directly for now. The whole secret management needs to move to env variable and
    // cv-nextgen should have a new secret with manager along with other services. Change this once everything is
    // standardized for service communication.
    VERIFICATION_SERVICE_SECRET.set(System.getenv(CVNextGenConstants.VERIFICATION_SERVICE_SECRET));
  }

  private void registerManagedBeans(Environment environment, Injector injector) {
    environment.lifecycle().manage(injector.getInstance(MaintenanceController.class));
  }

  private void registerResources(Environment environment, Injector injector) {
    long startTimeMs = System.currentTimeMillis();
    Reflections reflections = new Reflections(this.getClass().getPackage().getName());
    reflections.getTypesAnnotatedWith(Path.class).forEach(resource -> {
      if (!resource.getPackage().getName().endsWith("resources")) {
        throw new IllegalStateException("Resource classes should be in resources package." + resource);
      }
      if (Resource.isAcceptable(resource)) {
        environment.jersey().register(injector.getInstance(resource));
      }
    });
    log.info("Registered all the resources. Time taken(ms): {}", System.currentTimeMillis() - startTimeMs);
  }

  private void registerExceptionMappers(JerseyEnvironment jersey) {
    jersey.register(GenericExceptionMapper.class);
    jersey.register(ConstraintViolationExceptionMapper.class);
    jersey.register(NotFoundExceptionMapper.class);
    jersey.register(BadRequestExceptionMapper.class);
  }
}
