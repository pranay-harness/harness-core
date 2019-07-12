package io.harness.event.client;

import com.google.common.base.Preconditions;

import io.harness.event.EventPublisherGrpc.EventPublisherBlockingStub;
import io.harness.event.PublishMessage;
import io.harness.event.PublishRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class EventPublisherImpl implements EventPublisher {
  private static final int MAX_PENDING_MESSAGES = 5000;
  private final EventPublisherBlockingStub blockingStub;
  private final BlockingQueue<PublishMessage> pendingPublishes;

  private final AtomicBoolean shutDown = new AtomicBoolean(false);

  public EventPublisherImpl(EventPublisherBlockingStub blockingStub) {
    this.blockingStub = Preconditions.checkNotNull(blockingStub, "blockingStub");
    pendingPublishes = new LinkedBlockingQueue<>(MAX_PENDING_MESSAGES);
    Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(this ::publishPending, 1, 1, TimeUnit.SECONDS);
  }

  @Override
  public void publish(PublishMessage publishMessage) {
    Preconditions.checkState(!shutDown.get(), "Publisher shut-down. Cannot publish");
    try {
      pendingPublishes.put(publishMessage);
    } catch (InterruptedException e) {
      logger.warn("Publishing thread interrupted");
      Thread.currentThread().interrupt();
    }
  }

  // Publishes the pending messages
  private void publishPending() {
    List<PublishMessage> toPublishNow = new ArrayList<>();
    pendingPublishes.drainTo(toPublishNow);
    try {
      PublishRequest publishRequest = PublishRequest.newBuilder().addAllMessages(toPublishNow).build();
      blockingStub.publish(publishRequest);
      logger.info("Published {} events successfully", toPublishNow.size());
    } catch (Exception e) {
      logger.warn("Exception during publish", e);
      logger.warn("Publish failed. Dropped {} events", toPublishNow.size());
    }
  }

  @Override
  public void shutdown() {
    Preconditions.checkState(!shutDown.getAndSet(true), "Already shut down");
    publishPending();
  }
}
