package software.wings.search.framework;

import com.google.inject.Inject;
import com.google.inject.Provider;

import lombok.extern.slf4j.Slf4j;
import software.wings.dl.WingsPersistence;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * The job responsible to maintain sync
 * between application's persistent layer and
 * elasticsearch db at all times.
 *
 * @author utkarsh
 */

@Slf4j
public class ElasticsearchSyncJob implements Runnable {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private Provider<ElasticsearchBulkSyncTask> elasticsearchBulkSyncTaskProvider;
  @Inject private Provider<ElasticsearchRealtimeSyncTask> elasticsearchRealtimeSyncTaskProvider;
  @Inject private Provider<PerpetualSearchLocker> perpetualSearchLockerProvider;
  private ElasticsearchRealtimeSyncTask elasticsearchRealtimeSyncTask;
  private ScheduledExecutorService scheduledExecutorService;
  private ScheduledFuture searchLock;

  public void run() {
    try {
      PerpetualSearchLocker perpetualSearchLocker = perpetualSearchLockerProvider.get();
      ElasticsearchBulkSyncTask elasticsearchBulkSyncTask = elasticsearchBulkSyncTaskProvider.get();
      scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
      elasticsearchRealtimeSyncTask = elasticsearchRealtimeSyncTaskProvider.get();
      String uuid = UUID.randomUUID().toString();

      searchLock = perpetualSearchLocker.acquireLock(ElasticsearchSyncJob.class.getName(), uuid, this ::stop);
      logger.info("Starting search synchronization now");

      SearchSyncHeartbeat searchSyncHeartbeat =
          new SearchSyncHeartbeat(wingsPersistence, ElasticsearchSyncJob.class.getName(), uuid);
      scheduledExecutorService.scheduleAtFixedRate(searchSyncHeartbeat, 30, 30, TimeUnit.MINUTES);

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
    logger.info("Cancelling search monitor future");
    if (searchLock != null) {
      searchLock.cancel(true);
    }
    scheduledExecutorService.shutdownNow();
    logger.info("Stopping realtime synchronization");
    elasticsearchRealtimeSyncTask.stop();
  }
}
