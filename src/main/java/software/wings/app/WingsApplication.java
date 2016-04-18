package software.wings.app;

import java.util.EnumSet;
import java.util.Set;

import javax.servlet.DispatcherType;
import javax.ws.rs.Path;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.model.Resource;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;

import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.basic.BasicCredentialAuthFilter;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import ro.fortsoft.pf4j.PluginManager;
import ru.vyarus.guice.validator.ValidationModule;
import software.wings.beans.User;
import software.wings.exception.WingsExceptionMapper;
import software.wings.filter.AuditResponseFilter;
import software.wings.filter.ResponseMessageResolver;
import software.wings.health.WingsHealthCheck;
import software.wings.resources.AppResource;
import software.wings.security.AuthRuleFilter;
import software.wings.security.BasicAuthAuthenticator;

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
    Injector injector = Guice.createInjector(new ValidationModule(), new WingsModule(configuration));

    WingsBootstrap.initialize(injector);
    addResources(environment, injector);

    environment.jersey().register(ResponseMessageResolver.class);
    environment.jersey().register(MultiPartFeature.class);

    environment.servlets()
        .addFilter("AuditResponseFilter", new AuditResponseFilter())
        .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
    environment.healthChecks().register("WingsApp", new WingsHealthCheck(configuration));
    environment.jersey().register(WingsExceptionMapper.class);

    // Authentication/Authorization filters

    if (configuration.isEnableAuth()) {
      environment.jersey().register(new AuthDynamicFeature(new BasicCredentialAuthFilter.Builder<User>()
                                                               .setAuthenticator(new BasicAuthAuthenticator())
                                                               .buildAuthFilter()));
      environment.jersey().register(new AuthValueFactoryProvider.Binder<>(User.class));
      environment.jersey().register(AuthRuleFilter.class);
    }

    PluginManager pluginManager = injector.getInstance(PluginManager.class);
    pluginManager.loadPlugins();
    pluginManager.startPlugins();

    logger.info("Starting app done");
  }

  private void addResources(Environment environment, Injector injector) {
    Reflections reflections = new Reflections(AppResource.class.getPackage().getName());

    Set<Class<? extends Object>> resourceClasses = reflections.getTypesAnnotatedWith(Path.class);
    for (Class<?> resource : resourceClasses) {
      if (Resource.isAcceptable(resource)) {
        environment.jersey().register(injector.getInstance(resource));
      }
    }
  }
  private static Logger logger = LoggerFactory.getLogger(WingsApplication.class);
}
