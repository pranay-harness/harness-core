package io.harness.ng;

import static io.harness.AuthorizationServiceHeader.BEARER;
import static io.harness.AuthorizationServiceHeader.CI_MANAGER;
import static io.harness.AuthorizationServiceHeader.DEFAULT;
import static io.harness.AuthorizationServiceHeader.IDENTITY_SERVICE;
import static io.harness.AuthorizationServiceHeader.MANAGER;
import static io.harness.logging.LoggingInitializer.initializeLogging;
import static io.harness.ng.NextGenConfiguration.getResourceClasses;
import static io.harness.waiter.NgOrchestrationNotifyEventListener.NG_ORCHESTRATION;

import static com.google.common.collect.ImmutableMap.of;

import io.harness.cdng.creator.CDNGModuleInfoProvider;
import io.harness.cdng.creator.CDNGPlanCreatorProvider;
import io.harness.cdng.creator.filters.CDNGFilterCreationResponseMerger;
import io.harness.cdng.executionplan.ExecutionPlanCreatorRegistrar;
import io.harness.cdng.orchestration.NgStepRegistrar;
import io.harness.engine.events.OrchestrationEventListener;
import io.harness.gitsync.core.runnable.GitChangeSetRunnable;
import io.harness.maintenance.MaintenanceController;
import io.harness.ng.core.CorrelationFilter;
import io.harness.ng.core.EtagFilter;
import io.harness.ng.core.event.EntityCRUDStreamConsumer;
import io.harness.ng.core.exceptionmappers.GenericExceptionMapperV2;
import io.harness.ng.core.exceptionmappers.JerseyViolationExceptionMapperV2;
import io.harness.ng.core.exceptionmappers.NotFoundExceptionMapper;
import io.harness.ng.core.exceptionmappers.OptimisticLockingFailureExceptionMapper;
import io.harness.ng.core.exceptionmappers.WingsExceptionMapperV2;
import io.harness.ng.core.invites.ext.mail.EmailNotificationListener;
import io.harness.ngpipeline.common.NGPipelineObjectMapperHelper;
import io.harness.persistence.HPersistence;
import io.harness.pms.sdk.PmsSdkConfiguration;
import io.harness.pms.sdk.PmsSdkConfiguration.DeployMode;
import io.harness.pms.sdk.PmsSdkInitHelper;
import io.harness.pms.sdk.PmsSdkModule;
import io.harness.pms.sdk.core.execution.NodeExecutionEventListener;
import io.harness.pms.sdk.execution.SdkOrchestrationEventListener;
import io.harness.pms.serializer.jackson.PmsBeansJacksonModule;
import io.harness.queue.QueueListenerController;
import io.harness.queue.QueuePublisher;
import io.harness.registrars.NGExecutionEventHandlerRegistrar;
import io.harness.registrars.OrchestrationAdviserRegistrar;
import io.harness.registrars.OrchestrationStepsModuleFacilitatorRegistrar;
import io.harness.security.JWTAuthenticationFilter;
import io.harness.security.annotations.NextGenManagerAuth;
import io.harness.service.impl.DelegateAsyncServiceImpl;
import io.harness.service.impl.DelegateProgressServiceImpl;
import io.harness.service.impl.DelegateSyncServiceImpl;
import io.harness.threading.ExecutorModule;
import io.harness.threading.ThreadPool;
import io.harness.waiter.NgOrchestrationNotifyEventListener;
import io.harness.waiter.NotifierScheduledExecutorService;
import io.harness.waiter.NotifyEvent;
import io.harness.waiter.NotifyQueuePublisherRegister;
import io.harness.waiter.NotifyResponseCleaner;
import io.harness.waiter.ProgressUpdateService;
import io.harness.yaml.YamlSdkConfiguration;
import io.harness.yaml.YamlSdkConstants;
import io.harness.yaml.YamlSdkModule;

import software.wings.app.CharsetResponseFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.model.Resource;

@Slf4j
public class NextGenApplication extends Application<NextGenConfiguration> {
  private static final SecureRandom random = new SecureRandom();
  private static final String APPLICATION_NAME = "CD NextGen Application";

