package software.wings.service.impl.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.RemoteMethodReturnValueData;
import io.harness.exception.ExceptionUtils;
import io.harness.waiter.NotifyCallback;
import lombok.extern.slf4j.Slf4j;
import software.wings.helpers.ext.trigger.response.TriggerDeploymentNeededResponse;
import software.wings.helpers.ext.trigger.response.TriggerResponse;
import software.wings.service.intfc.TriggerService;

import java.util.Map;

@OwnedBy(CDC)
@Slf4j
public class TriggerCallback implements NotifyCallback {
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
  public void notify(Map<String, DelegateResponseData> response) {
    logger.info("Trigger command response {} for account {}", response, accountId);

    DelegateResponseData notifyResponseData = response.values().iterator().next();
    TriggerResponse triggerResponse = new TriggerResponse();
    triggerResponse.setExecutionStatus(ExecutionStatus.FAILED);
    if (notifyResponseData instanceof ErrorNotifyResponseData) {
      triggerResponse.setErrorMsg(((ErrorNotifyResponseData) notifyResponseData).getErrorMessage());
    } else if (notifyResponseData instanceof RemoteMethodReturnValueData) {
      triggerResponse.setErrorMsg(
          ExceptionUtils.getMessage(((RemoteMethodReturnValueData) notifyResponseData).getException()));
    } else if (!(notifyResponseData instanceof TriggerDeploymentNeededResponse)) {
      triggerResponse.setErrorMsg("Unknown Response from delegate");
    } else {
      triggerResponse = (TriggerDeploymentNeededResponse) notifyResponseData;
    }
    triggerService.handleTriggerTaskResponse(appId, triggerExecutionId, triggerResponse);
  }

  @Override
  public void notifyError(Map<String, DelegateResponseData> response) {
    logger.info("Trigger command request failed for account {} and for trigger executionId {} with response {}",
        accountId, triggerExecutionId, response);
    DelegateResponseData notifyResponseData = response.values().iterator().next();
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
