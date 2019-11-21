package software.wings.search.framework;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import software.wings.search.framework.changestreams.ChangeEvent;
import software.wings.search.framework.changestreams.ChangeSubscriber;

import java.time.Duration;
import java.time.Instant;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * This job performs realtime sync of the changes
 * to elasticsearch for the specified classes.
 *
 * @author utkarsh
 */

@Slf4j
public class ElasticsearchRealtimeSyncTask {
  @Inject private ElasticsearchSyncHelper elasticsearchSyncHelper;
  private Future<?> changeListenersFuture;

  private void processChanges(Queue<ChangeEvent<?>> changeEvents) {
    while (!changeEvents.isEmpty()) {
      ChangeEvent<?> changeEvent = changeEvents.poll();
      elasticsearchSyncHelper.processChange(changeEvent);
    }
  }

  private ChangeSubscriber<?> getChangeSubscriber() {
    return changeEvent -> {
      Instant start = Instant.now();
      boolean isRunningSuccessfully = elasticsearchSyncHelper.processChange(changeEvent);
      long timeTaken = Duration.between(Instant.now(), start).toMillis();
      logger.info("ChangeEvent {} of type {}:{} took {} ms to process", changeEvent.getUuid(),
          changeEvent.getEntityType(), changeEvent.getChangeType(), timeTaken);
      if (!isRunningSuccessfully) {
        elasticsearchSyncHelper.stopChangeListeners();
      }
    };
  }

  public boolean run(Queue<ChangeEvent<?>> pendingChangeEvents) {
    logger.info("Initializing change listeners for search entities");
    try {
      processChanges(pendingChangeEvents);
      changeListenersFuture = elasticsearchSyncHelper.startChangeListeners(getChangeSubscriber());
      changeListenersFuture.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      logger.error("Realtime sync stopped. stopping change listeners", e);
    } catch (CancellationException e) {
      logger.error("Realtime sync stopped due to changeListeners being cancelled", e);
    } catch (ExecutionException e) {
      logger.error("Error occured during realtime sync", e);
    } finally {
      stop();
    }
    return false;
  }

  public void stop() {
    elasticsearchSyncHelper.stopChangeListeners();
    changeListenersFuture.cancel(true);
  }
}
