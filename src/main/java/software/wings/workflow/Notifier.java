package software.wings.workflow;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.OP;
import software.wings.common.thread.ThreadPool;
import software.wings.dl.WingsPersistence;
import software.wings.utils.CollectionUtils;

/**
 *
 *
 * @author Rishi
 *
 */
public class Notifier implements Runnable {
  private WingsPersistence wingsPersistence;
  private String correlationId;

  public Notifier(WingsPersistence wingsPersistence, String correlationId) {
    this.wingsPersistence = wingsPersistence;
    this.correlationId = correlationId;
  }

  @Override
  public void run() {
    // Get notify response
    //		NotifyResponse notifyResponse = wingsPersistence.get(NotifyResponse.class, correlationId);
    //		if (notifyResponse == null) {
    //			logger.warn("NO notifyResponse found for correlationId:" + correlationId +"... Already processed or will be
    //processed by scheduler"); 			return;
    //		}

    try {
      PageRequest<NotifyResponse> reqNotifyRes = new PageRequest<>();
      reqNotifyRes.getFieldsIncluded().add("uuid");
      PageResponse<NotifyResponse> notifyPageResponses = wingsPersistence.query(NotifyResponse.class, reqNotifyRes);
      if (notifyPageResponses == null || notifyPageResponses.getResponse() == null) {
        logger.debug("There are no NotifyResponse entries to process");
      }
      List<NotifyResponse> notifyResponses = notifyPageResponses.getResponse();
      List<String> correlationIds = CollectionUtils.fields(String.class, notifyResponses, "uuid");

      // Get wait queue entries
      PageRequest<WaitQueue> req = new PageRequest<>();
      SearchFilter filter = new SearchFilter();
      filter.setFieldName("correlationId");
      filter.setFieldValues(correlationIds);
      filter.setOp(OP.IN);
      req.getFilters().add(filter);
      //			req.getFieldsIncluded().add("waitInstanceId");
      //			req.getFieldsIncluded().add("correlationId");
      PageResponse<WaitQueue> waitQueuesResponse = wingsPersistence.query(WaitQueue.class, req);
      if (waitQueuesResponse == null || waitQueuesResponse.getResponse() == null
          || waitQueuesResponse.getResponse().size() == 0) {
        logger.warn("No entry in the waitQueue found for the correlationId:" + correlationId + " skipping ...");
        return;
      }
      List<WaitQueue> waitQueues = waitQueuesResponse.getResponse();
      // process distinct set of wait instanceIds
      Set<String> waitInstanceIds = new HashSet<>();
      for (WaitQueue waitQueue : waitQueues) {
        waitInstanceIds.add(waitQueue.getWaitInstanceId());
      }
      for (String waitInstanceId : waitInstanceIds) {
        ThreadPool.execute(new NotifierForWaitInstance(wingsPersistence, waitInstanceId, correlationIds));
      }
    } catch (IllegalAccessException e) {
      logger.error(e.getMessage(), e);
    } catch (IllegalArgumentException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IntrospectionException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  private static Logger logger = LoggerFactory.getLogger(Notifier.class);
}
