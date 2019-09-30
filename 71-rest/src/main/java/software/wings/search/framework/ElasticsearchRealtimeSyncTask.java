package software.wings.search.framework;

import lombok.extern.slf4j.Slf4j;
import software.wings.search.framework.changestreams.ChangeEvent;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * This job performs realtime sync of the changes
 * to elasticsearch for the specified classes.
 *
 * @author utkarsh
 */

@Slf4j
public class ElasticsearchRealtimeSyncTask extends ElasticsearchSyncTask {
  public boolean run() {
    logger.info("Initializing change listeners for search entities");

    try {
      Future<?> f = super.initializeChangeListeners();
      f.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      logger.error("Realtime sync stopped. stopping change listeners", e);
    } catch (ExecutionException e) {
      logger.error("Error occured during realtime sync", e);
    } finally {
      stopChangeListeners();
    }
    return false;
  }

  public void onChange(ChangeEvent<?> changeEvent) {
    boolean isRunningSuccessfully = processChange(changeEvent);
    if (!isRunningSuccessfully) {
      logger.error("Realtime sync failed. Stopping change listeners");
      super.stopChangeListeners();
    }
  }
}
