package io.harness.event.client;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.event.EventPublisherGrpc.EventPublisherBlockingStub;
import io.harness.event.PublishMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@Singleton
class EventPublisherImpl extends AbstractPublisherImpl {
  private static final int MAX_QUEUE_SIZE = 5000;
  private final BlockingQueue<PublishMessage> pendingPublishes;

  private int readCount;

  @Inject
  EventPublisherImpl(EventPublisherBlockingStub blockingStub) {
    super(blockingStub);
    pendingPublishes = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
  }

  @Override
  void enqueue(PublishMessage publishMessage) {
    try {
      pendingPublishes.put(publishMessage);
    } catch (InterruptedException e) {
      logger.warn("Publishing thread interrupted");
      Thread.currentThread().interrupt();
    }
  }

  @Override
  List<PublishMessage> tryRead(int maxBatchSize) {
    List<PublishMessage> batchToSend = new ArrayList<>();
    Iterator<PublishMessage> iterator = pendingPublishes.iterator();
    for (int i = 0; i < maxBatchSize && iterator.hasNext(); i++) {
      batchToSend.add(iterator.next());
    }
    readCount = batchToSend.size();
    return batchToSend;
  }

  @Override
  void commitRead() {
    pendingPublishes.drainTo(new ArrayList<>(), readCount);
  }

  @Override
  void revertRead() {
    // Nothing to do here.
  }

  @Override
  void close() {
    // Nothing to do here.
  }
}
