package software.wings.service.impl.trigger;

import static java.lang.String.format;

import com.google.inject.Inject;

import io.harness.delegate.task.protocol.ResponseData;
import io.harness.waiter.ErrorNotifyResponseData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.delegatetasks.RemoteMethodReturnValueData;
import software.wings.helpers.ext.trigger.response.TriggerDeploymentNeededResponse;
import software.wings.helpers.ext.trigger.response.TriggerResponse;
import software.wings.service.intfc.TriggerService;
import software.wings.sm.ExecutionStatus;
import software.wings.utils.Misc;
import software.wings.waitnotify.NotifyCallback;

import java.util.Map;

public class TriggerCallback implements NotifyCallback {
  private static final Logger logger = LoggerFactory.getLogger(TriggerCallback.class);

  private String accountId;
  private String appId;
  private String triggerExecutionId;

  @Inject private TriggerService triggerService;

  public TriggerCallback(String accountId, String appId, String triggerExecutionId) {
    this.accountId = accountId;
    this.appId = appId;
    this.triggerExecutionId = triggerExecutionId;
  }

  @Override
  public void notify(Map<String, ResponseData> response) {
    logger.info(format("Trigger command response %s for account %s", response, accountId));

    ResponseData notifyResponseData = response.values().iterator().next();
    TriggerResponse triggerResponse = new TriggerResponse();
    triggerResponse.setExecutionStatus(ExecutionStatus.FAILED);
    if (notifyResponseData instanceof ErrorNotifyResponseData) {
      triggerResponse.setErrorMsg(((ErrorNotifyResponseData) notifyResponseData).getErrorMessage());
    } else if (notifyResponseData instanceof RemoteMethodReturnValueData) {
      triggerResponse.setErrorMsg(Misc.getMessage(((RemoteMethodReturnValueData) notifyResponseData).getException()));
    } else if (!(notifyResponseData instanceof TriggerDeploymentNeededResponse)) {
      triggerResponse.setErrorMsg("Unknown Response from delegate");
    } else {
      triggerResponse = (TriggerDeploymentNeededResponse) notifyResponseData;
    }
    triggerService.handleTriggerTaskResponse(appId, triggerExecutionId, triggerResponse);
  }

  @Override
  public void notifyError(Map<String, ResponseData> response) {
    logger.info(format("Trigger command request failed for account %s and for trigger executionId %s with response %s",
        accountId, triggerExecutionId, response));
    ResponseData notifyResponseData = response.values().iterator().next();
    TriggerResponse triggerResponse = new TriggerResponse();
    triggerResponse.setExecutionStatus(ExecutionStatus.FAILED);
    if (notifyResponseData instanceof ErrorNotifyResponseData) {
      triggerResponse.setErrorMsg(((ErrorNotifyResponseData) notifyResponseData).getErrorMessage());
    } else {
      triggerResponse.setErrorMsg("Unknown error occurred while verifying file content changed");
    }

    triggerService.handleTriggerTaskResponse(appId, triggerExecutionId, triggerResponse);
  }
}
