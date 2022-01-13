/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.event.client.impl.tailer;

import static com.google.common.base.Preconditions.checkArgument;
import static net.openhft.chronicle.queue.impl.single.SingleChronicleQueue.SUFFIX;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.impl.RollingChronicleQueue;
import net.openhft.chronicle.queue.impl.RollingResourcesCache;

/**
 * Responsible for deleting old queue files.
 */
@Singleton
@Slf4j
class FileDeletionManager {
  private static final String SENT_TAILER = "sent-tailer";

  private final RollingChronicleQueue queue;
  private final RollingResourcesCache fileIdMapper;
  private final ExcerptTailer sentTailer;

  @Inject
  FileDeletionManager(@Named("tailer") RollingChronicleQueue queue) {
    this.queue = queue;
    this.fileIdMapper = new RollingResourcesCache(
        queue.rollCycle(), queue.epoch(), name -> new File(queue.fileAbsolutePath(), name + SUFFIX), file -> {
          String s = file.getName();
          return s.substring(0, s.length() - SUFFIX.length());
        });
    sentTailer = queue.createTailer(SENT_TAILER);
  }

  /*
  Implementation Note:
  In CQ, index of an entry has 2 components - cycle & sequenceNumber. The cycle maps 1-1 to the actual file in which the
  entry is stored, and sequenceNumber is the position of the entry within this file.
  The cycle of an entry depends on the time at which the entry was appended and the configured RollCycle (eg: for
  minutely RollCycle, the cycle that an appended entry will be having will change on minute edges)
   */

  /**
   * Delete files older than the file sentTailer is on.
   */
  void deleteOlderFiles() {
    log.info("Checking for old queue files to be deleted");
    long cycle = sentTailer.cycle();
    boolean anyFilesDeleted = deleteOlderFilesInternal(cycle);
    // This method needs to be called to update internal caches whenever we delete any queue files.
    if (anyFilesDeleted) {
      queue.refreshDirectoryListing();
    }
  }

  private boolean deleteOlderFilesInternal(long cycle) {
    File[] filesInDir = queue.file().listFiles();
    if (filesInDir == null) {
      return false;
    }
    boolean anyFilesDeleted = false;
    List<File> olderFiles = Arrays.stream(filesInDir)
                                .filter(file -> file.getName().endsWith(SUFFIX))
                                // < instead of <= because we might want to rewind to previous file
                                // i.e. there'll always be minimum 2 files in the queue.
                                .filter(file -> fileIdMapper.toLong(file) < cycle)
                                .sorted()
                                .collect(Collectors.toList());
    if (!olderFiles.isEmpty()) {
      log.info("Deleting files {}", olderFiles);
      for (File file : olderFiles) {
        try {
          anyFilesDeleted = Files.deleteIfExists(file.toPath()) || anyFilesDeleted;
        } catch (Exception e) {
          log.error("Failed to delete {}", file.toPath(), e);
        }
      }
    }
    return anyFilesDeleted;
  }

  void setSentIndex(long index) {
    checkArgument(index >= getSentIndex(), "sent-tailer should not be rewinded");
    QueueUtils.moveToIndex(sentTailer, index);
  }

  long getSentIndex() {
    return sentTailer.index();
  }
}
