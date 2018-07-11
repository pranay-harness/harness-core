package software.wings.waitnotify;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Lists.newArrayList;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.core.maintenance.MaintenanceController.isMaintenance;
import static software.wings.dl.HQuery.excludeAuthority;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.SearchFilter.Operator;
import software.wings.core.managerConfiguration.ConfigurationController;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.sm.ExecutionStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
      PageResponse<NotifyResponse> notifyPageResponses = wingsPersistence.query(NotifyResponse.class,
          aPageRequest().addFilter("status", Operator.EQ, ExecutionStatus.SUCCESS).addFieldsIncluded(ID_KEY).build(),
          excludeAuthority);
      if (isEmpty(notifyPageResponses)) {
        logger.debug("There are no NotifyResponse entries to cleanup");
        return;
      }

      List<String> correlationIds = notifyPageResponses.stream().map(NotifyResponse::getUuid).collect(toList());
      PageResponse<WaitQueue> waitQueuesResponse = wingsPersistence.query(WaitQueue.class,
          aPageRequest().addFilter("correlationId", Operator.IN, correlationIds.toArray()).build(), excludeAuthority);

      Map<String, List<WaitQueue>> waitQueueMap = new HashMap<>();

      if (isEmpty(waitQueuesResponse)) {
        waitQueueMap = waitQueuesResponse.stream().collect(toMap(WaitQueue::getCorrelationId,
            waitQueue
            -> (List<WaitQueue>) newArrayList(waitQueue),
            (waitQueues, waitQueues2) -> Lists.<WaitQueue>newArrayList(concat(waitQueues, waitQueues2))));
      }

      for (NotifyResponse notifyResponse : notifyPageResponses) {
        if (waitQueueMap.get(notifyResponse.getUuid()) != null) {
          logger.info("Some wait queues still present .. skipping notifyResponse : " + notifyResponse.getUuid());
          continue;
        }

        wingsPersistence.delete(notifyResponse);
      }
    } catch (Exception exception) {
      logger.error("Error in NotifyResponseCleanupHandler", exception);
    }
  }
}
