package software.wings.app;

import java.util.EnumSet;

import javax.servlet.DispatcherType;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import software.wings.filter.AuditRequestFilter;
import software.wings.filter.AuditResponseFilter;
import software.wings.filter.ResponseMessageResolver;
import software.wings.health.WingsHealthCheck;
import software.wings.resources.AppResource;

/**
 *  The main application - entry point for the entire Wings Application
 *
 * @author Rishi
 *
 */
public class WingsApplication extends Application<MainConfiguration> {
  public static void main(String[] args) throws Exception {
    new WingsApplication().run(args);
  }

  @Override
  public String getName() {
    return "Wings Application";
  }

  @Override
  public void initialize(Bootstrap<MainConfiguration> bootstrap) {
    logger.info("bootstrapping ...");
    // Enable variable substitution with environment variables
    bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
        bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));
    bootstrap.addBundle(new AssetsBundle("/static", "/static", "index.html"));
    logger.info("bootstrapping done.");
  }

  @Override
  public void run(MainConfiguration configuration, Environment environment) {
    logger.info("Starting app ...");
    WingsBootstrap.initialize(configuration, environment);

    environment.jersey().packages(AppResource.class.getPackage().getName());
    environment.jersey().register(AuditRequestFilter.class);
    environment.jersey().register(ResponseMessageResolver.class);
    environment.jersey().register(MultiPartFeature.class);

    environment.servlets()
        .addFilter("AuditResponseFilter", new AuditResponseFilter())
        .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");

    environment.healthChecks().register("WingsApp", new WingsHealthCheck(configuration));

    logger.info("Starting app done");
  }

  private static Logger logger = LoggerFactory.getLogger(WingsApplication.class);
}
