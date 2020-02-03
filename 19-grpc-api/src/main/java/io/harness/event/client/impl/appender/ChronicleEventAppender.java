package io.harness.event.client.impl.appender;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.event.PublishMessage;
import io.harness.event.client.EventPublisher;
import lombok.extern.slf4j.Slf4j;
import net.openhft.chronicle.queue.impl.RollingChronicleQueue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * {@link EventPublisher} that appends the events to a chronicle-queue.
 */
@Slf4j
@Singleton
class ChronicleEventAppender extends EventPublisher {
  private final RollingChronicleQueue queue;
  private final AtomicBoolean shutDown;
  private final ChronicleQueueMonitor queueMonitor;

  @Inject
  ChronicleEventAppender(@Named("appender") RollingChronicleQueue queue, ChronicleQueueMonitor queueMonitor,
      Supplier<String> delegateIdSupplier) {
    super(delegateIdSupplier);
    this.queue = Preconditions.checkNotNull(queue);
    this.queueMonitor = queueMonitor;
    this.shutDown = new AtomicBoolean(false);
    queueMonitor.startAsync().awaitRunning();
  }

  @Override
  protected final void publish(PublishMessage publishMessage) {
    Preconditions.checkState(!shutDown.get(), "Publisher shut-down. Cannot publish any more messages");
    if (!queueMonitor.isHealthy()) {
      logger.warn("Dropping message as queue is not healthy");
      return;
    }
    queue.acquireAppender().writeDocument(wire -> wire.getValueOut().bytes(publishMessage.toByteArray()));
  }

  @Override
  public final void shutdown() {
    if (!shutDown.getAndSet(true)) {
      logger.info("Shutting down publisher");
      queueMonitor.stopAsync().awaitTerminated();
      queue.close();
    }
  }
}
