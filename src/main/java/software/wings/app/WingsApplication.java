package software.wings.app;

import com.deftlabs.lock.mongo.DistributedLockSvc;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.basic.BasicCredentialAuthFilter;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.model.Resource;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.pf4j.PluginManager;
import ru.vyarus.guice.validator.ValidationModule;
import software.wings.beans.User;
import software.wings.core.queue.AbstractQueueListener;
import software.wings.core.queue.QueueListenerController;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsExceptionMapper;
import software.wings.filter.AuditRequestFilter;
import software.wings.filter.AuditResponseFilter;
import software.wings.filter.ResponseMessageResolver;
import software.wings.health.WingsHealthCheck;
import software.wings.resources.AppResource;
import software.wings.security.AuthResponseFilter;
import software.wings.security.AuthRuleFilter;
import software.wings.security.BasicAuthAuthenticator;
import software.wings.waitNotify.Notifier;

import javax.servlet.DispatcherType;
import javax.ws.rs.Path;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

    registerResources(environment, injector);

    registerManagedBeans(environment, injector);

    registerQueueListeners(injector);

    registerScheduledJobs(injector);

    environment.servlets()
        .addFilter("AuditResponseFilter", new AuditResponseFilter())
        .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
    environment.jersey().register(AuditRequestFilter.class);

    registerJerseyProviders(environment);

    // Authentication/Authorization filters
    registerAuthFilters(configuration, environment);

    environment.healthChecks().register("WingsApp", new WingsHealthCheck(configuration));

    startPlugins(injector);

    logger.info("Starting app done");
  }

  /**
   * @param injector
   */
  private void startPlugins(Injector injector) {
    PluginManager pluginManager = injector.getInstance(PluginManager.class);
    pluginManager.loadPlugins();
    pluginManager.startPlugins();
  }

  private void registerAuthFilters(MainConfiguration configuration, Environment environment) {
    if (configuration.isEnableAuth()) {
      environment.jersey().register(new AuthDynamicFeature(new BasicCredentialAuthFilter.Builder<User>()
                                                               .setAuthenticator(new BasicAuthAuthenticator())
                                                               .buildAuthFilter()));
      environment.jersey().register(new AuthValueFactoryProvider.Binder<>(User.class));
      environment.jersey().register(AuthRuleFilter.class);
      environment.jersey().register(AuthResponseFilter.class);
    }
  }

  private void registerJerseyProviders(Environment environment) {
    environment.jersey().register(ResponseMessageResolver.class);
    environment.jersey().register(MultiPartFeature.class);
    environment.jersey().register(WingsExceptionMapper.class);
  }

  private void registerResources(Environment environment, Injector injector) {
    Reflections reflections = new Reflections(AppResource.class.getPackage().getName());

    Set<Class<? extends Object>> resourceClasses = reflections.getTypesAnnotatedWith(Path.class);
    for (Class<?> resource : resourceClasses) {
      if (Resource.isAcceptable(resource)) {
        environment.jersey().register(injector.getInstance(resource));
      }
    }
  }

  private void registerQueueListeners(Injector injector) {
    logger.info("Initializing queuelisteners...");
    Reflections reflections = new Reflections("software.wings");

    Set<Class<? extends AbstractQueueListener>> queueListeners = reflections.getSubTypesOf(AbstractQueueListener.class);
    for (Class<? extends AbstractQueueListener> queueListener : queueListeners) {
      logger.info("Registering queue listener for queue {}", injector.getInstance(queueListener).getQueue().name());
      injector.getInstance(QueueListenerController.class).register(injector.getInstance(queueListener), 5);
    }
  }

  private void registerScheduledJobs(Injector injector) {
    logger.info("Initializing scheduledJobs...");
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("notifier")))
        .scheduleWithFixedDelay(injector.getInstance(Notifier.class), 0L, 30000L, TimeUnit.MILLISECONDS);
  }

  private void registerManagedBeans(Environment environment, Injector injector) {
    environment.lifecycle().manage((Managed) injector.getInstance(WingsPersistence.class));
    environment.lifecycle().manage((Managed) injector.getInstance(DistributedLockSvc.class));
    environment.lifecycle().manage(injector.getInstance(QueueListenerController.class));
    environment.lifecycle().manage(
        (Managed) injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("timer"))));
    environment.lifecycle().manage(
        (Managed) injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("notifier"))));
    environment.lifecycle().manage((Managed) injector.getInstance(ExecutorService.class));
  }

  private static Logger logger = LoggerFactory.getLogger(WingsApplication.class);
}
