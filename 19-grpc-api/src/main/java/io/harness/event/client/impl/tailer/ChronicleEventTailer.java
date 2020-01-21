package io.harness.event.client.impl.tailer;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.protobuf.InvalidProtocolBufferException;

import io.harness.event.EventPublisherGrpc.EventPublisherBlockingStub;
import io.harness.event.PublishMessage;
import io.harness.event.PublishRequest;
import lombok.extern.slf4j.Slf4j;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.impl.RollingChronicleQueue;
import net.openhft.chronicle.wire.DocumentContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Tails the chronicle-queue and publishes the events over rpc.
 */
@Slf4j
@Singleton
public class ChronicleEventTailer extends AbstractScheduledService {
  private static final String READ_TAILER = "read-tailer";
  private static final int MAX_BATCH_SIZE = 5000;

  private final EventPublisherBlockingStub blockingStub;
  private final ExcerptTailer readTailer;
  private final FileDeletionManager fileDeletionManager;

  @Inject
  ChronicleEventTailer(EventPublisherBlockingStub blockingStub, @Named("tailer") RollingChronicleQueue chronicleQueue,
      FileDeletionManager fileDeletionManager) {
    this.blockingStub = blockingStub;
    chronicleQueue.refreshDirectlyListing();
    this.readTailer = chronicleQueue.createTailer(READ_TAILER);
    fileDeletionManager.setQueue(chronicleQueue);
    this.fileDeletionManager = fileDeletionManager;
  }

  @Override
  protected void startUp() throws Exception {
    logger.info("Starting up");
  }

  @Override
  protected void shutDown() throws Exception {
    logger.info("Shutting down");
  }

  @Override
  protected void runOneIteration() throws Exception {
    // service will terminate if exception is not caught.
    try {
      List<PublishMessage> batchToSend = new ArrayList<>();
      long prevIndex = getReadIndex();
      for (int i = 0; i < MAX_BATCH_SIZE; i++) {
        try (DocumentContext dc = readTailer.readingDocument()) {
          if (!dc.isPresent()) {
            break;
          }
          try {
            // dc.wire annotated with a different @NonNull annotation.
            PublishMessage result = PublishMessage.parseFrom(requireNonNull(dc.wire()).read().bytes());
            batchToSend.add(result);
          } catch (InvalidProtocolBufferException e) {
            logger.error("Exception while parsing message", e);
          }
        }
      }
      if (!batchToSend.isEmpty()) {
        PublishRequest publishRequest = PublishRequest.newBuilder().addAllMessages(batchToSend).build();
        try {
          blockingStub.withDeadlineAfter(5, TimeUnit.SECONDS).publish(publishRequest);
          logger.debug("Published {} messages successfully", batchToSend.size());
          fileDeletionManager.setSentCycle(readTailer.cycle());
        } catch (Exception e) {
          logger.warn("Exception during message publish", e);
          readTailer.moveToIndex(prevIndex);
          throw e;
        }
      }
    } catch (Exception e) {
      logger.error("Encountered exception", e);
    }
  }

  @Override
  protected Scheduler scheduler() {
    return Scheduler.newFixedDelaySchedule(0, 1, TimeUnit.SECONDS);
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
