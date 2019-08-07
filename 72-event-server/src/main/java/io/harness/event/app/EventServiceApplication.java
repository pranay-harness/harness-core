package io.harness.event.app;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.event.grpc.GrpcEventServer;
import io.harness.event.grpc.PublishedMessage;
import io.harness.govern.ProviderModule;
import io.harness.mongo.MongoConfig;
import io.harness.mongo.MongoModule;
import io.harness.serializer.YamlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.hibernate.validator.parameternameprovider.ReflectionParameterNameProvider;
import ru.vyarus.guice.validator.ValidationModule;

import java.io.File;
import java.io.IOException;
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
    File configFile = new File(args[1]);
    EventServiceConfig config =
        new YamlUtils().read(FileUtils.readFileToString(configFile, UTF_8), EventServiceConfig.class);
    new EventServiceApplication(config).run();
  }

  private void run() throws IOException, InterruptedException {
    ValidatorFactory validatorFactory = Validation.byDefaultProvider()
                                            .configure()
                                            .parameterNameProvider(new ReflectionParameterNameProvider())
                                            .buildValidatorFactory();

    Injector injector = Guice.createInjector(new ValidationModule(validatorFactory), new ProviderModule() {
      @Provides
      @Named("morphiaClasses")
      Set<Class> classes() {
        return morphiaClasses;
      }

      @Provides
      @Singleton
      MongoConfig mongoConfig() {
        return config.getMongoConnectionFactory();
      }
    }, new MongoModule(), new EventServiceModule(config));
    GrpcEventServer server = injector.getInstance(GrpcEventServer.class);
    Runtime.getRuntime().addShutdownHook(new Thread(xxxxxxxx:shutdown));
    server.awaitTermination();
  }
}
