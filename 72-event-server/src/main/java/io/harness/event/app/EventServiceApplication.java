package io.harness.event.app;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import io.harness.event.grpc.GrpcEventServer;
import io.harness.event.grpc.PublishedMessage;
import io.harness.govern.ProviderModule;
import io.harness.mongo.MongoConfig;
import io.harness.mongo.MongoModule;
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
import java.util.Set;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;

@Slf4j
public class EventServiceApplication {
  private final Set<Class> morphiaClasses = ImmutableSet.of(PublishedMessage.class);
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
        return config.getMongoConnectionFactory();
      }
    });

    modules.addAll(new MongoModule().cumulativeDependencies());
    modules.add(new EventServiceModule(config));

    Injector injector = Guice.createInjector(modules);

    GrpcEventServer server = injector.getInstance(GrpcEventServer.class);
    logger.info("Server startup complete");
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      logger.info("Shutting down server...");
      server.shutdown();
    }));
    server.awaitTermination();
    logger.info("Server shutdown complete");
    LogManager.shutdown();
  }
}
