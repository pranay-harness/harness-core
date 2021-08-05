package io.harness.analyserservice;

import static io.harness.logging.LoggingInitializer.initializeLogging;

import static com.google.common.collect.ImmutableMap.of;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.event.QueryAnalyserEventService;
import io.harness.event.queryRecords.AnalyserSampleAggregatorService;
import io.harness.govern.ProviderModule;
import io.harness.maintenance.MaintenanceController;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.ws.rs.Path;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.glassfish.jersey.server.model.Resource;
import org.reflections.Reflections;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class AnalyserServiceApplication extends Application<AnalyserServiceConfiguration> {
  public static final String RESOURCE_PACKAGE = "io.harness.analyserservice";

  public static void main(String[] args) throws Exception {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      log.info("Shutdown hook, entering maintenance...");
      MaintenanceController.forceMaintenance(true);
    }));
    (new AnalyserServiceApplication()).run(args);
  }

  @Override
  public void initialize(Bootstrap<AnalyserServiceConfiguration> bootstrap) {
    // Enable variable substitution with environment variables
    initializeLogging();
    configureObjectMapper(bootstrap.getObjectMapper());
    bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
        bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));
  }

  public static void configureObjectMapper(final ObjectMapper mapper) {
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  @Override
  public void run(AnalyserServiceConfiguration configuration, Environment environment) throws Exception {
    log.info("Starting Pipeline Service Application ...");
    MaintenanceController.forceMaintenance(true);
    List<Module> modules = new ArrayList<>();
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      AnalyserServiceConfiguration configuration() {
        return configuration;
      }
    });
    modules.add(AnalyserServiceModule.getInstance(configuration));
    Injector injector = Guice.createInjector(modules);
    registerCorsFilter(configuration, environment);
    registerResources(environment, injector);

    registerManagedBeans(environment, injector);
    registerScheduledJobs(injector, configuration);
    MaintenanceController.forceMaintenance(false);
  }

  private void registerCorsFilter(AnalyserServiceConfiguration configuration, Environment environment) {
    FilterRegistration.Dynamic cors = environment.servlets().addFilter("CORS", CrossOriginFilter.class);
    String allowedOrigins = String.join(",", configuration.getAllowedOrigins());
    cors.setInitParameters(of("allowedOrigins", allowedOrigins, "allowedHeaders",
        "X-Requested-With,Content-Type,Accept,Origin,Authorization,X-api-key", "allowedMethods",
        "OPTIONS,GET,PUT,POST,DELETE,HEAD", "preflightMaxAge", "86400"));
    cors.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
  }

  private void registerManagedBeans(Environment environment, Injector injector) {
    environment.lifecycle().manage(injector.getInstance(QueryAnalyserEventService.class));
  }

  private void registerScheduledJobs(Injector injector, AnalyserServiceConfiguration configuration) {
    injector
        .getInstance(Key.get(
            ScheduledExecutorService.class, Names.named(AnalyserServiceConstants.SAMPLE_AGGREGATOR_SCHEDULED_THREAD)))
        .scheduleWithFixedDelay(injector.getInstance(AnalyserSampleAggregatorService.class), 0L,
            configuration.getAggregateScheduleInterval(), TimeUnit.MINUTES);
  }

  private void registerResources(Environment environment, Injector injector) {
    for (Class<?> resource : getResourceClasses()) {
      if (Resource.isAcceptable(resource)) {
        environment.jersey().register(injector.getInstance(resource));
      }
    }
  }
  public static Collection<Class<?>> getResourceClasses() {
    Reflections reflections = new Reflections(RESOURCE_PACKAGE);
    return reflections.getTypesAnnotatedWith(Path.class);
  }
}
