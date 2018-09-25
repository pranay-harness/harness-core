package software.wings.service.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.WingsException;
import io.harness.queue.Queue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.MainConfiguration;
import software.wings.beans.DelegateTask;
import software.wings.beans.TaskType;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.EmailSendingFailedAlert;
import software.wings.helpers.ext.external.comm.EmailRequest;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.helpers.ext.mail.Mailer;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.email.EmailNotificationCallBack;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.utils.EmailHelperUtil;
import software.wings.utils.EmailUtil;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by peeyushaggarwal on 5/23/16.
 */
@Singleton
public class EmailNotificationServiceImpl implements EmailNotificationService {
  @Inject private Mailer mailer;

  @Inject private Queue<EmailData> emailEventQueue;

  @Inject private MainConfiguration mainConfiguration;

  @Inject private SecretManager secretManager;

  @Inject private DelegateService delegateService;

  @Inject private WaitNotifyEngine waitNotifyEngine;

  @Inject private EmailHelperUtil emailHelperUtil;

  @Inject private EmailUtil emailUtil;

  @Inject private AlertService alertService;

  private static final Logger logger = LoggerFactory.getLogger(EmailNotificationServiceImpl.class);

  /* (non-Javadoc)
   * @see software.wings.service.intfc.EmailNotificationService#send(java.lang.Object)
   */
  @Override
  public void send(EmailData emailData) {
    SmtpConfig config = emailData.isSystem() ? mainConfiguration.getSmtpConfig()
                                             : emailHelperUtil.getSmtpConfig(emailData.getAccountId());

    if (!emailHelperUtil.isSmtpConfigValid(config)) {
      config = mainConfiguration.getSmtpConfig();
    }

    if (!emailHelperUtil.isSmtpConfigValid(config)) {
      sendEmailNotSentAlert(emailData);
      return;
    }

    List<EncryptedDataDetail> encryptionDetails = config.equals(mainConfiguration.getSmtpConfig())
        ? Collections.emptyList()
        : secretManager.getEncryptionDetails(config, emailData.getAppId(), emailData.getWorkflowExecutionId());

    if (config.equals(mainConfiguration.getSmtpConfig())) {
      try {
        mailer.send(config, encryptionDetails, emailData);
        closeEmailNotSentAlert(emailData);
      } catch (WingsException e) {
        String errorString = emailUtil.getErrorString(emailData);
        logger.warn(errorString, e);
        sendEmailNotSentAlert(emailData);
      }
    } else {
      sendEmailAsDelegateTask(config, encryptionDetails, emailData);
    }
  }

  private void sendEmailNotSentAlert(EmailData emailData) {
    String errorString = emailUtil.getErrorString(emailData);
    alertService.openAlert(emailData.getAccountId(), GLOBAL_APP_ID, AlertType.EMAIL_NOT_SENT_ALERT,
        EmailSendingFailedAlert.builder().emailAlertData(errorString).build());
  }

  private void closeEmailNotSentAlert(EmailData emailData) {
    alertService.closeAlertsOfType(emailData.getAccountId(), GLOBAL_APP_ID, AlertType.EMAIL_NOT_SENT_ALERT);
  }

  private void sendEmailAsDelegateTask(
      SmtpConfig config, List<EncryptedDataDetail> encryptionDetails, EmailData emailData) {
    String waitId = generateUuid();
    try {
      EmailRequest request =
          EmailRequest.builder().emailData(emailData).encryptionDetails(encryptionDetails).smtpConfig(config).build();
      DelegateTask delegateTask = aDelegateTask()
                                      .withTaskType(TaskType.COLLABORATION_PROVIDER_TASK)
                                      .withAccountId(emailData.getAccountId())
                                      .withAppId(GLOBAL_APP_ID)
                                      .withWaitId(waitId)
                                      .withParameters(new Object[] {request})
                                      .withTimeout(TimeUnit.MINUTES.toMillis(10))
                                      .withAsync(true)
                                      .build();
      waitNotifyEngine.waitForAll(new EmailNotificationCallBack(), waitId);
      delegateService.queueTask(delegateTask);
    } catch (Exception e) {
      String errorString = emailUtil.getErrorString(emailData);
      logger.warn(errorString, e);
      sendEmailNotSentAlert(emailData);
    }
  }

  @Override
  public void sendAsync(EmailData emailData) {
    emailEventQueue.send(emailData);
  }
}
