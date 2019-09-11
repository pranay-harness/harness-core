package software.wings.search.framework;

import lombok.extern.slf4j.Slf4j;
import software.wings.search.framework.changestreams.ChangeEvent;

/**
 * This job performs realtime sync of the changes
 * to elasticsearch for the specified classes.
 *
 * @author utkarsh
 */

@Slf4j
public class ElasticsearchRealtimeSyncTask extends ElasticsearchSyncTask {
  private static final Thread syncThread = Thread.currentThread();

  public boolean run() {
    logger.info("Initializing change listeners for search entities");

    super.initializeChangeListeners();

    try {
      latch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      logger.error("Realtime sync stopped. stopping change listeners", e);
      stopChangeListeners();
      return false;
    }
    return true;
  }

  public void handleChangeEvent(ChangeEvent changeEvent) {
    boolean isRunningSuccessfully = processChange(changeEvent);
    if (!isRunningSuccessfully) {
      syncThread.interrupt();
    }
  }
}
