package io.harness;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.utils.CacheManager.USER_CACHE;

import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.hazelcast.core.HazelcastInstance;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.dropwizard.Application;
import io.dropwizard.setup.Environment;
import io.harness.event.EventsModule;
import io.harness.event.model.EventsMorphiaClasses;
import io.harness.exception.WingsException;
import io.harness.govern.ProviderModule;
import io.harness.limits.LimitsMorphiaClasses;
import io.harness.maintenance.MaintenanceController;
import io.harness.mongo.MongoConfig;
import io.harness.mongo.MongoModule;
import io.harness.mongo.PersistenceMorphiaClasses;
import io.harness.persistence.HPersistence;
import io.harness.threading.ExecutorModule;
import io.harness.threading.ThreadPool;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ServerConnector;
import org.hibernate.validator.parameternameprovider.ReflectionParameterNameProvider;
import ru.vyarus.guice.validator.ValidationModule;
import software.wings.app.AuthModule;
import software.wings.app.CacheModule;
import software.wings.app.DeployMode;
import software.wings.app.FeatureViolationsModule;
import software.wings.app.GcpMarketplaceIntegrationModule;
import software.wings.app.GuiceObjectFactory;
import software.wings.app.MainConfiguration;
import software.wings.app.ManagerExecutorModule;
import software.wings.app.ManagerQueueModule;
import software.wings.app.SSOModule;
import software.wings.app.StreamModule;
import software.wings.app.TemplateModule;
import software.wings.app.WingsModule;
import software.wings.app.YamlModule;
import software.wings.beans.ManagerMorphiaClasses;
import software.wings.beans.User;
import software.wings.licensing.LicenseService;
import software.wings.security.ThreadLocalUserProvider;
import software.wings.utils.CacheManager;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.cache.Caching;
import javax.cache.configuration.Configuration;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;

@Slf4j
public class MigrationJobApplication extends Application<MainConfiguration> {
  public static final Set<Class> morphiaClasses = ImmutableSet.<Class>builder()
                                                      .addAll(ManagerMorphiaClasses.classes)
                                                      .addAll(ManagerMorphiaClasses.dependentClasses)
                                                      .addAll(EventsMorphiaClasses.classes)
                                                      .addAll(PersistenceMorphiaClasses.classes)
                                                      .addAll(LimitsMorphiaClasses.classes)
                                                      .build();

  public static void main(String... args) throws Exception {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      logger.info("Shutdown hook, entering maintenance...");
      // TODO: any cleanup
    }));
    logger.info("Starting DataGen Application");
    logger.info("Process: {}", ManagementFactory.getRuntimeMXBean().getName());
    MigrationJobApplication migrationJobApplication = new MigrationJobApplication();
    migrationJobApplication.run(args);
  }

  @SuppressFBWarnings("DM_EXIT")
  @Override
  public void run(MainConfiguration configuration, Environment environment) throws Exception {
    logger.info("Starting app ...");

    logger.info("Entering startup maintenance mode");
    MaintenanceController.forceMaintenance(true);

    ExecutorModule.getInstance().setExecutorService(ThreadPool.create(20, 1000, 500L, TimeUnit.MILLISECONDS));

    List<Module> modules = new ArrayList<>();
    modules.add(new ProviderModule() {
      @Provides
      @Named("morphiaClasses")
      Set<Class> classes() {
        return morphiaClasses;
      }

      @Provides
      @Singleton
      MongoConfig mongoConfig() {
        return configuration.getMongoConnectionFactory();
      }
    });
    modules.add(new MongoModule());

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
      }
    });

    modules.add(new ValidationModule(validatorFactory));
    modules.addAll(new WingsModule(configuration).cumulativeDependencies());
    modules.add(new YamlModule());
    modules.add(new ManagerQueueModule());
    modules.add(new ManagerExecutorModule());
    modules.add(new TemplateModule());
    modules.add(new EventsModule(configuration));
    modules.add(new FeatureViolationsModule());
    modules.add(new SSOModule());
    modules.add(new AuthModule());
    modules.add(new GcpMarketplaceIntegrationModule());

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

    registerStores(injector);

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
    CacheManager cacheManager = injector.getInstance(CacheManager.class);

    cacheManager.getUserCache();
    cacheManager.getUserPermissionInfoCache();
    cacheManager.getNewRelicApplicationCache();
    cacheManager.getWhitelistConfigCache();

    String deployMode = System.getenv(DeployMode.DEPLOY_MODE);

    if (DeployMode.isOnPrem(deployMode)) {
      LicenseService licenseService = injector.getInstance(LicenseService.class);
      String encryptedLicenseInfoBase64String = System.getenv(LicenseService.LICENSE_INFO);
      if (isEmpty(encryptedLicenseInfoBase64String)) {
        logger.error("No license info is provided");
      } else {
        try {
          licenseService.updateAccountLicenseForOnPrem(encryptedLicenseInfoBase64String);
        } catch (WingsException ex) {
          logger.error("Error while updating license info", ex);
        }
      }
    }

    logger.info("Leaving startup maintenance mode");
    MaintenanceController.resetForceMaintenance();

    logger.info("Starting app done");

    logger.info("Populating the data");

    MigrationJobService migrationJobService = injector.getInstance(MigrationJobService.class);
    try {
      migrationJobService.migrate();
      logger.info("Populating data is completed");
      System.exit(0);
    } catch (Exception e) {
      logger.error("Exception occurred. Exiting datagen application...", e);
      System.exit(1);
    }
  }

  private void registerStores(Injector injector) {
    final HPersistence persistence = injector.getInstance(HPersistence.class);
    persistence.registerUserProvider(new ThreadLocalUserProvider());
  }
}
