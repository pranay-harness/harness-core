package io.harness.analyserservice;

import static io.harness.logging.LoggingInitializer.initializeLogging;

import io.harness.event.QueryAnalyserEventService;
import io.harness.govern.ProviderModule;
import io.harness.maintenance.MaintenanceController;
import io.harness.service.QueryRecordService;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AnalyserServiceApplication extends Application<AnalyserServiceConfiguration> {
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

    registerManagedBeans(environment, injector);
    MaintenanceController.forceMaintenance(false);
    populateCache(injector);
  }

  private void registerManagedBeans(Environment environment, Injector injector) {
    environment.lifecycle().manage(injector.getInstance(QueryAnalyserEventService.class));
  }

  private void populateCache(Injector injector) {
    injector.getInstance(QueryRecordService.class).storeHashesInsideCache();
  }
}
