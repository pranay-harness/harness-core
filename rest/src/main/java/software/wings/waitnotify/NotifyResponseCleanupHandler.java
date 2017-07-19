package software.wings.waitnotify;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.eclipse.jetty.util.LazyList.isEmpty;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.common.collect.Lists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.SearchFilter.Operator;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.sm.ExecutionStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

/**
 * This is meant to cleanup notifyResponse objects that have already been used to callback waiting
 * instances, or have expired.
 *
 * @author Rishi
 */
public final class NotifyResponseCleanupHandler implements Runnable {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Inject private WingsPersistence wingsPersistence;

  /* (non-Javadoc)
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    try {
      PageResponse<NotifyResponse> notifyPageResponses = wingsPersistence.query(NotifyResponse.class,
          aPageRequest()
              .addFilter(aSearchFilter().withField("status", Operator.EQ, ExecutionStatus.SUCCESS).build())
              .addFieldsIncluded(ID_KEY)
              .build());
      if (isEmpty(notifyPageResponses)) {
        logger.debug("There are no NotifyResponse entries to cleanup");
        return;
      }

      List<String> correlationIds = notifyPageResponses.stream().map(NotifyResponse::getUuid).collect(toList());
      PageResponse<WaitQueue> waitQueuesResponse = wingsPersistence.query(WaitQueue.class,
          aPageRequest()
              .addFilter(aSearchFilter().withField("correlationId", Operator.IN, correlationIds.toArray()).build())
              .build());

      Map<String, List<WaitQueue>> waitQueueMap = new HashMap<>();

      if (isEmpty(waitQueuesResponse)) {
        waitQueueMap = waitQueuesResponse.stream().collect(toMap(WaitQueue::getCorrelationId,
            waitQueue
            -> (List<WaitQueue>) newArrayList(waitQueue),
            (waitQueues, waitQueues2) -> Lists.<WaitQueue>newArrayList(concat(waitQueues, waitQueues2))));
      }

      for (NotifyResponse notifyResponse : notifyPageResponses) {
        if (waitQueueMap.get(notifyResponse.getUuid()) != null) {
          logger.info("Some wait queues still present .. skiping notifyResponse : " + notifyResponse.getUuid());
          continue;
        }

        wingsPersistence.delete(notifyResponse);
      }
    } catch (Exception exception) {
      logger.error("Error in NotifyResponseCleanupHandler: " + exception.getMessage(), exception);
      for (StackTraceElement elem : exception.getStackTrace()) {
        logger.error("Trace: {}", elem);
      }
    }
  }
}
