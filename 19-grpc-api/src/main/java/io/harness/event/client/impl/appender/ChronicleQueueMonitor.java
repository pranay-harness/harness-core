package io.harness.event.client.impl.appender;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
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
public class ChronicleQueueMonitor extends AbstractScheduledService {
  private final RollingChronicleQueue queue;

  @Inject
  public ChronicleQueueMonitor(@Named("appender") RollingChronicleQueue queue) {
    this.queue = queue;
  }

  @Override
  protected void runOneIteration() throws Exception {
    logger.info("entryCount: {}", queue.entryCount());
    logger.info("fileCount: {}",
        Optional.ofNullable(queue.file())
            .map(File::listFiles)
            .map(fileList -> fileList.length - 1) // -1 to exclude the metadata file
            .orElse(0));
  }

  @Override
  protected Scheduler scheduler() {
    return Scheduler.newFixedDelaySchedule(1, 1, TimeUnit.MINUTES);
  }
}
