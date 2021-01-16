package io.harness.notification;

import static io.harness.logging.LoggingInitializer.initializeLogging;
import static io.harness.notification.NotificationConfiguration.getResourceClasses;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.stream.Collectors.toSet;

import io.harness.AuthorizationServiceHeader;
import io.harness.maintenance.MaintenanceController;
import io.harness.manage.ManagedScheduledExecutorService;
import io.harness.metrics.MetricRegistryModule;
import io.harness.ng.core.CorrelationFilter;
import io.harness.ng.core.exceptionmappers.GenericExceptionMapperV2;
import io.harness.ng.core.exceptionmappers.JerseyViolationExceptionMapperV2;
import io.harness.ng.core.exceptionmappers.WingsExceptionMapperV2;
import io.harness.notification.annotations.NotificationMicroserviceAuth;
import io.harness.notification.eventbackbone.MessageConsumer;
import io.harness.notification.eventbackbone.MongoMessageConsumer;
import io.harness.notification.exception.NotificationExceptionMapper;
import io.harness.notification.service.api.SeedDataPopulaterService;
import io.harness.persistence.HPersistence;
import io.harness.queue.QueueListenerController;
import io.harness.remote.CharsetResponseFilter;
import io.harness.remote.NGObjectMapperHelper;
import io.harness.security.JWTAuthenticationFilter;
import io.harness.service.impl.DelegateSyncServiceImpl;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
public class NotificationApplication extends Application<NotificationConfiguration> {
  private static final String APPLICATION_NAME = "Notification Microservice";

