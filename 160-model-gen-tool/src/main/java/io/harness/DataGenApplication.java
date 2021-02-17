package io.harness;

import static io.harness.cache.CacheBackend.NOOP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.pms.sdk.PmsSdkConfiguration.DeployMode.LOCAL;
import static io.harness.stream.AtmosphereBroadcaster.MEMORY;

import static org.mockito.Mockito.mock;

import io.harness.cache.CacheConfig;
import io.harness.cache.CacheModule;
import io.harness.commandlibrary.client.CommandLibraryServiceHttpClient;
import io.harness.configuration.DeployMode;
import io.harness.cvng.client.CVNGClientModule;
import io.harness.delegate.beans.DelegateAsyncTaskResponse;
import io.harness.delegate.beans.DelegateSyncTaskResponse;
import io.harness.delegate.beans.DelegateTaskProgressResponse;
import io.harness.event.EventsModule;
import io.harness.event.handler.segment.SegmentConfig;
import io.harness.exception.WingsException;
import io.harness.govern.ProviderModule;
import io.harness.maintenance.MaintenanceController;
import io.harness.manage.GlobalContextManager;
import io.harness.mongo.MongoModule;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.persistence.HPersistence;
import io.harness.pms.contracts.execution.events.OrchestrationEventType;
import io.harness.pms.sdk.PmsSdkConfiguration;
import io.harness.pms.sdk.PmsSdkModule;
import io.harness.pms.sdk.core.events.OrchestrationEventHandler;
import io.harness.registrars.OrchestrationModuleRegistrarHelper;
import io.harness.registrars.OrchestrationStepsModuleEventHandlerRegistrar;
import io.harness.registrars.OrchestrationStepsModuleFacilitatorRegistrar;
import io.harness.registrars.OrchestrationVisualizationModuleEventHandlerRegistrar;
import io.harness.registrars.WingsAdviserRegistrar;
import io.harness.registrars.WingsStepRegistrar;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.ManagerRegistrars;
import io.harness.service.DelegateServiceModule;
import io.harness.springdata.SpringPersistenceModule;
import io.harness.stream.AtmosphereBroadcaster;
import io.harness.stream.StreamModule;
import io.harness.threading.ExecutorModule;
import io.harness.threading.ThreadPool;

import software.wings.app.AuthModule;
import software.wings.app.GcpMarketplaceIntegrationModule;
import software.wings.app.IndexMigratorModule;
import software.wings.app.MainConfiguration;
import software.wings.app.ManagerExecutorModule;
import software.wings.app.ManagerQueueModule;
import software.wings.app.SSOModule;
import software.wings.app.SignupModule;
import software.wings.app.TemplateModule;
import software.wings.app.WingsModule;
import software.wings.app.YamlModule;
import software.wings.licensing.LicenseService;
import software.wings.security.ThreadLocalUserProvider;
import software.wings.service.impl.AccountServiceImpl;
import software.wings.service.impl.DelegateProfileServiceImpl;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.DelegateProfileService;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.dropwizard.Application;
import io.dropwizard.setup.Environment;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ServerConnector;
import org.hibernate.validator.parameternameprovider.ReflectionParameterNameProvider;
import org.mongodb.morphia.converters.TypeConverter;
import org.springframework.core.convert.converter.Converter;
import ru.vyarus.guice.validator.ValidationModule;

