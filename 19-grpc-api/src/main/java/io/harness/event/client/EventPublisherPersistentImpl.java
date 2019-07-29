package io.harness.event.client;

import com.google.common.base.Preconditions;

import com.squareup.tape2.ObjectQueue;
import io.harness.event.EventPublisherGrpc.EventPublisherBlockingStub;
import io.harness.event.PublishMessage;
import io.harness.event.PublishRequest;
import io.harness.exception.WingsException;
import io.harness.manage.ManagedScheduledExecutorService;
import lombok.extern.slf4j.Slf4j;
import net.jcip.annotations.GuardedBy;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
// TODO - Handle the scenario of a lack of space on disk.
/**
 * {@link EventPublisher} that persists the events to the local disk before publishing it to the server.
 */
@Slf4j
public class EventPublisherPersistentImpl implements EventPublisher {
  private static final int MAX_BATCH_SIZE = 5000;
  private final EventPublisherBlockingStub blockingStub;

  // ObjectQueue is not thread-safe.
  @GuardedBy("lock") private final ObjectQueue<PublishMessage> pendingPublishes;
  private final Object lock = new Object();

  private final AtomicBoolean shutDown = new AtomicBoolean(false);

  private final ManagedScheduledExecutorService executorService;

  public EventPublisherPersistentImpl(
      EventPublisherBlockingStub blockingStub, ObjectQueue<PublishMessage> pendingPublishes) {
    this.blockingStub = Preconditions.checkNotNull(blockingStub, "blockingStub");
    this.pendingPublishes = pendingPublishes;
    executorService = new ManagedScheduledExecutorService("grpc-event-publisher");
    executorService.scheduleWithFixedDelay(this ::publishPending, 1, 1, TimeUnit.SECONDS);
  }

  @Override
  public void publish(PublishMessage publishMessage) {
    Preconditions.checkState(!shutDown.get(), "Publisher shut-down. Cannot publish any more messages");
    try {
      synchronized (lock) {
        pendingPublishes.add(publishMessage);
      }
    } catch (IOException e) {
      logger.warn("Persisting event failed");
      throw new WingsException(e);
    }
  }

  private boolean hasPendingMessages() {
    synchronized (lock) {
      return !pendingPublishes.isEmpty();
    }
  }

  private void publishPending() {
    logger.info("Publishing pending messages");
    // Once scheduled on the executor, keep running until all pending messages are published.
    while (hasPendingMessages()) {
      if (Thread.interrupted()) {
        // To exit out of loop during shutdown
        logger.info("Publishing interrupted");
        break;
      }
      try {
        int batchSize;
        List<PublishMessage> currentBatch;
        synchronized (lock) {
          batchSize = Math.min(MAX_BATCH_SIZE, pendingPublishes.size());
          currentBatch = pendingPublishes.peek(batchSize);
        }
        PublishRequest publishRequest = PublishRequest.newBuilder().addAllMessages(currentBatch).build();
        blockingStub.publish(publishRequest);
        logger.debug("Published {} messages successfully", batchSize);
        synchronized (lock) {
          pendingPublishes.remove(batchSize);
        }
        logger.debug("{} messages removed from queue", batchSize);
      } catch (Exception e) {
        logger.warn("Exception during message publish", e);
      }
    }
  }

  @Override
  public void shutdown() {
    Preconditions.checkState(!shutDown.getAndSet(true), "Already shut down");
    try {
      executorService.shutdownNow();
      try {
        executorService.awaitTermination(5, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    } finally {
      try {
        pendingPublishes.close();
      } catch (IOException e) {
        logger.error("Exception while closing the queue file", e);
      }
    }
  }
}
