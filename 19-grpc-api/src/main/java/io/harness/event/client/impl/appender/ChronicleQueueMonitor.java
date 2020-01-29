package io.harness.event.client.impl.appender;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import lombok.extern.slf4j.Slf4j;
import net.openhft.chronicle.queue.impl.RollingChronicleQueue;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Monitors queue stats.
 */
@Slf4j
@Singleton
public class ChronicleQueueMonitor extends AbstractScheduledService {
  // Since the queue is configured with RollCycles.MINUTELY, each file correspond to one minute of data.
  public static final int THRESHOLD = 30;

  private final RollingChronicleQueue queue;

  private volatile boolean healthy = true;

  @Inject
  public ChronicleQueueMonitor(@Named("appender") RollingChronicleQueue queue) {
    this.queue = queue;
  }

  @Override
  protected void runOneIteration() throws Exception {
    long entryCount = queue.entryCount();
    int fileCount = Optional.ofNullable(queue.file())
                        .map(File::listFiles)
                        .map(fileList -> fileList.length - 1) // -1 to exclude the metadata file
                        .orElse(0);
    logger.info("entryCount: {}, fileCount: {}", entryCount, fileCount);
    if (fileCount >= THRESHOLD) {
      logger.warn("EventQueue file count on delegate is too high. Marking unhealthy");
      healthy = false;
    } else if (!healthy) {
      logger.info("Event queue recovered. Marking healthy.");
      healthy = true;
    }
  }

  @Override
  protected Scheduler scheduler() {
    return Scheduler.newFixedDelaySchedule(1, 1, TimeUnit.MINUTES);
  }

  public boolean isHealthy() {
    return healthy;
  }
}