@Slf4j
public class DataGenApplication extends Application<MainConfiguration> {
  public static void main(String... args) throws Exception {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      log.info("Shutdown hook, entering maintenance...");
      // TODO: any cleanup
    }));
    log.info("Starting DataGen Application");
    log.info("Process: {}", ManagementFactory.getRuntimeMXBean().getName());
    DataGenApplication dataGenApplication = new DataGenApplication();
    if (args.length == 1) {
      dataGenApplication.run("server", args[0]);
    } else {
      dataGenApplication.run(args);
    }
  }

  @Override
  public void run(MainConfiguration configuration, Environment environment) throws Exception {
    log.info("Starting app ...");

    log.info("Entering startup maintenance mode");
    MaintenanceController.forceMaintenance(true);

    configuration.setSegmentConfig(SegmentConfig.builder().enabled(false).build());
    ExecutorModule.getInstance().setExecutorService(ThreadPool.create(20, 1000, 500L, TimeUnit.MILLISECONDS));

    List<Module> modules = new ArrayList<>();
    modules.add(MongoModule.getInstance());
    modules.add(new SpringPersistenceModule());
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      Set<Class<? extends KryoRegistrar>> registrars() {
        return ImmutableSet.<Class<? extends KryoRegistrar>>builder().addAll(ManagerRegistrars.kryoRegistrars).build();
      }

      @Provides
      @Singleton
      Set<Class<? extends MorphiaRegistrar>> morphiaRegistrars() {
        return ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
            .addAll(ManagerRegistrars.morphiaRegistrars)
            .build();
      }

      @Provides
      @Singleton
      Set<Class<? extends TypeConverter>> morphiaConverters() {
        return ImmutableSet.<Class<? extends TypeConverter>>builder()
            .addAll(ManagerRegistrars.morphiaConverters)
            .build();
      }

      @Provides
      @Singleton
      List<Class<? extends Converter<?, ?>>> springConverters() {
        return ImmutableList.<Class<? extends Converter<?, ?>>>builder()
            .addAll(ManagerRegistrars.springConverters)
            .build();
      }
    });

    ValidatorFactory validatorFactory = Validation.byDefaultProvider()
                                            .configure()
                                            .parameterNameProvider(new ReflectionParameterNameProvider())
                                            .buildValidatorFactory();

    CacheModule cacheModule = new CacheModule(CacheConfig.builder().cacheBackend(NOOP).build());
    modules.add(cacheModule);
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      AtmosphereBroadcaster atmosphereBroadcaster() {
        return MEMORY;
      }
    });
    modules.add(StreamModule.getInstance());

    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        bind(CommandLibraryServiceHttpClient.class).toInstance(mock(CommandLibraryServiceHttpClient.class));
      }
    });

    modules.add(new ValidationModule(validatorFactory));
    modules.add(new DelegateServiceModule());
    modules.add(new WingsModule(configuration));
    modules.add(new CVNGClientModule(configuration.getCvngClientConfig()));
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      @Named("morphiaClasses")
      Map<Class, String> morphiaCustomCollectionNames() {
        return ImmutableMap.<Class, String>builder()
            .put(DelegateSyncTaskResponse.class, "delegateSyncTaskResponses")
            .put(DelegateAsyncTaskResponse.class, "delegateAsyncTaskResponses")
            .put(DelegateTaskProgressResponse.class, "delegateTaskProgressResponses")
            .build();
      }
    });

    modules.add(new IndexMigratorModule());
    modules.add(new YamlModule());
    modules.add(new ManagerQueueModule());
    modules.add(new ManagerExecutorModule());
    modules.add(new TemplateModule());
    modules.add(new EventsModule(configuration));
    modules.add(new SSOModule());
    modules.add(new SignupModule());
    modules.add(new GcpMarketplaceIntegrationModule());
    modules.add(new AuthModule());
    modules.add(PmsSdkModule.getInstance(getPmsSdkConfiguration()));

    Injector injector = Guice.createInjector(modules);

    registerObservers(injector);

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

    String deployMode = System.getenv(DeployMode.DEPLOY_MODE);

    if (DeployMode.isOnPrem(deployMode)) {
      LicenseService licenseService = injector.getInstance(LicenseService.class);
      String encryptedLicenseInfoBase64String = System.getenv(LicenseService.LICENSE_INFO);
      if (isEmpty(encryptedLicenseInfoBase64String)) {
        log.error("No license info is provided");
      } else {
        try {
          licenseService.updateAccountLicenseForOnPrem(encryptedLicenseInfoBase64String);
        } catch (WingsException ex) {
          log.error("Error while updating license info", ex);
        }
      }
    }

    log.info("Leaving startup maintenance mode");
    MaintenanceController.resetForceMaintenance();

    log.info("Starting app done");

    log.info("Populating the data");

    DataGenService dataGenService = injector.getInstance(DataGenService.class);

    try (GlobalContextManager.GlobalContextGuard globalContextGuard =
             GlobalContextManager.initGlobalContextGuard(null)) {
      dataGenService.populateData();
      log.info("Populating data is completed");
      System.exit(0);
    } catch (Exception e) {
      log.error("Exception occurred. Exiting datagen application...", e);
      System.exit(1);
    }
  }

  private PmsSdkConfiguration getPmsSdkConfiguration() {
    Map<OrchestrationEventType, Set<Class<? extends OrchestrationEventHandler>>> engineEventHandlersMap =
        new HashMap<>();
    OrchestrationModuleRegistrarHelper.mergeEventHandlers(
        engineEventHandlersMap, OrchestrationVisualizationModuleEventHandlerRegistrar.getEngineEventHandlers());
    OrchestrationModuleRegistrarHelper.mergeEventHandlers(
        engineEventHandlersMap, OrchestrationStepsModuleEventHandlerRegistrar.getEngineEventHandlers());
    return PmsSdkConfiguration.builder()
        .deploymentMode(LOCAL)
        .serviceName("datagen")
        .engineSteps(WingsStepRegistrar.getEngineSteps())
        .engineAdvisers(WingsAdviserRegistrar.getEngineAdvisers())
        .engineFacilitators(OrchestrationStepsModuleFacilitatorRegistrar.getEngineFacilitators())
        .engineEventHandlersMap(engineEventHandlersMap)
        .build();
  }

  private void registerStores(Injector injector) {
    HPersistence persistence = injector.getInstance(HPersistence.class);
    persistence.registerUserProvider(new ThreadLocalUserProvider());
  }

  private void registerObservers(Injector injector) {
    AccountServiceImpl accountService = (AccountServiceImpl) injector.getInstance(Key.get(AccountService.class));
    accountService.getAccountCrudSubject().register(
        (DelegateProfileServiceImpl) injector.getInstance(Key.get(DelegateProfileService.class)));
  }
}
