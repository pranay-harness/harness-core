package software.wings.service.impl.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.waitnotify.NotifyCallback;
import software.wings.waitnotify.NotifyResponseData;

import java.util.Map;

/**
 * Created by rsingh on 5/18/17.
 */
public class LogCollectionCallback implements NotifyCallback {
  private static final Logger logger = LoggerFactory.getLogger(LogCollectionCallback.class);

  private String appId;

  public LogCollectionCallback() {}

  public LogCollectionCallback(String appId) {
    this.appId = appId;
  }

  @Override
  public void notify(Map<String, NotifyResponseData> response) {
    final LogDataCollectionTaskResult result = (LogDataCollectionTaskResult) response.values().iterator().next();
    logger.info("data collection result for app " + appId + " is: " + result);
  }

  @Override
  public void notifyError(Map<String, NotifyResponseData> response) {
    logger.info("error in data collection for app " + appId);
  }
}
