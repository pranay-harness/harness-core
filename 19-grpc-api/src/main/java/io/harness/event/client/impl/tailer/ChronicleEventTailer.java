package io.harness.event.client.impl.tailer;

import static com.google.common.base.Verify.verify;
import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.event.EventPublisherGrpc.EventPublisherBlockingStub;
import io.harness.event.PublishMessage;
import io.harness.event.PublishRequest;
import io.harness.flow.BackoffScheduler;
import io.harness.logging.LoggingListener;
import lombok.extern.slf4j.Slf4j;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.impl.RollingChronicleQueue;
import net.openhft.chronicle.wire.DocumentContext;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Tails the chronicle-queue and publishes the events over rpc.
 * <p/>
 * Intentionally not thread-safe as CQ tailers are also not thread-safe. The methods in this class, and {@link
 * FileDeletionManager} are supposed to be executed by the same single thread.
 */
@Slf4j
@Singleton
public class ChronicleEventTailer extends AbstractScheduledService {
  private static final String READ_TAILER = "read-tailer";

  // Batching settings
  private static final int MAX_BATCH_COUNT = 5000;
  private static final int MAX_BATCH_BYTES = 1024 * 1024; // 1MiB

  // Delay between successive iterations
  private static final Duration MIN_DELAY = Duration.ofSeconds(1);
  private static final Duration MAX_DELAY = Duration.ofMinutes(5);

  private final ExcerptTailer readTailer;

  private final FileDeletionManager fileDeletionManager;
  private final BackoffScheduler scheduler;
  private final Sampler sampler;

  private final EventPublisherBlockingStub blockingStub;

  private final RollingChronicleQueue queue;

  @Inject
  ChronicleEventTailer(EventPublisherBlockingStub blockingStub, @Named("tailer") RollingChronicleQueue chronicleQueue,
      FileDeletionManager fileDeletionManager) {
    this.blockingStub = blockingStub;
    this.queue = chronicleQueue;
    this.readTailer = chronicleQueue.createTailer(READ_TAILER);
    this.fileDeletionManager = fileDeletionManager;
    this.scheduler = new BackoffScheduler(getClass().getSimpleName(), MIN_DELAY, MAX_DELAY);
    this.sampler = new Sampler(Duration.ofMinutes(1));
    addListener(new LoggingListener(this), MoreExecutors.directExecutor());
  }

  @Override
  protected void startUp() {
    logger.info("Starting up");
    if (fileDeletionManager.getSentIndex() == 0 && readTailer.index() != 0) {
      // Only for migration when readTailer is present, and sentTailer is not.
      logger.info("Index of sent-tailer is 0. Setting it to read-tailer index");
      fileDeletionManager.setSentIndex(readTailer.index());
    }
    printStats();
    fileDeletionManager.deleteOlderFiles();
  }

  @Override
  protected void shutDown() {
    logger.info("Shutting down");
    printStats();
    fileDeletionManager.deleteOlderFiles();
    this.queue.close();
  }

  private void printStats() {
    long readIndex = readTailer.index();
    long sentIndex = fileDeletionManager.getSentIndex();
    long endIndex = queue.createTailer().toEnd().index();
    long excerptCount = queue.countExcerpts(readIndex, endIndex);
    logger.info("index.read-tailer={},  index.sent-tailer={}, index.end={}, excerptCount={}", readIndex, sentIndex,
        endIndex, excerptCount);
  }

  @Override
  protected void runOneIteration() {
    // service will terminate if exception is not caught.
    try {
      sampler.updateTime();
      sampler.sampled(() -> logger.info("Checking for messages to publish"));
      Batch batchToSend = new Batch(MAX_BATCH_BYTES, MAX_BATCH_COUNT);
      while (!batchToSend.isFull()) {
        long endIndex = queue.createTailer().toEnd().index();
        try (DocumentContext dc = readTailer.readingDocument()) {
          if (!dc.isPresent()) {
            sampler.sampled(() -> logger.info("Reached end of queue"));
            long readIndex = readTailer.index();
            if (readIndex < endIndex) {
              readTailer.moveToIndex(endIndex);
              fileDeletionManager.setSentIndex(endIndex);
              logger.warn(
                  "Observed readTailer not at end with no document context. Moved from {} to {}", readIndex, endIndex);
            }
            break;
          }
          try {
            verify(dc.wire() != null, "Null wire with document context present");
            byte[] bytes = requireNonNull(dc.wire()).read().bytes();
            if (bytes != null) {
              PublishMessage message = PublishMessage.parseFrom(bytes);
              batchToSend.add(message);
            } else {
              // could happen in case of an error during append with document context open.
              logger.warn("Read NULL message. Skipping");
            }
          } catch (Exception e) {
            logger.error("Exception while parsing message", e);
          }
        }
      }
      if (batchToSend.isFull()) {
        logger.info("Batch is full");
      }
      if (!batchToSend.isEmpty()) {
        PublishRequest publishRequest = PublishRequest.newBuilder().addAllMessages(batchToSend.getMessages()).build();
        try {
          blockingStub.withDeadlineAfter(30, TimeUnit.SECONDS).publish(publishRequest);
          logger.info("Published {} messages successfully", batchToSend.size());
          fileDeletionManager.setSentIndex(readTailer.index());
          scheduler.recordSuccess();
        } catch (Exception e) {
          logger.warn("Exception during message publish", e);
          QueueUtils.moveToIndex(readTailer, fileDeletionManager.getSentIndex());
          scheduler.recordFailure();
        }
      } else {
        sampler.sampled(() -> logger.info("Skipping message publish as batch is empty"));
      }
    } catch (Exception e) {
      logger.error("Encountered exception", e);
    } finally {
      sampler.sampled(this ::printStats);
      sampler.sampled(fileDeletionManager::deleteOlderFiles);
    }
  }

  @Override
  protected Scheduler scheduler() {
    return scheduler;
  }
}
