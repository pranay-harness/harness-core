package io.harness;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.beans.FeatureName.MONGO_QUEUE_VERSIONING;
import static software.wings.common.Constants.USER_CACHE;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import com.hazelcast.core.HazelcastInstance;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.dropwizard.Application;
import io.dropwizard.setup.Environment;
import io.harness.exception.WingsException;
import io.harness.maintenance.MaintenanceController;
import io.harness.mongo.MongoModule;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ServerConnector;
import org.hibernate.validator.parameternameprovider.ReflectionParameterNameProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.guice.validator.ValidationModule;
import software.wings.app.CacheModule;
import software.wings.app.DeployMode;
import software.wings.app.ExecutorModule;
import software.wings.app.GuiceObjectFactory;
import software.wings.app.MainConfiguration;
import software.wings.app.QueueModule;
import software.wings.app.StreamModule;
import software.wings.app.TemplateModule;
import software.wings.app.WingsModule;
import software.wings.app.YamlModule;
import software.wings.beans.User;
import software.wings.common.Constants;
import software.wings.health.WingsHealthCheck;
import software.wings.service.intfc.AccountService;
import software.wings.utils.CacheHelper;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import javax.cache.Caching;
import javax.cache.configuration.Configuration;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;

public class DataGenApplication extends Application<MainConfiguration> {
  private static final Logger logger = LoggerFactory.getLogger(DataGenApplication.class);

  public static void main(String... args) throws Exception {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      logger.info("Shutdown hook, entering maintenance...");
      // TODO: any cleanup
    }));
    logger.info("Starting DataGen Application");
    logger.info("Process: {}", ManagementFactory.getRuntimeMXBean().getName());
    DataGenApplication dataGenApplication = new DataGenApplication();
    dataGenApplication.run(args);
  }

  @SuppressFBWarnings("DM_EXIT")
  @Override
  public void run(MainConfiguration configuration, Environment environment) throws Exception {
    logger.info("Starting app ...");

    logger.info("Entering startup maintenance mode");
    MaintenanceController.forceMaintenance(true);

    MongoModule databaseModule = new MongoModule(configuration.getMongoConnectionFactory());
    List<Module> modules = new ArrayList<>();
    modules.add(databaseModule);

    ValidatorFactory validatorFactory = Validation.byDefaultProvider()
                                            .configure()
                                            .parameterNameProvider(new ReflectionParameterNameProvider())
                                            .buildValidatorFactory();

    CacheModule cacheModule = new CacheModule(configuration);
    modules.add(cacheModule);
    StreamModule streamModule = new StreamModule(environment, cacheModule.getHazelcastInstance());

    modules.add(streamModule);

    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        bind(HazelcastInstance.class).toInstance(cacheModule.getHazelcastInstance());
        //        bind(MetricRegistry.class).toInstance(metricRegistry);
      }
    });

    //    modules.add(MetricsInstrumentationModule.builder()
    //                    .withMetricRegistry(metricRegistry)
    //                    .withMatcher(not(new AbstractMatcher<TypeLiteral<?>>() {
    //                      @Override
    //                      public boolean matches(TypeLiteral<?> typeLiteral) {
    //                        return typeLiteral.getRawType().isAnnotationPresent(Path.class);
    //                      }
    //                    }))
    //                    .build());

    modules.add(new ValidationModule(validatorFactory));
    modules.addAll(new WingsModule(configuration).cumulativeDependencies());
    modules.add(new YamlModule());
    modules.add(
        new QueueModule(StringUtils.contains(configuration.getFeatureNames(), MONGO_QUEUE_VERSIONING.toString())));
    modules.add(new ExecutorModule());
    modules.add(new TemplateModule());

    Injector injector = Guice.createInjector(modules);

    Caching.getCachingProvider().getCacheManager().createCache(USER_CACHE, new Configuration<String, User>() {
      public static final long serialVersionUID = 1L;

      @Override
      public Class<String> getKeyType() {
        return String.class;
      }

      @Override
      public Class<User> getValueType() {
        return User.class;
      }

      @Override
      public boolean isStoreByValue() {
        return true;
      }
    });

    streamModule.getAtmosphereServlet().framework().objectFactory(new GuiceObjectFactory(injector));

    //    registerResources(environment, injector);

    //    registerManagedBeans(environment, injector);

    //    registerQueueListeners(injector);

    //    scheduleJobs(injector);

    //    registerObservers(injector);

    //    registerCronJobs(injector);

    //    registerCorsFilter(configuration, environment);

    //    registerAuditResponseFilter(environment, injector);

    //    registerJerseyProviders(environment);

    //    registerCharsetResponseFilter(environment, injector);

    // Authentication/Authorization filters
    //    registerAuthFilters(configuration, environment, injector);

    environment.healthChecks().register("WingsApp", new WingsHealthCheck());

    //    startPlugins(injector);

    environment.lifecycle().addServerLifecycleListener(server -> {
      for (Connector connector : server.getConnectors()) {
        if (connector instanceof ServerConnector) {
          ServerConnector serverConnector = (ServerConnector) connector;
          if (serverConnector.getName().equalsIgnoreCase("application")) {
            configuration.setSslEnabled(
                serverConnector.getDefaultConnectionFactory().getProtocol().equalsIgnoreCase("ssl"));
            configuration.setApplicationPort(serverConnector.getLocalPort());
            return;
          }
        }
      }
    });

    // Access all caches before coming out of maintenance
    CacheHelper cacheHelper = injector.getInstance(CacheHelper.class);

    cacheHelper.getUserCache();
    cacheHelper.getUserPermissionInfoCache();
    cacheHelper.getNewRelicApplicationCache();
    cacheHelper.getWhitelistConfigCache();

    String deployMode = System.getenv("DEPLOY_MODE");

    if (DeployMode.isOnPrem(deployMode)) {
      AccountService accountService = injector.getInstance(AccountService.class);
      String encryptedLicenseInfoBase64String = System.getenv(Constants.LICENSE_INFO);
      if (isEmpty(encryptedLicenseInfoBase64String)) {
        logger.error("No license info is provided");
      } else {
        try {
          accountService.updateAccountLicenseForOnPrem(encryptedLicenseInfoBase64String);
        } catch (WingsException ex) {
          logger.error("Error while updating license info", ex);
        }
      }
    }

    logger.info("Leaving startup maintenance mode");
    MaintenanceController.resetForceMaintenance();

    logger.info("Starting app done");

    logger.info("Populating the data");

    DataGenService dataGenService = injector.getInstance(DataGenService.class);
    dataGenService.populateData();
    logger.info("Populating data is completed");
    System.exit(0);
  }
}
