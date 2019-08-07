package io.harness.event.client;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;

import io.harness.event.EventPublisherGrpc.EventPublisherBlockingStub;
import io.harness.event.PublishMessage;
import lombok.extern.slf4j.Slf4j;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.impl.RollingChronicleQueue;
import net.openhft.chronicle.wire.DocumentContext;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link EventPublisher} that persists the events to the local disk before publishing it to the server.
 */
@Slf4j
@Singleton
class EventPublisherChronicleImpl extends AbstractPublisherImpl {
  private final RollingChronicleQueue queue;
  private final FileDeletionManager fileDeletionManager;
  private final ExcerptTailer readTailer;

  private long prevIndex;

  @Inject
  EventPublisherChronicleImpl(
      EventPublisherBlockingStub blockingStub, RollingChronicleQueue queue, FileDeletionManager fileDeletionManager) {
    super(blockingStub);
    this.queue = Preconditions.checkNotNull(queue);
    readTailer = queue.createTailer("readTailer");
    this.fileDeletionManager = fileDeletionManager;
    fileDeletionManager.setQueue(queue);
    fileDeletionManager.setSentCycle(readTailer.cycle());
  }

  @Override
  void enqueue(PublishMessage publishMessage) {
    queue.acquireAppender().writeDocument(wire -> wire.getValueOut().bytes(publishMessage.toByteArray()));
  }

  @Override
  List<PublishMessage> tryRead(int maxBatchSize) {
    List<PublishMessage> batchToSend = new ArrayList<>();
    // store index to rewind to, if push to server fails.
    prevIndex = getReadIndex();
    for (int i = 0; i < maxBatchSize; i++) {
      try (DocumentContext dc = readTailer.readingDocument()) {
        if (!dc.isPresent()) {
          break;
        }
        try {
          PublishMessage result = PublishMessage.parseFrom(dc.wire().read().bytes());
          batchToSend.add(result);
        } catch (InvalidProtocolBufferException e) {
          logger.error("Exception while parsing message", e);
        }
      }
    }
    return batchToSend;
  }

  @Override
  void commitRead() {
    fileDeletionManager.setSentCycle(readTailer.cycle());
  }

  @Override
  void revertRead() {
    if (!readTailer.moveToIndex(prevIndex)) {
      logger.error("Invalid moveToIndex: {}", prevIndex);
    }
  }

  @Override
  void close() {
    queue.close();
  }

  private long getReadIndex() {
    long index = readTailer.index();
    if (index == 0) {
      // For some reason, for a newly created tailer, tailer.index() returns 0 instead of the correct index, and the
      // rewind does not work. This is a workaround for that.
      readTailer.toStart();
      return readTailer.index();
    }
    return index;
  }
}
