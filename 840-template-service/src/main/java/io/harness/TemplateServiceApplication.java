package io.harness;

import static io.harness.TemplateServiceConfiguration.getResourceClasses;
import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.logging.LoggingInitializer.initializeLogging;

import static com.google.common.collect.ImmutableMap.of;

import io.harness.annotations.dev.OwnedBy;
import io.harness.maintenance.MaintenanceController;
import io.harness.persistence.HPersistence;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.dropwizard.Application;
import io.dropwizard.jersey.errors.EarlyEofExceptionMapper;
import io.dropwizard.jersey.jackson.JsonProcessingExceptionMapper;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import java.util.EnumSet;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.model.Resource;

@Slf4j
@OwnedBy(CDC)
public class TemplateServiceApplication extends Application<TemplateServiceConfiguration> {
  private static final String APPLICATION_NAME = "Template Service Application";

  public static void main(String[] args) throws Exception {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      log.info("Shutdown hook, entering maintenance...");
      MaintenanceController.forceMaintenance(true);
    }));

    new TemplateServiceApplication().run(args);
  }

  @Override
  public String getName() {
    return APPLICATION_NAME;
  }

  @Override
  public void initialize(Bootstrap<TemplateServiceConfiguration> bootstrap) {
    initializeLogging();

    bootstrap.addBundle(new SwaggerBundle<TemplateServiceConfiguration>() {
      @Override
      protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(
          TemplateServiceConfiguration templateServiceConfiguration) {
        return templateServiceConfiguration.getSwaggerBundleConfiguration();
      }
    });
  }

  @Override
  public void run(TemplateServiceConfiguration templateServiceConfiguration, Environment environment) throws Exception {
    log.info("Starting Template Service Application ...");

    MaintenanceController.forceMaintenance(true);

    Injector injector = Guice.createInjector(new TemplateServiceModule(templateServiceConfiguration));
    injector.getInstance(HPersistence.class);
    registerJerseyProviders(environment, injector);
    registerResources(environment, injector);
    registerCorsFilter(templateServiceConfiguration, environment);

    MaintenanceController.forceMaintenance(false);
  }

  private void registerResources(Environment environment, Injector injector) {
    for (Class<?> resource : getResourceClasses()) {
      if (Resource.isAcceptable(resource)) {
        environment.jersey().register(injector.getInstance(resource));
      }
    }
  }

  private void registerCorsFilter(TemplateServiceConfiguration appConfig, Environment environment) {
    FilterRegistration.Dynamic cors = environment.servlets().addFilter("CORS", CrossOriginFilter.class);
    String allowedOrigins = String.join(",", appConfig.getAllowedOrigins());
    cors.setInitParameters(of("allowedOrigins", allowedOrigins, "allowedHeaders",
        "X-Requested-With,Content-Type,Accept,Origin,Authorization,X-api-key", "allowedMethods",
        "OPTIONS,GET,PUT,POST,DELETE,HEAD", "preflightMaxAge", "86400"));
    cors.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
  }

  private void registerJerseyProviders(Environment environment, Injector injector) {
    environment.jersey().register(JsonProcessingExceptionMapper.class);
    environment.jersey().register(EarlyEofExceptionMapper.class);
    environment.jersey().register(MultiPartFeature.class);
  }
}
