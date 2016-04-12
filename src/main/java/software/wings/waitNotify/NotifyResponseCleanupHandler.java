/**
 *
 */
package software.wings.waitNotify;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.OP;
import software.wings.dl.WingsPersistence;
import software.wings.sm.ExecutionStatus;
import software.wings.utils.CollectionUtils;

/**
 * This is meant to cleanup notifyResponse objects that have already been used to callback waiting instances, or have
 * expired.
 * @author Rishi
 *
 */
public class NotifyResponseCleanupHandler implements Runnable {
  @Inject private WingsPersistence wingsPersistence;

  /* (non-Javadoc)
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    try {
      PageRequest<NotifyResponse> reqNotifyRes = new PageRequest<>();
      reqNotifyRes.addFilter("status", ExecutionStatus.SUCCESS, OP.EQ);
      reqNotifyRes.setLimit(PageRequest.UNLIMITED);
      reqNotifyRes.getFieldsIncluded().add("uuid");
      PageResponse<NotifyResponse> notifyPageResponses = wingsPersistence.query(NotifyResponse.class, reqNotifyRes);
      if (notifyPageResponses == null || notifyPageResponses.getResponse() == null
          || notifyPageResponses.getResponse().size() == 0) {
        logger.debug("There are no NotifyResponse entries to cleanup");
        return;
      }

      List<String> correlationIds = CollectionUtils.fields(String.class, notifyPageResponses.getResponse(), "uuid");

      // Get wait queue entries
      PageRequest<WaitQueue> req = new PageRequest<>();
      SearchFilter filter = new SearchFilter();
      filter.setFieldName("correlationId");
      filter.setFieldValues(correlationIds);
      filter.setOp(OP.IN);
      req.getFilters().add(filter);
      PageResponse<WaitQueue> waitQueuesResponse = wingsPersistence.query(WaitQueue.class, req);
      if (waitQueuesResponse == null || waitQueuesResponse.getResponse() == null
          || waitQueuesResponse.getResponse().size() == 0) {
        logger.warn("No entry in the waitQueue found for the correlationIds:" + correlationIds + " skipping ...");
        return;
      }
      List<WaitQueue> waitQueues = waitQueuesResponse.getResponse();
      Map<String, List<WaitQueue>> waitQueueMap = CollectionUtils.hierarchy(waitQueues, "correlationId");

      for (NotifyResponse notifyResponse : notifyPageResponses.getResponse()) {
        if (waitQueueMap.get(notifyResponse.getUuid()) != null) {
          logger.info("Some wait queues still present .. skiping notifyResponse : " + notifyResponse.getUuid());
          continue;
        }

        wingsPersistence.delete(notifyResponse);
      }
    } catch (Exception e) {
      logger.error("Error in NotifyResponseCleanupHandler", e);
    }
  }

  private static Logger logger = LoggerFactory.getLogger(NotifyResponseCleanupHandler.class);
}
