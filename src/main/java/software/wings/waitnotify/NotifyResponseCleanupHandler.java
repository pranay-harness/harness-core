package software.wings.waitnotify;

import static java.util.stream.Collectors.toList;
import static org.eclipse.jetty.util.LazyList.isEmpty;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.sm.ExecutionStatus;
import software.wings.utils.CollectionUtils;

import java.util.ArrayList;
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
      PageRequest<NotifyResponse> reqNotifyRes = new PageRequest<>();
      reqNotifyRes.addFilter("status", ExecutionStatus.SUCCESS, Operator.EQ);
      reqNotifyRes.setLimit(PageRequest.UNLIMITED);
      reqNotifyRes.addFieldsIncluded(ID_KEY);
      PageResponse<NotifyResponse> notifyPageResponses = wingsPersistence.query(NotifyResponse.class, reqNotifyRes);
      if (isEmpty(notifyPageResponses)) {
        logger.debug("There are no NotifyResponse entries to cleanup");
        return;
      }

      List<String> correlationIds = notifyPageResponses.stream().map(NotifyResponse::getUuid).collect(toList());

      // Get wait queue entries
      SearchFilter filter = new SearchFilter();
      filter.setFieldName("correlationId");
      ArrayList<Object> fieldValues = new ArrayList();
      fieldValues.addAll(correlationIds);
      filter.setFieldValues(fieldValues);
      filter.setOp(Operator.IN);
      PageRequest<WaitQueue> req = new PageRequest<>();
      req.addFilter(filter);

      Map<String, List<WaitQueue>> waitQueueMap = new HashMap<>();
      PageResponse<WaitQueue> waitQueuesResponse = wingsPersistence.query(WaitQueue.class, req);
      if (isEmpty(waitQueuesResponse)) {
        waitQueueMap = CollectionUtils.hierarchy(waitQueuesResponse, "correlationId");
      }

      for (NotifyResponse notifyResponse : notifyPageResponses) {
        if (waitQueueMap.get(notifyResponse.getUuid()) != null) {
          logger.info("Some wait queues still present .. skiping notifyResponse : " + notifyResponse.getUuid());
          continue;
        }

        wingsPersistence.delete(notifyResponse);
      }
    } catch (Exception exception) {
      logger.error("Error in NotifyResponseCleanupHandler", exception);
    }
  }
}
