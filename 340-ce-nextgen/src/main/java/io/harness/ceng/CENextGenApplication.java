package io.harness.ceng;

import static io.harness.logging.LoggingInitializer.initializeLogging;
import static io.harness.remote.NGObjectMapperHelper.configureNGObjectMapper;

import io.harness.AuthorizationServiceHeader;
import io.harness.ff.FeatureFlagService;
import io.harness.health.HealthService;
import io.harness.maintenance.MaintenanceController;
import io.harness.metrics.MetricRegistryModule;
import io.harness.persistence.HPersistence;
import io.harness.security.NextGenAuthenticationFilter;
import io.harness.security.annotations.NextGenManagerAuth;
import io.harness.threading.ExecutorModule;
import io.harness.threading.ThreadPool;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
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
public class CENextGenApplication extends Application<CENextGenConfiguration> {
  private static final String APPLICATION_NAME = "CENextGen Microservice";
  private final MetricRegistry metricRegistry = new MetricRegistry();

  public static void main(String[] args) throws Exception {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      log.info("Shutdown hook, entering maintenance...");
      MaintenanceController.forceMaintenance(true);
    }));
    new CENextGenApplication().run(args);
  }

  @Override
  public String getName() {
    return APPLICATION_NAME;
  }

  @Override
  public void initialize(Bootstrap<CENextGenConfiguration> bootstrap) {
    initializeLogging();
    // Enable variable substitution with environment variables
    bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
        bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));
    configureObjectMapper(bootstrap.getObjectMapper());
  }

  public static void configureObjectMapper(final ObjectMapper mapper) {
    configureNGObjectMapper(mapper);
  }

  @Override
  public void run(CENextGenConfiguration configuration, Environment environment) throws Exception {
    ExecutorModule.getInstance().setExecutorService(ThreadPool.create(
        20, 100, 500L, TimeUnit.MILLISECONDS, new ThreadFactoryBuilder().setNameFormat("main-app-pool-%d").build()));
    log.info("Starting CE NextGen Application ...");
    MaintenanceController.forceMaintenance(true);
    Injector injector = Guice.createInjector(
        new io.harness.ceng.CENextGenModule(configuration), new MetricRegistryModule(metricRegistry));

    // create collection and indexes
    injector.getInstance(HPersistence.class);

    registerAuthFilters(configuration, environment);
    registerJerseyFeatures(environment);
    registerCorsFilter(configuration, environment);
    registerResources(environment, injector);
    initializeFeatureFlags(configuration, injector);
    registerHealthCheck(environment, injector);
    MaintenanceController.forceMaintenance(false);
  }

  private void registerHealthCheck(Environment environment, Injector injector) {
    final HealthService healthService = injector.getInstance(HealthService.class);
    environment.healthChecks().register("application", healthService);
    healthService.registerMonitor(injector.getInstance(HPersistence.class));
  }

  private void registerResources(Environment environment, Injector injector) {
    for (Class<?> resource : CENextGenConfiguration.getResourceClasses()) {
      if (Resource.isAcceptable(resource)) {
        environment.jersey().register(injector.getInstance(resource));
      }
    }
  }

  private void registerAuthFilters(CENextGenConfiguration configuration, Environment environment) {
    if (configuration.isEnableAuth()) {
      Predicate<Pair<ResourceInfo, ContainerRequestContext>> predicate = resourceInfoAndRequest
          -> resourceInfoAndRequest.getKey().getResourceMethod().getAnnotation(NextGenManagerAuth.class) != null
          || resourceInfoAndRequest.getKey().getResourceClass().getAnnotation(NextGenManagerAuth.class) != null;
      Map<String, String> serviceToSecretMapping = new HashMap<>();
      serviceToSecretMapping.put(
          AuthorizationServiceHeader.IDENTITY_SERVICE.getServiceId(), configuration.getJwtIdentityServiceSecret());
      serviceToSecretMapping.put(AuthorizationServiceHeader.BEARER.getServiceId(), configuration.getJwtAuthSecret());
      serviceToSecretMapping.put(
          AuthorizationServiceHeader.DEFAULT.getServiceId(), configuration.getNgManagerServiceSecret());
      environment.jersey().register(new NextGenAuthenticationFilter(predicate, null, serviceToSecretMapping));
    }
  }

  private void registerJerseyFeatures(Environment environment) {
    environment.jersey().register(MultiPartFeature.class);
  }

  private void registerCorsFilter(CENextGenConfiguration configuration, Environment environment) {
    FilterRegistration.Dynamic cors = environment.servlets().addFilter("CORS", CrossOriginFilter.class);
    String allowedOrigins = String.join(",", configuration.getAllowedOrigins());
    cors.setInitParameters(ImmutableMap.of("allowedOrigins", allowedOrigins, "allowedHeaders",
        "X-Requested-With,Content-Type,Accept,Origin,Authorization,X-api-key", "allowedMethods",
        "OPTIONS,GET,PUT,POST,DELETE,HEAD", "preflightMaxAge", "86400"));
    cors.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
  }

  private void initializeFeatureFlags(CENextGenConfiguration configuration, Injector injector) {
    injector.getInstance(FeatureFlagService.class)
        .initializeFeatureFlags(configuration.getDeployMode(), configuration.getFeatureFlagsEnabled());
  }
}
