package software.wings.search.framework;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
import java.util.concurrent.ScheduledFuture;

/**
 * The job responsible to maintain sync
 * between application's persistent layer and
 * elasticsearch db at all times.
 *
 * @author utkarsh
 */

@Slf4j
public class ElasticsearchSyncJob implements Runnable {
  @Inject private ElasticsearchBulkSyncTask elasticsearchBulkSyncTask;
  @Inject private ElasticsearchRealtimeSyncTask elasticsearchRealtimeSyncTask;
  @Inject private PerpetualSearchLocker perpetualSearchLocker;
  private ScheduledFuture scheduledFuture;

  public void run() {
    String uuid = UUID.randomUUID().toString();
    try {
      scheduledFuture = perpetualSearchLocker.tryToAcquireLock(ElasticsearchSyncJob.class.getName(), uuid, this ::stop);
      logger.info("Starting search synchronization now");

      boolean isBulkMigrated = elasticsearchBulkSyncTask.run();
      if (isBulkMigrated) {
        elasticsearchRealtimeSyncTask.run();
      }
    } catch (RuntimeException | InterruptedException e) {
      logger.error("Search Sync Job unexpectedly stopped", e);
    } finally {
      logger.info("Search sync job has stopped");
      stop();
    }
  }

  public void stop() {
    if (scheduledFuture != null) {
      logger.info("Cancelling search monitor future");
      scheduledFuture.cancel(true);
    }
    elasticsearchRealtimeSyncTask.stopChangeListeners();
  }
}
