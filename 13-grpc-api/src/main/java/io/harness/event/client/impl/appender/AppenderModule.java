package io.harness.event.client.impl.appender;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.event.client.EventPublisher;
import io.harness.event.client.impl.EventPublisherConstants;
import io.harness.govern.ProviderModule;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.impl.RollingChronicleQueue;

import java.util.function.Supplier;

@Slf4j
public class AppenderModule extends ProviderModule {
  private final Config config;
  private final Supplier<String> delegateIdSupplier;

  public AppenderModule(Config config, Supplier<String> delegateIdSupplier) {
    this.config = config;
    this.delegateIdSupplier = delegateIdSupplier;
  }

  @Provides
  @Singleton
  @Named("appender")
  RollingChronicleQueue chronicleQueue() {
    return ChronicleQueue.singleBuilder(config.queueFilePath)
        .rollCycle(EventPublisherConstants.QUEUE_ROLL_CYCLE)
        .timeoutMS(EventPublisherConstants.QUEUE_TIMEOUT_MS)
        .build();
  }

  @Provides
  @Singleton
  private EventPublisher chronicleEventPublisher(
      @Named("appender") RollingChronicleQueue queue, ChronicleQueueMonitor queueMonitor) {
    return new ChronicleEventAppender(queue, queueMonitor, delegateIdSupplier);
  }

  @Value
  @Builder
  public static class Config {
    String queueFilePath;
  }
}
