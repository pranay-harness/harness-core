package io.harness.app;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.logging.LoggingInitializer.initializeLogging;
import static io.harness.security.ServiceTokenGenerator.VERIFICATION_SERVICE_SECRET;
import static io.harness.waiter.OrchestrationNotifyEventListener.ORCHESTRATION;
import static java.util.Collections.singletonList;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.harness.CIBeansModule;
import io.harness.CIExecutionServiceModule;
import io.harness.delegate.beans.DelegateAsyncTaskResponse;
import io.harness.delegate.beans.DelegateSyncTaskResponse;
import io.harness.executionplan.CIExecutionPlanCreatorRegistrar;
import io.harness.executionplan.ExecutionPlanModule;
import io.harness.govern.ProviderModule;
import io.harness.maintenance.MaintenanceController;
import io.harness.mongo.MongoConfig;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.persistence.HPersistence;
import io.harness.persistence.Store;
import io.harness.queue.QueueController;
import io.harness.queue.QueueListenerController;
import io.harness.queue.QueuePublisher;
import io.harness.serializer.CiExecutionRegistrars;
import io.harness.serializer.KryoModule;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.ManagerRegistrars;
import io.harness.serializer.kryo.CIBeansKryoRegistrar;
import io.harness.service.impl.DelegateAsyncServiceImpl;
import io.harness.service.impl.DelegateSyncServiceImpl;
import io.harness.waiter.NotifierScheduledExecutorService;
import io.harness.waiter.NotifyEvent;
import io.harness.waiter.NotifyQueuePublisherRegister;
import io.harness.waiter.NotifyResponseCleaner;
import io.harness.waiter.OrchestrationNotifyEventListener;
import org.apache.log4j.LogManager;
import org.glassfish.jersey.server.model.Resource;
import org.hibernate.validator.parameternameprovider.ReflectionParameterNameProvider;
import org.reflections.Reflections;
import org.slf4j.Logger;
import ru.vyarus.guice.validator.ValidationModule;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.ci.CIServiceAuthSecretKey;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import javax.ws.rs.Path;

public class CIManagerApplication extends Application<CIManagerConfiguration> {
  private static final SecureRandom random = new SecureRandom();
  public static final Store HARNESS_STORE = Store.builder().name("harness").build();
  private static final Logger logger = org.slf4j.LoggerFactory.getLogger(CIManagerApplication.class);
  private static final String APP_NAME = "CI Manager Service Application";
  public static final String BASE_PACKAGE = "io.harness.app.resources";

