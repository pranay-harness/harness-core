package software.wings.service.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.microservice.NotifyEngineTarget.GENERAL;
import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.TaskData;
import io.harness.exception.WingsException;
import io.harness.queue.QueuePublisher;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.waiter.WaitNotifyEngine;
import lombok.extern.slf4j.Slf4j;
import software.wings.app.MainConfiguration;
import software.wings.beans.TaskType;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.EmailSendingFailedAlert;
import software.wings.helpers.ext.external.comm.EmailRequest;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.helpers.ext.mail.Mailer;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.service.impl.email.EmailNotificationCallBack;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.utils.EmailHelperUtils;
import software.wings.utils.EmailUtils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by peeyushaggarwal on 5/23/16.
 */
@Singleton
@Slf4j
public class EmailNotificationServiceImpl implements EmailNotificationService {
  @Inject private Mailer mailer;
  @Inject private QueuePublisher<EmailData> emailEventQueue;
  @Inject private MainConfiguration mainConfiguration;
  @Inject private SecretManager secretManager;
  @Inject private DelegateService delegateService;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private EmailHelperUtils emailHelperUtils;
  @Inject private EmailUtils emailUtils;
  @Inject private AlertService alertService;

  @Override
  public boolean send(EmailData emailData) {
    SmtpConfig defaultSMTPConfig;
    SmtpConfig fallBackSMTPConfig;

    if (emailData.isSystem()) {
      defaultSMTPConfig = mainConfiguration.getSmtpConfig();
      fallBackSMTPConfig = emailHelperUtils.getSmtpConfig(emailData.getAccountId());
    } else {
      defaultSMTPConfig = emailHelperUtils.getSmtpConfig(emailData.getAccountId());
      fallBackSMTPConfig = mainConfiguration.getSmtpConfig();
    }

    boolean isDefaultSMTPConfigValid = emailHelperUtils.isSmtpConfigValid(defaultSMTPConfig);
    boolean isFallBackSMTPConfigValid = emailHelperUtils.isSmtpConfigValid(fallBackSMTPConfig);

    boolean mailSentSuccessFully = false;
    if (!isDefaultSMTPConfigValid && !isFallBackSMTPConfigValid) {
      sendEmailNotSentAlert(emailData);
    } else if (isDefaultSMTPConfigValid) {
      mailSentSuccessFully = true;
      if (!sendMail(defaultSMTPConfig, emailData)
          && (!isFallBackSMTPConfigValid || !sendMail(fallBackSMTPConfig, emailData))) {
        sendEmailNotSentAlert(emailData);
        mailSentSuccessFully = false;
      }
    } else if (isFallBackSMTPConfigValid) {
      mailSentSuccessFully = sendMail(fallBackSMTPConfig, emailData);
    }

    return mailSentSuccessFully;
  }

  private boolean sendMail(SmtpConfig config, EmailData emailData) {
    List<EncryptedDataDetail> encryptionDetails = config.equals(mainConfiguration.getSmtpConfig())
        ? Collections.emptyList()
        : secretManager.getEncryptionDetails(config, emailData.getAppId(), emailData.getWorkflowExecutionId());

    if (config.equals(mainConfiguration.getSmtpConfig())) {
      try {
        mailer.send(config, encryptionDetails, emailData);
        closeEmailNotSentAlert(emailData);
        return true;
      } catch (WingsException e) {
        String errorString = emailUtils.getErrorString(emailData);
        logger.warn(errorString, e);
        return false;
      }
    } else {
      return sendEmailAsDelegateTask(config, encryptionDetails, emailData);
    }
  }

  private void sendEmailNotSentAlert(EmailData emailData) {
    String errorString = emailUtils.getErrorString(emailData);
    alertService.openAlert(emailData.getAccountId(), GLOBAL_APP_ID, AlertType.EMAIL_NOT_SENT_ALERT,
        EmailSendingFailedAlert.builder().emailAlertData(errorString).build());
  }

  private void closeEmailNotSentAlert(EmailData emailData) {
    alertService.closeAlertsOfType(emailData.getAccountId(), GLOBAL_APP_ID, AlertType.EMAIL_NOT_SENT_ALERT);
  }

  private boolean sendEmailAsDelegateTask(
      SmtpConfig config, List<EncryptedDataDetail> encryptionDetails, EmailData emailData) {
    String waitId = generateUuid();
    try {
      EmailRequest request =
          EmailRequest.builder().emailData(emailData).encryptionDetails(encryptionDetails).smtpConfig(config).build();
      DelegateTask delegateTask = DelegateTask.builder()
                                      .accountId(emailData.getAccountId())
                                      .appId(GLOBAL_APP_ID)
                                      .waitId(waitId)
                                      .data(TaskData.builder()
                                                .taskType(TaskType.COLLABORATION_PROVIDER_TASK.name())
                                                .parameters(new Object[] {request})
                                                .timeout(TimeUnit.MINUTES.toMillis(10))
                                                .build())
                                      .async(true)
                                      .build();
      waitNotifyEngine.waitForAllOn(GENERAL, new EmailNotificationCallBack(), waitId);
      delegateService.queueTask(delegateTask);
      return true;
    } catch (Exception e) {
      String errorString = emailUtils.getErrorString(emailData);
      logger.warn(errorString, e);
      return false;
    }
  }

  @Override
  public void sendAsync(EmailData emailData) {
    emailEventQueue.send(emailData);
  }
}
