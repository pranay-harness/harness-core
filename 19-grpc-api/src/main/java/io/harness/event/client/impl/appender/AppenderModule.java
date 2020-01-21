package io.harness.event.client.impl.appender;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.event.PublishMessage;
import io.harness.event.client.EventPublisher;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.RollCycles;
import net.openhft.chronicle.queue.impl.RollingChronicleQueue;

import java.time.Duration;

@Slf4j
public class AppenderModule extends AbstractModule {
  private final Config config;

  public AppenderModule(Config config) {
    this.config = config;
  }

  @Singleton
  static class NoopEventPublisher extends EventPublisher {
    @Override
    protected void publish(PublishMessage publishMessage) {
      // No-op
    }
  }

  @Provides
  @Singleton
  @Named("appender")
  RollingChronicleQueue chronicleQueue() {
    return ChronicleQueue.singleBuilder(config.queueFilePath)
        .rollCycle(RollCycles.MINUTELY)
        .timeoutMS(Duration.ofSeconds(30).toMillis())
        .build();
  }

  @Override
  protected void configure() {
    if (config.queueFilePath == null) {
      // EventPublisher optional for delegate start-up
      logger.info("EventPublisher configuration not present. Injecting Noop publisher");
      bind(EventPublisher.class).to(NoopEventPublisher.class);
    } else {
      bind(EventPublisher.class).to(ChronicleEventAppender.class);
    }
  }

  @Value
  @Builder
  public static class Config {
    String queueFilePath;
  }
}