  public static void main(String[] args) throws Exception {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      logger.info("Shutdown hook, entering maintenance...");
      MaintenanceController.forceMaintenance(true);
    }));
    new CIManagerApplication().run(args);
  }

  public static Collection<Class<?>> getResourceClasses() {
    Reflections reflections = new Reflections(BASE_PACKAGE);
    return reflections.getTypesAnnotatedWith(Path.class);
  }

  @Override
  public String getName() {
    return APP_NAME;
  }

  @Override
  public void run(CIManagerConfiguration configuration, Environment environment) {
    logger.info("Starting ci manager app ...");

    logger.info("Entering startup maintenance mode");
    MaintenanceController.forceMaintenance(true);

    logger.info("Leaving startup maintenance mode");
    List<Module> modules = new ArrayList<>();
    modules.add(KryoModule.getInstance());
    modules.add(new SCMGrpcClientModule(configuration.getScmConnectionConfig()));
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      Set<Class<? extends KryoRegistrar>> registrars() {
        return ImmutableSet.<Class<? extends KryoRegistrar>>builder()
            .addAll(ManagerRegistrars.kryoRegistrars)
            .add(CIBeansKryoRegistrar.class)
            .build();
      }

      @Provides
      @Singleton
      Set<Class<? extends MorphiaRegistrar>> morphiaRegistrars() {
        return ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
            .addAll(CiExecutionRegistrars.morphiaRegistrars)
            .build();
      }

      @Provides
      @Singleton
      @Named("morphiaClasses")
      Map<Class, String> morphiaCustomCollectionNames() {
        return ImmutableMap.<Class, String>builder()
            .put(DelegateSyncTaskResponse.class, "ciManager_delegateSyncTaskResponses")
            .put(DelegateAsyncTaskResponse.class, "ciManager_delegateAsyncTaskResponses")
            .build();
      }
    });

    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      MongoConfig mongoConfig() {
        return configuration.getHarnessCIMongo();
      }
    });

    modules.add(new CIPersistenceModule());
    addGuiceValidationModule(modules);
    modules.add(new CIManagerServiceModule(configuration, configuration.getManagerUrl()));
    modules.add(CIExecutionServiceModule.getInstance());
    modules.add(CIBeansModule.getInstance());
    modules.add(ExecutionPlanModule.getInstance());

    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        bind(QueueController.class).toInstance(new QueueController() {
          @Override
          public boolean isPrimary() {
            return true;
          }

          @Override
          public boolean isNotPrimary() {
            return false;
          }
        });
      }
    });

    Injector injector = Guice.createInjector(modules);
    registerResources(environment, injector);
    registerWaitEnginePublishers(injector);
    registerManagedBeans(environment, injector);
    registerExecutionPlanCreators(injector);
    registerQueueListeners(injector);
    registerStores(configuration, injector);
    scheduleJobs(injector);
    initializeServiceSecretKeys(injector);
    logger.info("Starting app done");
    MaintenanceController.forceMaintenance(false);
    LogManager.shutdown();
  }

  @Override
  public void initialize(Bootstrap<CIManagerConfiguration> bootstrap) {
    initializeLogging();
    logger.info("bootstrapping ...");

    bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
        bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));

    logger.info("bootstrapping done.");
  }

  private void registerResources(Environment environment, Injector injector) {
    for (Class<?> resource : getResourceClasses()) {
      if (Resource.isAcceptable(resource)) {
        environment.jersey().register(injector.getInstance(resource));
      }
    }
  }

  private void scheduleJobs(Injector injector) {
    injector.getInstance(NotifierScheduledExecutorService.class)
        .scheduleWithFixedDelay(
            injector.getInstance(NotifyResponseCleaner.class), random.nextInt(300), 300L, TimeUnit.SECONDS);
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("taskPollExecutor")))
        .scheduleWithFixedDelay(injector.getInstance(DelegateSyncServiceImpl.class), 0L, 2L, TimeUnit.SECONDS);
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("taskPollExecutor")))
        .scheduleWithFixedDelay(injector.getInstance(DelegateAsyncServiceImpl.class), 0L, 5L, TimeUnit.SECONDS);
  }

  private void registerQueueListeners(Injector injector) {
    QueueListenerController queueListenerController = injector.getInstance(QueueListenerController.class);
    queueListenerController.register(injector.getInstance(OrchestrationNotifyEventListener.class), 5);
  }

  private void registerManagedBeans(Environment environment, Injector injector) {
    environment.lifecycle().manage(injector.getInstance(QueueListenerController.class));
    environment.lifecycle().manage(injector.getInstance(NotifierScheduledExecutorService.class));
  }

  private static void addGuiceValidationModule(List<Module> modules) {
    ValidatorFactory validatorFactory = Validation.byDefaultProvider()
                                            .configure()
                                            .parameterNameProvider(new ReflectionParameterNameProvider())
                                            .buildValidatorFactory();
    modules.add(new ValidationModule(validatorFactory));
  }

  private static void registerStores(CIManagerConfiguration config, Injector injector) {
    final String ciMongo = config.getHarnessCIMongo().getUri();
    if (isNotEmpty(ciMongo) && !ciMongo.equals(config.getHarnessMongo().getUri())) {
      final HPersistence hPersistence = injector.getInstance(HPersistence.class);
      final WingsPersistence wingsPersistence = injector.getInstance(WingsPersistence.class);
      hPersistence.register(HARNESS_STORE, config.getHarnessMongo().getUri());
      wingsPersistence.register(HARNESS_STORE, config.getHarnessMongo().getUri());
    }
  }

  private void initializeServiceSecretKeys(Injector injector) {
    // TODO change it to CI token, we have to write authentication
    VERIFICATION_SERVICE_SECRET.set(injector.getInstance(CIServiceAuthSecretKey.class).getCIAuthServiceSecretKey());
  }

  private void registerWaitEnginePublishers(Injector injector) {
    final QueuePublisher<NotifyEvent> publisher =
        injector.getInstance(Key.get(new TypeLiteral<QueuePublisher<NotifyEvent>>() {}));
    final NotifyQueuePublisherRegister notifyQueuePublisherRegister =
        injector.getInstance(NotifyQueuePublisherRegister.class);
    notifyQueuePublisherRegister.register(
        ORCHESTRATION, payload -> publisher.send(singletonList(ORCHESTRATION), payload));
  }

  private void registerExecutionPlanCreators(Injector injector) {
    injector.getInstance(CIExecutionPlanCreatorRegistrar.class).register();
  }
}
