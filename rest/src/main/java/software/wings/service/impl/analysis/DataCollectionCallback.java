package software.wings.service.impl.analysis;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.waitnotify.NotifyCallback;
import software.wings.waitnotify.NotifyResponseData;

import java.util.Map;

/**
 * Created by rsingh on 5/18/17.
 */
@AllArgsConstructor
public class DataCollectionCallback implements NotifyCallback {
  private static final Logger logger = LoggerFactory.getLogger(DataCollectionCallback.class);

  private String appId;
  @Override
  public void notify(Map<String, NotifyResponseData> response) {
    final DataCollectionTaskResult result = (DataCollectionTaskResult) response.values().iterator().next();
    logger.info("data collection result for app " + appId + " is: " + result);
  }

  @Override
  public void notifyError(Map<String, NotifyResponseData> response) {
    logger.info("error in data collection for app " + appId);
  }
}
