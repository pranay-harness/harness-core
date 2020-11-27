package io.harness;

import static io.harness.PipelineServiceConfiguration.getResourceClasses;
import static io.harness.logging.LoggingInitializer.initializeLogging;

import static com.google.common.collect.ImmutableMap.of;

import io.harness.govern.ProviderModule;
import io.harness.maintenance.MaintenanceController;
import io.harness.persistence.HPersistence;
import io.harness.pms.exception.WingsExceptionMapper;
import io.harness.queue.QueueListenerController;

import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.jersey.errors.EarlyEofExceptionMapper;
import io.dropwizard.jersey.jackson.JsonProcessingExceptionMapper;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.model.Resource;

@Slf4j
public class PipelineServiceApplication extends Application<PipelineServiceConfiguration> {
  private static final String APPLICATION_NAME = "Pipeline Service Application";

  public static void main(String[] args) throws Exception {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      log.info("Shutdown hook, entering maintenance...");
      MaintenanceController.forceMaintenance(true);
    }));

    new PipelineServiceApplication().run(args);
  }

  @Override
  public String getName() {
    return APPLICATION_NAME;
  }

  @Override
  public void initialize(Bootstrap<PipelineServiceConfiguration> bootstrap) {
    initializeLogging();
    // Enable variable substitution with environment variables
    bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
        bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));
    bootstrap.addBundle(new SwaggerBundle<PipelineServiceConfiguration>() {
      @Override
      protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(PipelineServiceConfiguration appConfig) {
        return appConfig.getSwaggerBundleConfiguration();
      }
    });
  }

  @Override
  public void run(PipelineServiceConfiguration appConfig, Environment environment) {
    log.info("Starting Pipeline Service Application ...");
    MaintenanceController.forceMaintenance(true);

    List<Module> modules = new ArrayList<>();
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      PipelineServiceConfiguration configuration() {
        return appConfig;
      }
    });
    modules.add(PipelineServiceModule.getInstance());

    Injector injector = Guice.createInjector(modules);
    injector.getInstance(HPersistence.class);
    registerCorsFilter(appConfig, environment);
    registerResources(environment, injector);
    registerJerseyProviders(environment, injector);
    registerManagedBeans(environment, injector);

    log.info("Initializing gRPC servers...");
    ServiceManager serviceManager = injector.getInstance(ServiceManager.class).startAsync();
    serviceManager.awaitHealthy();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> serviceManager.stopAsync().awaitStopped()));

    MaintenanceController.forceMaintenance(false);
  }

  private void registerManagedBeans(Environment environment, Injector injector) {
    environment.lifecycle().manage(injector.getInstance(QueueListenerController.class));
  }

  private void registerCorsFilter(PipelineServiceConfiguration appConfig, Environment environment) {
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

  private void registerJerseyProviders(Environment environment, Injector injector) {
    environment.jersey().register(JsonProcessingExceptionMapper.class);
    environment.jersey().register(EarlyEofExceptionMapper.class);
    environment.jersey().register(WingsExceptionMapper.class);

    environment.jersey().register(MultiPartFeature.class);
    //    environment.jersey().register(injector.getInstance(CharsetResponseFilter.class));
    //    environment.jersey().register(injector.getInstance(CorrelationFilter.class));
    //    environment.jersey().register(injector.getInstance(EtagFilter.class));
  }
}