  public static void main(String[] args) throws Exception {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      log.info("Shutdown hook, entering maintenance...");
      MaintenanceController.forceMaintenance(true);
    }));
    new NextGenApplication().run(args);
  }

  @Override
  public String getName() {
    return APPLICATION_NAME;
  }

  @Override
  public void initialize(Bootstrap<NextGenConfiguration> bootstrap) {
    initializeLogging();
    // Enable variable substitution with environment variables
    bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
        bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));
    configureObjectMapper(bootstrap.getObjectMapper());
    bootstrap.addBundle(new SwaggerBundle<NextGenConfiguration>() {
      @Override
      protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(NextGenConfiguration appConfig) {
        return appConfig.getSwaggerBundleConfiguration();
      }
    });
  }
  public static void configureObjectMapper(final ObjectMapper mapper) {
    NGPipelineObjectMapperHelper.configureNGObjectMapper(mapper);
    mapper.registerModule(new PmsBeansJacksonModule());
  }

  @Override
  public void run(NextGenConfiguration appConfig, Environment environment) {
    log.info("Starting Next Gen Application ...");
    ExecutorModule.getInstance().setExecutorService(ThreadPool.create(
        20, 1000, 500L, TimeUnit.MILLISECONDS, new ThreadFactoryBuilder().setNameFormat("main-app-pool-%d").build()));
    MaintenanceController.forceMaintenance(true);
    List<Module> modules = new ArrayList<>();
    modules.add(new NextGenModule(appConfig));
    modules.add(PmsSdkModule.getInstance(getPmsSdkConfiguration(appConfig)));
    Injector injector = Guice.createInjector(modules);

    // Will create collections and Indexes
    injector.getInstance(HPersistence.class);
    registerCorsFilter(appConfig, environment);
    registerResources(environment, injector);
    registerJerseyProviders(environment);
    registerJerseyFeatures(environment);
    registerCharsetResponseFilter(environment, injector);
    registerCorrelationFilter(environment, injector);
    registerEtagFilter(environment, injector);
    registerScheduleJobs(injector);
    registerWaitEnginePublishers(injector);
    registerManagedBeans(environment, injector);
    registerQueueListeners(injector, appConfig);
    registerExecutionPlanCreators(injector);
    registerAuthFilters(appConfig, environment, injector);
    registerPipelineSDK(appConfig, injector);

    registerYamlSdk();

    MaintenanceController.forceMaintenance(false);
    createConsumerThreadsToListenToEvents(injector);
  }

  private void createConsumerThreadsToListenToEvents(Injector injector) {
    new Thread(injector.getInstance(EntityCRUDStreamConsumer.class)).start();
  }

  private void registerYamlSdk() {
    final InputStream snippetIndexFile =
        getClass().getClassLoader().getResourceAsStream(YamlSdkConstants.snippetsResourceFile);
    YamlSdkConfiguration yamlSdkConfiguration = YamlSdkConfiguration.builder()
                                                    .snippetIndex(snippetIndexFile)
                                                    .schemaBasePath(YamlSdkConstants.schemaBasePath)
                                                    .build();
    YamlSdkModule.initializeDefaultInstance(yamlSdkConfiguration);
  }

  public void registerPipelineSDK(NextGenConfiguration appConfig, Injector injector) {
    PmsSdkConfiguration sdkConfig = getPmsSdkConfiguration(appConfig);
    if (sdkConfig.getDeploymentMode().equals(DeployMode.REMOTE)) {
      try {
        PmsSdkInitHelper.initializeSDKInstance(injector, sdkConfig);
      } catch (Exception e) {
        log.error("Failed To register pipeline sdk");
        System.exit(1);
      }
    }
  }

  private PmsSdkConfiguration getPmsSdkConfiguration(NextGenConfiguration appConfig) {
    boolean remote = false;
    if (appConfig.getShouldConfigureWithPMS() != null && appConfig.getShouldConfigureWithPMS()) {
      remote = true;
    }
    return PmsSdkConfiguration.builder()
        .deploymentMode(remote ? DeployMode.REMOTE : DeployMode.LOCAL)
        .serviceName("cd")
        .mongoConfig(appConfig.getPmsMongoConfig())
        .grpcServerConfig(appConfig.getPmsSdkGrpcServerConfig())
        .pmsGrpcClientConfig(appConfig.getPmsGrpcClientConfig())
        .pipelineServiceInfoProviderClass(CDNGPlanCreatorProvider.class)
        .filterCreationResponseMerger(new CDNGFilterCreationResponseMerger())
        .engineSteps(NgStepRegistrar.getEngineSteps())
        .engineAdvisers(OrchestrationAdviserRegistrar.getEngineAdvisers())
        .engineFacilitators(OrchestrationStepsModuleFacilitatorRegistrar.getEngineFacilitators())
        .engineEventHandlersMap(NGExecutionEventHandlerRegistrar.getEngineEventHandlers())
        .executionSummaryModuleInfoProviderClass(CDNGModuleInfoProvider.class)
        .build();
  }

  private void registerManagedBeans(Environment environment, Injector injector) {
    environment.lifecycle().manage(injector.getInstance(QueueListenerController.class));
    environment.lifecycle().manage(injector.getInstance(NotifierScheduledExecutorService.class));
  }

  private void registerCorsFilter(NextGenConfiguration appConfig, Environment environment) {
    FilterRegistration.Dynamic cors = environment.servlets().addFilter("CORS", CrossOriginFilter.class);
    String allowedOrigins = String.join(",", appConfig.getAllowedOrigins());
    cors.setInitParameters(of("allowedOrigins", allowedOrigins, "allowedHeaders",
        "X-Requested-With,Content-Type,Accept,Origin,Authorization,X-api-key", "allowedMethods",
        "OPTIONS,GET,PUT,POST,DELETE,HEAD", "preflightMaxAge", "86400"));
    cors.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
  }

  private void registerResources(Environment environment, Injector injector) {
    for (Class<?> resource : getResourceClasses()) {
      if (Resource.isAcceptable(resource)) {
        environment.jersey().register(injector.getInstance(resource));
      }
    }
  }

  private void registerJerseyProviders(Environment environment) {
    environment.jersey().register(JerseyViolationExceptionMapperV2.class);
    environment.jersey().register(OptimisticLockingFailureExceptionMapper.class);
    environment.jersey().register(NotFoundExceptionMapper.class);
    environment.jersey().register(WingsExceptionMapperV2.class);
    environment.jersey().register(GenericExceptionMapperV2.class);
  }

  private void registerJerseyFeatures(Environment environment) {
    environment.jersey().register(MultiPartFeature.class);
  }

  private void registerCharsetResponseFilter(Environment environment, Injector injector) {
    environment.jersey().register(injector.getInstance(CharsetResponseFilter.class));
  }

  private void registerCorrelationFilter(Environment environment, Injector injector) {
    environment.jersey().register(injector.getInstance(CorrelationFilter.class));
  }

  private void registerEtagFilter(Environment environment, Injector injector) {
    environment.jersey().register(injector.getInstance(EtagFilter.class));
  }

  private void registerQueueListeners(Injector injector, NextGenConfiguration appConfig) {
    log.info("Initializing queue listeners...");
    QueueListenerController queueListenerController = injector.getInstance(QueueListenerController.class);
    queueListenerController.register(injector.getInstance(NgOrchestrationNotifyEventListener.class), 5);
    queueListenerController.register(injector.getInstance(EmailNotificationListener.class), 1);
    queueListenerController.register(injector.getInstance(OrchestrationEventListener.class), 1);
    queueListenerController.register(injector.getInstance(NodeExecutionEventListener.class), 1);
    if (appConfig.getShouldConfigureWithPMS()) {
      queueListenerController.register(injector.getInstance(SdkOrchestrationEventListener.class), 1);
    }
  }

  private void registerWaitEnginePublishers(Injector injector) {
    final QueuePublisher<NotifyEvent> publisher =
        injector.getInstance(Key.get(new TypeLiteral<QueuePublisher<NotifyEvent>>() {}));
    final NotifyQueuePublisherRegister notifyQueuePublisherRegister =
        injector.getInstance(NotifyQueuePublisherRegister.class);
    notifyQueuePublisherRegister.register(
        NG_ORCHESTRATION, payload -> publisher.send(Arrays.asList(NG_ORCHESTRATION), payload));
  }

  private void registerScheduleJobs(Injector injector) {
    log.info("Initializing scheduled jobs...");
    injector.getInstance(NotifierScheduledExecutorService.class)
        .scheduleWithFixedDelay(
            injector.getInstance(NotifyResponseCleaner.class), random.nextInt(300), 300L, TimeUnit.SECONDS);
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("gitChangeSet")))
        .scheduleWithFixedDelay(
            injector.getInstance(GitChangeSetRunnable.class), random.nextInt(4), 4L, TimeUnit.SECONDS);
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("taskPollExecutor")))
        .scheduleWithFixedDelay(injector.getInstance(DelegateSyncServiceImpl.class), 0L, 2L, TimeUnit.SECONDS);
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("taskPollExecutor")))
        .scheduleWithFixedDelay(injector.getInstance(DelegateAsyncServiceImpl.class), 0L, 5L, TimeUnit.SECONDS);
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("taskPollExecutor")))
        .scheduleWithFixedDelay(injector.getInstance(DelegateProgressServiceImpl.class), 0L, 5L, TimeUnit.SECONDS);
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("taskPollExecutor")))
        .scheduleWithFixedDelay(injector.getInstance(ProgressUpdateService.class), 0L, 5L, TimeUnit.SECONDS);
  }

  private void registerExecutionPlanCreators(Injector injector) {
    injector.getInstance(ExecutionPlanCreatorRegistrar.class).register();
  }

  private void registerAuthFilters(NextGenConfiguration configuration, Environment environment, Injector injector) {
    if (configuration.isEnableAuth()) {
      // sample usage
      Predicate<Pair<ResourceInfo, ContainerRequestContext>> predicate = resourceInfoAndRequest
          -> resourceInfoAndRequest.getKey().getResourceMethod().getAnnotation(NextGenManagerAuth.class) != null
          || resourceInfoAndRequest.getKey().getResourceClass().getAnnotation(NextGenManagerAuth.class) != null;
      Map<String, String> serviceToSecretMapping = new HashMap<>();
      serviceToSecretMapping.put(BEARER.getServiceId(), configuration.getNextGenConfig().getJwtAuthSecret());
      serviceToSecretMapping.put(
          IDENTITY_SERVICE.getServiceId(), configuration.getNextGenConfig().getNgManagerServiceSecret());
      serviceToSecretMapping.put(DEFAULT.getServiceId(), configuration.getNextGenConfig().getNgManagerServiceSecret());
      serviceToSecretMapping.put(MANAGER.getServiceId(), configuration.getNextGenConfig().getNgManagerServiceSecret());
      serviceToSecretMapping.put(
          CI_MANAGER.getServiceId(), configuration.getNextGenConfig().getNgManagerServiceSecret());
      environment.jersey().register(new JWTAuthenticationFilter(predicate, null, serviceToSecretMapping));
    }
  }
}
