package software.wings.service.impl.email;

import static java.lang.String.format;
import static software.wings.beans.Base.GLOBAL_APP_ID;

import com.google.inject.Inject;

import io.harness.delegate.task.protocol.ResponseData;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.EmailSendingFailedAlert;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.helpers.ext.external.comm.CollaborationProviderResponse;
import software.wings.service.intfc.AlertService;
import software.wings.waitnotify.NotifyCallback;

import java.util.Map;

public class EmailNotificationCallBack implements NotifyCallback {
  @Transient private static Logger logger = LoggerFactory.getLogger(EmailNotificationCallBack.class);

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
      logger.warn(format("Failed on notify for response=[%s]", response.toString()), e);
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
      logger.warn(format("Failed on notifyError for response=[%s]", response.toString()), e);
    }
  }

  private void openEmailNotSentAlert(ResponseData data, CollaborationProviderResponse collaborationProviderResponse) {
    alertService.openAlert(collaborationProviderResponse.getAccountId(), GLOBAL_APP_ID, AlertType.EMAIL_NOT_SENT_ALERT,
        EmailSendingFailedAlert.builder().emailAlertData(collaborationProviderResponse.getErrorMessage()).build());
    logger.warn("Email Sending failed : Delegate Response : [{}]", data);
  }
}
