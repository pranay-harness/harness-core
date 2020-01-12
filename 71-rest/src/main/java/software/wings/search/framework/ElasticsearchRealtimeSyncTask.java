package software.wings.search.framework;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import software.wings.search.framework.changestreams.ChangeEvent;
import software.wings.search.framework.changestreams.ChangeSubscriber;

import java.util.Queue;

/**
 * This job performs realtime sync of the changes
 * to elasticsearch for the specified classes.
 *
 * @author utkarsh
 */

@Slf4j
public class ElasticsearchRealtimeSyncTask {
  @Inject private ElasticsearchSyncHelper elasticsearchSyncHelper;
  @Inject private ChangeEventProcessor changeEventProcessor;

  private void processChanges(Queue<ChangeEvent<?>> changeEvents) {
    while (!changeEvents.isEmpty()) {
      ChangeEvent<?> changeEvent = changeEvents.poll();
      changeEventProcessor.processChangeEvent(changeEvent);
    }
  }

  private ChangeSubscriber<?> getChangeSubscriber() {
    return changeEvent -> {
      boolean isRunningSuccessfully = changeEventProcessor.processChangeEvent(changeEvent);
      if (!isRunningSuccessfully) {
        stop();
      }
    };
  }

  public boolean run(Queue<ChangeEvent<?>> pendingChangeEvents) throws InterruptedException {
    logger.info("Initializing change listeners for search entities");
    processChanges(pendingChangeEvents);
    elasticsearchSyncHelper.startChangeListeners(getChangeSubscriber());
    changeEventProcessor.startProcessingChangeEvents();
    boolean isAlive = true;
    while (!Thread.currentThread().isInterrupted() && isAlive) {
      Thread.sleep(2000);
      isAlive = elasticsearchSyncHelper.checkIfAnyChangeListenerIsAlive();
      isAlive = isAlive && changeEventProcessor.isAlive();
    }
    return false;
  }

  public void stop() {
    elasticsearchSyncHelper.stopChangeListeners();
    changeEventProcessor.shutdown();
  }
}
