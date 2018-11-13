package software.wings.service.impl.trigger;

import static java.lang.String.format;

import io.harness.delegate.task.protocol.ResponseData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.helpers.ext.trigger.response.TriggerDeploymentNeededResponse;
import software.wings.waitnotify.NotifyCallback;

import java.util.Map;

public class TriggerCallback implements NotifyCallback {
  private static final Logger logger = LoggerFactory.getLogger(TriggerCallback.class);

  private String accountId;

  public TriggerCallback(String accountId) {
    this.accountId = accountId;
  }

  @Override
  public void notify(Map<String, ResponseData> response) {
    logger.info(format("Trigger command response %s for account %s", response, accountId));

    ResponseData notifyResponseData = response.values().iterator().next();

    if (notifyResponseData instanceof TriggerDeploymentNeededResponse) {
      TriggerDeploymentNeededResponse triggerResponse = (TriggerDeploymentNeededResponse) notifyResponseData;
    } else {
      logger.error(format("Unknown trigger command response %s for account %s", response, accountId));
    }
  }

  @Override
  public void notifyError(Map<String, ResponseData> response) {
    logger.info(format("Trigger command request failed for account %s with response %s", accountId, response));
  }
}
