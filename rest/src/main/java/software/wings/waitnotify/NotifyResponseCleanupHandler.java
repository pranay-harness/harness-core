package software.wings.waitnotify;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;
import static software.wings.core.maintenance.MaintenanceController.isMaintenance;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.FindOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Base;
import software.wings.core.managerConfiguration.ConfigurationController;
import software.wings.dl.WingsPersistence;
import software.wings.sm.ExecutionStatus;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * This is meant to cleanup notifyResponse objects that have already been used to callback waiting
 * instances, or have expired.
 *
 * @author Rishi
 */
public final class NotifyResponseCleanupHandler implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(NotifyResponseCleanupHandler.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private ConfigurationController configurationController;

  /* (non-Javadoc)
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    if (isMaintenance() || configurationController.isNotPrimary()) {
      return;
    }

    execute();
  }

  public void execute() {
    try {
      // Get all successful notify responses
      final List<Key<NotifyResponse>> keys = wingsPersistence.createQuery(NotifyResponse.class, excludeAuthority)
                                                 .filter(NotifyResponse.STATUS_KEY, ExecutionStatus.SUCCESS)
                                                 .field(Base.CREATED_AT_KEY)
                                                 .lessThan(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(1))
                                                 .asKeyList(new FindOptions().limit(2500));
      if (isEmpty(keys)) {
        logger.debug("There are no NotifyResponse entries to cleanup");
        return;
      }

      Set<String> notifyResponseUuids =
          keys.stream().map(notifyResponseKey -> notifyResponseKey.getId().toString()).collect(Collectors.toSet());

      Set<String> waitQueueCorrelationIds = new HashSet<>();
      try (HIterator<WaitQueue> waitQueues =
               new HIterator<>(wingsPersistence.createQuery(WaitQueue.class, excludeAuthority)
                                   .project(WaitQueue.CORRELATION_ID_KEY, true)
                                   .field(WaitQueue.CORRELATION_ID_KEY)
                                   .in(notifyResponseUuids)
                                   .fetch())) {
        while (waitQueues.hasNext()) {
          waitQueueCorrelationIds.add(waitQueues.next().getCorrelationId());
        }
      }

      Set<String> toBeDeletedResponseIds = new HashSet<>(notifyResponseUuids);
      toBeDeletedResponseIds.removeAll(waitQueueCorrelationIds);

      if (isNotEmpty(toBeDeletedResponseIds)) {
        wingsPersistence.delete(
            wingsPersistence.createQuery(NotifyResponse.class).field(Base.ID_KEY).in(toBeDeletedResponseIds));
      }
    } catch (Exception exception) {
      logger.error("Error in NotifyResponseCleanupHandler", exception);
    }
  }
}
