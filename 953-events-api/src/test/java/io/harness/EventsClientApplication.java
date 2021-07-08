package io.harness;

import io.harness.eventsframework.impl.redis.GitAwareRedisProducer;
import io.harness.eventsframework.impl.redis.RedisConsumer;
import io.harness.eventsframework.impl.redis.RedisProducer;
import io.harness.eventsframework.impl.redis.RedisSerialConsumer;
import io.harness.eventsframework.impl.redis.VersionedRedisConsumer;
import io.harness.eventsframework.impl.redis.VersionedRedisProducer;
import io.harness.lock.redis.RedisPersistentLocker;
import io.harness.logging.LoggingInitializer;
import io.harness.maintenance.MaintenanceController;
import io.harness.metrics.MetricRegistryModule;
import io.harness.queue.QueueListenerController;
import io.harness.redis.RedisConfig;
import io.harness.remote.NGObjectMapperHelper;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.protobuf.InvalidProtocolBufferException;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.io.UnsupportedEncodingException;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

@Slf4j
public class EventsClientApplication extends Application<EventsClientApplicationConfiguration> {
  private static final String APPLICATION_NAME = "Events API Client Test";

  public static void main(String[] args) throws Exception {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      log.info("Shutdown hook, entering maintenance...");
      MaintenanceController.forceMaintenance(true);
    }));
    new EventsClientApplication().run(args);
  }

  private final MetricRegistry metricRegistry = new MetricRegistry();

  @Override
  public String getName() {
    return APPLICATION_NAME;
  }

  @Override
  public void initialize(Bootstrap<EventsClientApplicationConfiguration> bootstrap) {
    LoggingInitializer.initializeLogging();
    // Enable variable substitution with environment variables
    bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
        bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));
    configureObjectMapper(bootstrap.getObjectMapper());
  }
  public static void configureObjectMapper(final ObjectMapper mapper) {
    NGObjectMapperHelper.configureNGObjectMapper(mapper);
  }

  @Override
  public void run(EventsClientApplicationConfiguration appConfig, Environment environment)
      throws InvalidProtocolBufferException, UnsupportedEncodingException, InterruptedException {
    log.info("Starting Next Gen Application ...");
    MaintenanceController.forceMaintenance(true);
    Injector injector =
        Guice.createInjector(new EventsClientApplicationModule(appConfig), new MetricRegistryModule(metricRegistry));

    registerJerseyFeatures(environment);
    registerManagedBeans(environment, injector);
    MaintenanceController.forceMaintenance(false);

    String channel = "test_events_application";

    RedisPersistentLocker redisLocker = injector.getInstance(RedisPersistentLocker.class);
    /* ----------------- Perform operations ----------------- */
    RedisConfig redisConfig = appConfig.getEventsFrameworkConfiguration().getRedisConfig();

    /* Push messages to redis channel */
    RedisProducer redisProducer = RedisProducer.of(channel, redisConfig, 10000, "dummyMessageProducer");
    VersionedRedisProducer versionedRedisProducer1 =
        VersionedRedisProducer.of(channel, redisConfig, 10000, "dummyMessageProducer", "1.0");
    VersionedRedisProducer versionedRedisProducer2 =
        VersionedRedisProducer.of(channel, redisConfig, 10000, "dummyMessageProducer", "2.0");
    GitAwareRedisProducer gitAwareRedisProducer =
        GitAwareRedisProducer.of(channel, redisConfig, 10000, "dummyGitAwareMessageProducer");
    //    new Thread(new MessageProducer(versionedRedisProducer1, ColorConstants.TEXT_YELLOW, false, 1, 50)).start();
    //    new Thread(new MessageProducer(versionedRedisProducer2, ColorConstants.TEXT_CYAN, false, 1000,
    //    1000000)).start(); new Thread(new MessageProducer(redisProducer, ColorConstants.TEXT_YELLOW, false)).start();
    //    new Thread(new MessageProducer(gitAwareRedisProducer, ColorConstants.TEXT_GREEN, true)).start();

    //    RedisSerialConsumer redisSerialConsumer = RedisSerialConsumer.of(channel, "group1", "hardcodedconsumer",
    //    redisConfig, Duration.ofSeconds(10));
    VersionedRedisConsumer versionedRedisConsumer1 =
        VersionedRedisConsumer.of(channel, "group2", redisConfig, Duration.ofSeconds(10), 5, "1.0");
    VersionedRedisConsumer versionedRedisConsumer2 =
        VersionedRedisConsumer.of(channel, "group2", redisConfig, Duration.ofSeconds(10), 5, "2.0");
    //    RedisConsumer redisConsumer = RedisConsumer.of(channel, "group3", redisConfig, Duration.ofSeconds(10), 5);

    new Thread(new MessageConsumer(versionedRedisConsumer1, "group3", 1000, ColorConstants.TEXT_BLUE)).start();

    new Thread(new MessageConsumer(versionedRedisConsumer2, "group3", 1000, ColorConstants.TEXT_PURPLE)).start();

    /* Read via Consumer groups - order is important - Sync processing usecase (Gitsync) */
    //    new Thread(new MessageConsumer(
    //                   redisLocker, redisConfig, "serialConsumerGroups", channel, "group1", 3000,
    //                   ColorConstants.TEXT_BLUE))
    //        .start();
    //    new Thread(new MessageConsumer(
    //                   redisLocker, redisConfig, "serialConsumerGroups", channel, "group1", 1000,
    //                   ColorConstants.TEXT_CYAN))
    //        .start();
    //    new Thread(new MessageConsumer(redisLocker, redisConfig, "serialConsumerGroups", channel, "group1", 500,
    //                   ColorConstants.TEXT_PURPLE))
    //        .start();

    /* Read via Consumer groups - order is not important - Load balancing usecase */
    //        new Thread(new MessageConsumer("consumerGroups", redisConfig, channel, "group2", 1000,
    //        ColorConstants.TEXT_BLUE))
    //            .start();
    //    new Thread(new MessageConsumer("consumerGroups", redisConfig, channel, "group2", 4000,
    //    ColorConstants.TEXT_PURPLE))
    //        .start();

    /* Read messages with the same build version as the consumer */
    //    new Thread(new MessageConsumer("versionedConsumerGroup", redisConfig, channel, "group3", 1000,
    //            ColorConstants.TEXT_BLUE))
    //            .start();

    //    new Thread(new MessageConsumer("versionedConsumerGroup", redisConfig, channel, "group3", 1000,
    //            ColorConstants.TEXT_PURPLE))
    //            .start();

    while (true) {
      Thread.sleep(10000);
    }
  }

  private void registerJerseyFeatures(Environment environment) {
    environment.jersey().register(MultiPartFeature.class);
  }

  private void registerManagedBeans(Environment environment, Injector injector) {
    environment.lifecycle().manage(injector.getInstance(QueueListenerController.class));
  }
}
