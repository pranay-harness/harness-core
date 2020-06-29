package io.harness.ng;

import static com.google.common.collect.ImmutableMap.of;
import static io.harness.logging.LoggingInitializer.initializeLogging;
import static io.harness.ng.NextGenConfiguration.getResourceClasses;

import com.google.inject.Guice;
import com.google.inject.Injector;

import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import io.harness.ng.core.CorrelationFilter;
import io.harness.ng.core.exceptionmappers.GenericExceptionMapperV2;
import io.harness.ng.core.exceptionmappers.JerseyViolationExceptionMapperV2;
import io.harness.ng.core.exceptionmappers.WingsExceptionMapperV2;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.model.Resource;
import software.wings.app.CharsetResponseFilter;

import java.util.EnumSet;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;

@Slf4j
public class NextGenApplication extends Application<NextGenConfiguration> {
  private static final String APPLICATION_NAME = "CD NextGen Application";

  public static void main(String[] args) throws Exception {
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
    bootstrap.addBundle(new SwaggerBundle<NextGenConfiguration>() {
      @Override
      protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(NextGenConfiguration appConfig) {
        return appConfig.getSwaggerBundleConfiguration();
      }
    });
  }

  @Override
  public void run(NextGenConfiguration appConfig, Environment environment) {
    logger.info("Starting Next Gen Application ...");

    Injector injector = Guice.createInjector(new NextGenModule(appConfig));

    // Will create collections and Indexes
    injector.getInstance(HPersistence.class);

    registerCorsFilter(appConfig, environment);
    registerResources(environment, injector);
    registerJerseyProviders(environment);
    registerJerseyFeatures(environment);
    registerCharsetResponseFilter(environment, injector);
    registerCorrelationFilter(environment, injector);
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
}
