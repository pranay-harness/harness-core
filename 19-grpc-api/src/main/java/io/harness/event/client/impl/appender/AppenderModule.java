package io.harness.event.client.impl.appender;

import static com.google.inject.name.Names.named;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provider;
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
import java.util.function.Supplier;

@Slf4j
public class AppenderModule extends AbstractModule {
  private final Config config;
  private final Supplier<String> delegateIdSupplier;

  public AppenderModule(Config config, Supplier<String> delegateIdSupplier) {
    this.config = config;
    this.delegateIdSupplier = delegateIdSupplier;
  }

  @Singleton
  static class NoopEventPublisher extends EventPublisher {
    NoopEventPublisher(Supplier<String> delegateIdSupplier) {
      super(delegateIdSupplier);
    }

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
      bind(EventPublisher.class).toProvider(() -> new NoopEventPublisher(delegateIdSupplier)).in(Singleton.class);
    } else {
      Provider<RollingChronicleQueue> queueProvider =
          getProvider(Key.get(RollingChronicleQueue.class, named("appender")));
      Provider<ChronicleQueueMonitor> queueMonitorProvider = getProvider(ChronicleQueueMonitor.class);
      bind(EventPublisher.class)
          .toProvider(
              () -> new ChronicleEventAppender(queueProvider.get(), queueMonitorProvider.get(), delegateIdSupplier))
          .in(Singleton.class);
    }
  }

  @Value
  @Builder
  public static class Config {
    String queueFilePath;
  }
}
