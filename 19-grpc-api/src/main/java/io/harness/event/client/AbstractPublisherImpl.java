package io.harness.event.client;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.MoreExecutors;

import io.harness.event.EventPublisherGrpc.EventPublisherBlockingStub;
import io.harness.event.PublishMessage;
import io.harness.event.PublishRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
abstract class AbstractPublisherImpl extends EventPublisher {
  private static final int MAX_BATCH_SIZE = 5000;
  private static final int MAX_DELAY_MILLIS = 1000;

  private final AtomicLong acceptedCount = new AtomicLong();
  private final AtomicLong sentCount = new AtomicLong();

  private final ExecutorService executorService;
  private final AtomicBoolean shutDown = new AtomicBoolean(false);
  private final EventPublisherBlockingStub blockingStub;

  AbstractPublisherImpl(EventPublisherBlockingStub blockingStub) {
    this.blockingStub = blockingStub;
    ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    executorService.scheduleWithFixedDelay(
        this ::publishPending, MAX_DELAY_MILLIS, MAX_DELAY_MILLIS, TimeUnit.MILLISECONDS);
    this.executorService = executorService;
  }

  @Override
  protected final void publish(PublishMessage publishMessage) {
    Preconditions.checkState(!shutDown.get(), "Publisher shut-down. Cannot publish any more messages");
    enqueue(publishMessage);
    acceptedCount.incrementAndGet();
  }

  // Note: This method is not thread-safe - but it's only accessed from the single-threaded executorService
  private void publishPending() {
    logger.debug("Publishing pending messages");
    // Once scheduled, loop until no more pending messages or interrupted during shutdown.
    while (true) {
      if (Thread.interrupted()) {
        logger.info("Publish pending interrupted");
        logger.info("Events accepted: {}, Events sent: {}", acceptedCount.get(), sentCount.get());
        break;
      }
      List<PublishMessage> batchToSend = tryRead(MAX_BATCH_SIZE);
      if (batchToSend.isEmpty()) {
        break;
      }
      PublishRequest publishRequest = PublishRequest.newBuilder().addAllMessages(batchToSend).build();
      try {
        blockingStub.publish(publishRequest);
        logger.debug("Published {} messages successfully", batchToSend.size());
        sentCount.addAndGet(batchToSend.size());
        commitRead();
      } catch (Exception e) {
        logger.warn("Exception during message publish", e);
        revertRead();
      }
    }
  }

  @Override
  public final void shutdown() {
    Preconditions.checkState(!shutDown.getAndSet(true), "Already shut down");
    MoreExecutors.shutdownAndAwaitTermination(executorService, 10, TimeUnit.SECONDS);
    logger.info("Events accepted: {}, Events sent: {}", acceptedCount.get(), sentCount.get());
    close();
  }

  public long getAcceptedCount() {
    return acceptedCount.get();
  }

  public long getSentCount() {
    return sentCount.get();
  }

  // Implementations need to implement the below methods.
  /**
   * Enqueue the message to be published eventually. (should be thread-safe)
   */
  abstract void enqueue(PublishMessage publishMessage);

  /**
   * Read a batch of messages, without removing them from the queue.
   * @param maxBatchSize Maximum number of messages to read.
   * @return list of messages to send (possibly empty)
   */
  abstract List<PublishMessage> tryRead(int maxBatchSize);

  /**
   * Commit the last {@link #tryRead} i.e. remove the read messages from the queue.
   */
  abstract void commitRead();

  /**
   * Revert the last {@link #tryRead}.
   */
  abstract void revertRead();

  /**
   * Close any underlying resources.
   */
  abstract void close();
}
