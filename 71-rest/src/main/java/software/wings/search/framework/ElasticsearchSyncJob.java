package software.wings.search.framework;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
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
  private PerpetualSearchLocker perpetualSearchLocker;
  private ElasticsearchRealtimeSyncTask elasticsearchRealtimeSyncTask;
  private ScheduledExecutorService scheduledExecutorService;
  private ScheduledFuture searchLock;

  @Override
  public void run() {
    try {
      ElasticsearchBulkSyncTask elasticsearchBulkSyncTask = elasticsearchBulkSyncTaskProvider.get();
      perpetualSearchLocker = perpetualSearchLockerProvider.get();
      scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(
          new ThreadFactoryBuilder().setNameFormat("search-heartbeat").build());
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
    logger.info("Stopping realtime synchronization");
    elasticsearchRealtimeSyncTask.stop();
    scheduledExecutorService.shutdownNow();
    perpetualSearchLocker.shutdown();
  }
}
