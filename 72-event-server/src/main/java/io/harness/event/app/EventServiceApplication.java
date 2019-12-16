package io.harness.event.app;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import io.harness.govern.ProviderModule;
import io.harness.mongo.MongoConfig;
import io.harness.mongo.MongoModule;
import io.harness.persistence.HPersistence;
import io.harness.persistence.Store;
import io.harness.serializer.YamlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.hibernate.validator.parameternameprovider.ReflectionParameterNameProvider;
import ru.vyarus.guice.validator.ValidationModule;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;

@Slf4j
public class EventServiceApplication {
  public static final String EVENTS_DB = "events";
  public static final Store EVENTS_STORE = Store.builder().name(EVENTS_DB).build();

  private final EventServiceConfig config;

  private EventServiceApplication(EventServiceConfig config) {
    this.config = config;
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    logger.info("Starting event service application...");

    File configFile = new File(args[1]);
    EventServiceConfig config =
        new YamlUtils().read(FileUtils.readFileToString(configFile, UTF_8), EventServiceConfig.class);
    new EventServiceApplication(config).run();
  }

  private void run() throws InterruptedException {
    logger.info("Starting application using config: {}", config);
    ValidatorFactory validatorFactory = Validation.byDefaultProvider()
                                            .configure()
                                            .parameterNameProvider(new ReflectionParameterNameProvider())
                                            .buildValidatorFactory();

    List<Module> modules = new ArrayList<>();
    modules.add(new ValidationModule(validatorFactory));
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      MongoConfig mongoConfig() {
        return config.getHarnessMongo();
      }
    });

    modules.addAll(new MongoModule().cumulativeDependencies());
    modules.add(new EventServiceModule(config));

    Injector injector = Guice.createInjector(modules);
    registerStores(config, injector);

    ServiceManager serviceManager = injector.getInstance(ServiceManager.class).startAsync();
    serviceManager.awaitHealthy();
    logger.info("Server startup complete");
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      logger.info("Shutting down server...");
      serviceManager.stopAsync().awaitStopped();
    }));
    serviceManager.awaitStopped();
    logger.info("Server shutdown complete");
    LogManager.shutdown();
  }

  private static void registerStores(EventServiceConfig config, Injector injector) {
    final String eventsMongoUri = config.getEventsMongo().getUri();
    if (isNotEmpty(eventsMongoUri) && !eventsMongoUri.equals(config.getHarnessMongo().getUri())) {
      final HPersistence hPersistence = injector.getInstance(HPersistence.class);
      hPersistence.register(EVENTS_STORE, config.getEventsMongo().getUri());
    }
  }
}
