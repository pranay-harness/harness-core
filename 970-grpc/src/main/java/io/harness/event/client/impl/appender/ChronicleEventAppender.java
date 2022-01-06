/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.event.client.impl.appender;

import io.harness.event.PublishMessage;
import io.harness.event.client.EventPublisher;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import net.openhft.chronicle.queue.impl.RollingChronicleQueue;
import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.Wire;

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

  // catching throwable is necessary
  @SuppressWarnings({"PMD.AvoidCatchingThrowable", "squid:S1181"})
  @Override
  protected final void publish(PublishMessage publishMessage) {
    Preconditions.checkState(!shutDown.get(), "Publisher shut-down. Cannot publish any more messages");
    if (!queueMonitor.isHealthy()) {
      log.warn("Dropping message as queue is not healthy");
      return;
    }
    byte[] bytes = publishMessage.toByteArray();
    DocumentContext dc = queue.acquireAppender().writingDocument();
    try {
      Wire wire = dc.wire();
      if (wire != null) {
        wire.getValueOut().bytes(bytes);
      }
    } catch (Throwable t) {
      dc.rollbackOnClose();
      throw t;
    } finally {
      dc.close();
    }
  }

  @Override
  public final void shutdown() {
    if (!shutDown.getAndSet(true)) {
      log.info("Shutting down publisher");
      queueMonitor.stopAsync().awaitTerminated();
      queue.close();
    }
  }
}
