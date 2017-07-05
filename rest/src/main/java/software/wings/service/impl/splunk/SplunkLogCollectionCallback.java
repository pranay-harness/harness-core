package software.wings.service.impl.splunk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.service.impl.appdynamics.AppdynamicsDataCollectionTaskResult;
import software.wings.waitnotify.NotifyCallback;
import software.wings.waitnotify.NotifyResponseData;

import java.util.Map;

/**
 * Created by rsingh on 5/18/17.
 */
public class SplunkLogCollectionCallback implements NotifyCallback {
  private static final Logger logger = LoggerFactory.getLogger(SplunkLogCollectionCallback.class);

  private String appId;

  public SplunkLogCollectionCallback() {}

  public SplunkLogCollectionCallback(String appId) {
    this.appId = appId;
  }

  @Override
  public void notify(Map<String, NotifyResponseData> response) {
    final SplunkDataCollectionTaskResult result = (SplunkDataCollectionTaskResult) response.values().iterator().next();
    logger.info("data collection result for app " + appId + " is: " + result);
  }

  @Override
  public void notifyError(Map<String, NotifyResponseData> response) {
    logger.info("error in data collection for app " + appId);
  }
}
