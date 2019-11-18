package io.harness.event.client.impl.tailer;

import static net.openhft.chronicle.queue.impl.single.SingleChronicleQueue.SUFFIX;

import com.google.common.base.Preconditions;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import net.openhft.chronicle.queue.impl.RollingChronicleQueue;
import net.openhft.chronicle.queue.impl.RollingResourcesCache;
import net.openhft.chronicle.queue.impl.StoreFileListener;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Responsible for deleting old queue files, whose data has already been pushed to server. This is {@link
 * StoreFileListener} so onReleased will be called whenever a file is released by an appender/tailer.
 */
@Singleton
@Slf4j
class FileDeletionManager implements StoreFileListener {
  private volatile RollingChronicleQueue queue;
  private volatile RollingResourcesCache fileIdMapper;
  private volatile int sentCycle;

  void setSentCycle(int sentCycle) {
    this.sentCycle = sentCycle;
  }

  void setQueue(RollingChronicleQueue queue) {
    this.queue = queue;
    // Re-using the rolling mechanism used within the chronicle queue to map file names to timestamps.
    this.fileIdMapper = new RollingResourcesCache(
        queue.rollCycle(), queue.epoch(), name -> new File(queue.fileAbsolutePath(), name + SUFFIX), file -> {
          String s = file.getName();
          return s.substring(0, s.length() - SUFFIX.length());
        });
  }

  @Override
  public void onReleased(int cycle, File releasedFile) {
    Preconditions.checkNotNull(queue);
    if (sentCycle >= cycle) {
      deleteOlderFiles(releasedFile);
    }
  }

  private void deleteOlderFiles(File releasedFile) {
    File[] filesInDir = queue.file().listFiles();
    if (filesInDir == null) {
      return;
    }
    List<File> olderFiles = Arrays.stream(filesInDir)
                                .filter(file -> file.getName().endsWith(SUFFIX))
                                .filter(file -> fileIdMapper.toLong(file) < fileIdMapper.toLong(releasedFile))
                                .collect(Collectors.toList());
    logger.info("Deleting files {}", olderFiles);
    for (File file : olderFiles) {
      try {
        Files.deleteIfExists(file.toPath());
      } catch (Exception e) {
        logger.error("Failed to delete {}", file.toPath(), e);
      }
    }
  }
}
