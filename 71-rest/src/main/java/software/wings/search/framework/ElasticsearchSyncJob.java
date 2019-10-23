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
    try {
      String uuid = UUID.randomUUID().toString();
      scheduledFuture = perpetualSearchLocker.acquireLock(ElasticsearchSyncJob.class.getName(), uuid, this ::stop);
      logger.info("Starting search synchronization now");

      ElasticsearchBulkSyncTaskResult elasticsearchBulkSyncTaskResult = elasticsearchBulkSyncTask.run();
      if (elasticsearchBulkSyncTaskResult.isSuccessful()) {
        elasticsearchRealtimeSyncTask.run(elasticsearchBulkSyncTaskResult.getChangeEventsDuringBulkSync());
      }
    } catch (RuntimeException e) {
      logger.error("Search Sync Job unexpectedly stopped", e);
    } catch (InterruptedException e) {
      logger.error("Search Sync job interrupted", e);
      Thread.currentThread().interrupt();
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
    elasticsearchRealtimeSyncTask.stop();
  }
}