  public static void main(String[] args) throws Exception {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      log.info("Shutdown hook, entering maintenance...");
      MaintenanceController.forceMaintenance(true);
    }));
    new NotificationApplication().run(args);
  }

  private final MetricRegistry metricRegistry = new MetricRegistry();

  @Override
  public String getName() {
    return APPLICATION_NAME;
  }

  @Override
  public void initialize(Bootstrap<NotificationConfiguration> bootstrap) {
    initializeLogging();
    bootstrap.addBundle(new SwaggerBundle<NotificationConfiguration>() {
      @Override
      protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(NotificationConfiguration appConfig) {
        return getSwaggerConfiguration();
      }
    });
    // Enable variable substitution with environment variables
    bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
        bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));
    configureObjectMapper(bootstrap.getObjectMapper());
  }

  public static void configureObjectMapper(final ObjectMapper mapper) {
    NGObjectMapperHelper.configureNGObjectMapper(mapper);
  }

  @Override
  public void run(NotificationConfiguration appConfig, Environment environment) {
    log.info("Starting Next Gen Application ...");
    MaintenanceController.forceMaintenance(true);
    Injector injector =
        Guice.createInjector(new NotificationModule(appConfig), new MetricRegistryModule(metricRegistry));

    // Will create collections and Indexes
    injector.getInstance(HPersistence.class);
    registerCorsFilter(appConfig, environment);
    registerResources(environment, injector);
    registerJerseyProviders(environment);
    registerJerseyFeatures(environment);
    registerCharsetResponseFilter(environment, injector);
    registerCorrelationFilter(environment, injector);
    registerScheduleJobs(injector);
    registerManagedBeans(environment, injector);
    registerQueueListeners(injector, appConfig);
    registerAuthFilters(appConfig, environment, injector);
    //    registerIterators(injector);
    populateSeedData(injector, appConfig.getSeedDataConfiguration());
    MaintenanceController.forceMaintenance(false);
    new Thread(injector.getInstance(MessageConsumer.class)).start();
  }

  private void populateSeedData(Injector injector, SeedDataConfiguration seedDataConfiguration) {
    injector.getInstance(SeedDataPopulaterService.class).populateSeedData(seedDataConfiguration);
  }

  private void registerJerseyFeatures(Environment environment) {
    environment.jersey().register(MultiPartFeature.class);
  }

  private void registerIterators(Injector injector) {
    //    injector.getInstance(NotificationRetryHandler.class).registerIterators();
  }

  private void registerManagedBeans(Environment environment, Injector injector) {
    environment.lifecycle().manage(injector.getInstance(QueueListenerController.class));
    environment.lifecycle().manage(
        injector.getInstance(Key.get(ManagedScheduledExecutorService.class, Names.named("delegate-response"))));
  }

  private void registerCorsFilter(NotificationConfiguration appConfig, Environment environment) {
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
    environment.jersey().register(NotificationExceptionMapper.class);
    environment.jersey().register(JerseyViolationExceptionMapperV2.class);
    environment.jersey().register(WingsExceptionMapperV2.class);
    environment.jersey().register(GenericExceptionMapperV2.class);
  }

  private void registerCharsetResponseFilter(Environment environment, Injector injector) {
    environment.jersey().register(injector.getInstance(CharsetResponseFilter.class));
  }

  private void registerCorrelationFilter(Environment environment, Injector injector) {
    environment.jersey().register(injector.getInstance(CorrelationFilter.class));
  }

  private void registerQueueListeners(Injector injector, NotificationConfiguration appConfig) {
    log.info("Initializing queue listeners...");
    QueueListenerController queueListenerController = injector.getInstance(QueueListenerController.class);
    if (appConfig.getNotificationClientConfiguration()
            .getNotificationClientBackendConfiguration()
            .getType()
            .equalsIgnoreCase("mongo")) {
      queueListenerController.register(injector.getInstance(MongoMessageConsumer.class), 1);
    }
  }

  public SwaggerBundleConfiguration getSwaggerConfiguration() {
    SwaggerBundleConfiguration defaultSwaggerBundleConfiguration = new SwaggerBundleConfiguration();
    String resourcePackage = String.join(",", getUniquePackages(getResourceClasses()));
    defaultSwaggerBundleConfiguration.setResourcePackage(resourcePackage);
    defaultSwaggerBundleConfiguration.setSchemes(new String[] {"https", "http"});
    defaultSwaggerBundleConfiguration.setVersion("1.0");
    defaultSwaggerBundleConfiguration.setSchemes(new String[] {"https", "http"});
    defaultSwaggerBundleConfiguration.setHost("{{host}}");
    defaultSwaggerBundleConfiguration.setTitle("Notification Service API Reference");
    return defaultSwaggerBundleConfiguration;
  }

  private void registerScheduleJobs(Injector injector) {
    log.info("Initializing scheduled jobs...");
    injector.getInstance(Key.get(ManagedScheduledExecutorService.class, Names.named("delegate-response")))
        .scheduleWithFixedDelay(injector.getInstance(DelegateSyncServiceImpl.class), 0L, 2L, TimeUnit.SECONDS);
  }

  private void registerAuthFilters(
      NotificationConfiguration configuration, Environment environment, Injector injector) {
    if (configuration.isEnableAuth()) {
      // sample usage
      Predicate<Pair<ResourceInfo, ContainerRequestContext>> predicate = resourceInfoAndRequest
          -> resourceInfoAndRequest.getKey().getResourceMethod().getAnnotation(NotificationMicroserviceAuth.class)
              != null
          || resourceInfoAndRequest.getKey().getResourceClass().getAnnotation(NotificationMicroserviceAuth.class)
              != null;
      Map<String, String> serviceToSecretMapping = new HashMap<>();
      serviceToSecretMapping.put(AuthorizationServiceHeader.DEFAULT.getServiceId(),
          configuration.getNotificationSecrets().getManagerServiceSecret());
      environment.jersey().register(new JWTAuthenticationFilter(predicate, null, serviceToSecretMapping));
    }
  }

  private static Set<String> getUniquePackages(Collection<Class<?>> classes) {
    return classes.stream().map(aClass -> aClass.getPackage().getName()).collect(toSet());
  }
}
