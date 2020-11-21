package io.harness;

import static io.harness.logging.LoggingInitializer.initializeLogging;

import io.harness.eventsframework.Event;
import io.harness.eventsframework.ProjectUpdate;
import io.harness.eventsframework.RedisStreamClient;
import io.harness.eventsframework.StreamChannel;
import io.harness.maintenance.MaintenanceController;
import io.harness.metrics.MetricRegistryModule;
import io.harness.queue.QueueListenerController;
import io.harness.remote.NGObjectMapperHelper;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.redisson.api.StreamInfo;

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
    initializeLogging();
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

    RedisStreamClient client = new RedisStreamClient(appConfig.getEventsFrameworkConfiguration().getRedisConfig());
    String groupName = "group1";
    StreamChannel channel = StreamChannel.PROJECT_UPDATE;

    // ----------------- Perform operations -----------------
    client.createConsumerGroup(channel, groupName);
    //    publishMessages(client, channel);
    readViaConsumerGroups(client, channel, "group1", "cons3");
    //    readViaPubSub(client, channel);
  }

  private void publishMessages(RedisStreamClient client, StreamChannel channel) throws InterruptedException {
    int count = 0;
    while (true) {
      Event projectEvent =
          Event.newBuilder()
              .setAccountId("account2")
              .setPayload(Any.pack(
                  ProjectUpdate.newBuilder().setProjectId(String.valueOf(count)).setDescription("updated").build()))
              .build();
      client.publishEvent(channel, projectEvent);
      count += 1;
      Thread.sleep(1000);
    }
  }
  private void readViaConsumerGroups(RedisStreamClient client, StreamChannel channel, String groupName,
      String consumerName) throws InterruptedException {
    Map<String, Event> getValue;
    SortedSet<String> sortedKeys;
    while (true) {
      getValue = client.readEvent(channel, groupName, consumerName);
      sortedKeys = new TreeSet<String>(getValue.keySet());

      for (String key : sortedKeys) {
        try {
          ProjectUpdate p = getValue.get(key).getPayload().unpack(ProjectUpdate.class);
          log.info("Received from redis - " + consumerName + " - pid: " + p.getProjectId()
              + ", description: " + p.getDescription());
        } catch (InvalidProtocolBufferException e) {
          log.error("Exception in unpacking data for key " + key, e);
        }
        client.acknowledge(channel, groupName, key);
      }

      StreamInfo info = client.getStreamInfo(channel);
      log.info("Total stream length: " + info.getLength());
      log.info("--------------------");
      Thread.sleep(1000);
    }
  }

  private void readViaPubSub(RedisStreamClient client, StreamChannel channel) throws InterruptedException {
    Map<String, Event> getValue;
    SortedSet<String> sortedKeys;
    String lastId = "$";
    while (true) {
      getValue = client.readEvent(channel, lastId);
      sortedKeys = new TreeSet<String>(getValue.keySet());
      for (String key : sortedKeys) {
        try {
          ProjectUpdate p = getValue.get(key).getPayload().unpack(ProjectUpdate.class);
          log.info("Received from redis " + p.getProjectId());
        } catch (InvalidProtocolBufferException e) {
          log.error("Exception in unpacking data for key " + key, e);
        }
        lastId = key;
      }

      StreamInfo info = client.getStreamInfo(channel);
      log.info("Total stream length: " + info.getLength());
      log.info("--------------------");
      Thread.sleep(1000);
    }
  }

  private void registerJerseyFeatures(Environment environment) {
    environment.jersey().register(MultiPartFeature.class);
  }

  private void registerManagedBeans(Environment environment, Injector injector) {
    environment.lifecycle().manage(injector.getInstance(QueueListenerController.class));
  }
}
