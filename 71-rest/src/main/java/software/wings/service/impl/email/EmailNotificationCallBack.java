package software.wings.service.impl.email;

import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.inject.Inject;

import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.waiter.NotifyCallback;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.EmailSendingFailedAlert;
import software.wings.helpers.ext.external.comm.CollaborationProviderResponse;
import software.wings.service.intfc.AlertService;

import java.util.Map;

@Slf4j
public class EmailNotificationCallBack implements NotifyCallback {
  @Inject private AlertService alertService;

  @Override
  public void notify(Map<String, ResponseData> response) {
    try {
      ResponseData data = response.entrySet().iterator().next().getValue();
      CollaborationProviderResponse collaborationProviderResponse = (CollaborationProviderResponse) data;
      if (collaborationProviderResponse.getStatus().equals(CommandExecutionStatus.SUCCESS)) {
        logger.info("Email sending succeeded. Response : [{}]", data);
        alertService.closeAlertsOfType(
            collaborationProviderResponse.getAccountId(), GLOBAL_APP_ID, AlertType.EMAIL_NOT_SENT_ALERT);
        alertService.closeAlertsOfType(
            collaborationProviderResponse.getAccountId(), GLOBAL_APP_ID, AlertType.INVALID_SMTP_CONFIGURATION);
      } else {
        openEmailNotSentAlert(data, collaborationProviderResponse);
      }
    } catch (Exception e) {
      logger.warn("Failed on notify for response=[{}]", response.toString(), e);
    }
  }

  @Override
  public void notifyError(Map<String, ResponseData> response) {
    try {
      ResponseData data = response.entrySet().iterator().next().getValue();
      if (data instanceof CollaborationProviderResponse) {
        CollaborationProviderResponse collaborationProviderResponse = (CollaborationProviderResponse) data;
        openEmailNotSentAlert(data, collaborationProviderResponse);
      } else {
        logger.warn("Failed to send Email, errorResponse=[{}] ", data);
      }
    } catch (Exception e) {
      logger.warn("Failed on notifyError for response=[{}]", response.toString(), e);
    }
  }

  private void openEmailNotSentAlert(ResponseData data, CollaborationProviderResponse collaborationProviderResponse) {
    alertService.openAlert(collaborationProviderResponse.getAccountId(), GLOBAL_APP_ID, AlertType.EMAIL_NOT_SENT_ALERT,
        EmailSendingFailedAlert.builder().emailAlertData(collaborationProviderResponse.getErrorMessage()).build());
    logger.warn("Email Sending failed : Delegate Response : [{}]", data);
  }
}
