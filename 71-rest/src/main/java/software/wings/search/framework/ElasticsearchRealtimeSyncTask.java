package software.wings.search.framework;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import software.wings.search.framework.changestreams.ChangeEvent;
import software.wings.search.framework.changestreams.ChangeSubscriber;

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
  @Inject private ElasticsearchChangeEventProcessor elasticsearchChangeEventProcessor;
  private Future<?> changeListenersFuture;

  private void processChanges(Queue<ChangeEvent<?>> changeEvents) {
    while (!changeEvents.isEmpty()) {
      ChangeEvent<?> changeEvent = changeEvents.poll();
      elasticsearchChangeEventProcessor.processChange(changeEvent);
    }
  }

  private ChangeSubscriber<?> getChangeSubscriber() {
    return changeEvent -> {
      boolean isRunningSuccessfully = elasticsearchChangeEventProcessor.processChange(changeEvent);
      if (!isRunningSuccessfully) {
        stop();
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
      logger.error("Realtime sync interrupted. stopping change listeners", e);
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
    elasticsearchChangeEventProcessor.shutdown();
    changeListenersFuture.cancel(true);
  }
}
