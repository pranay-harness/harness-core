package software.wings.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.app.MainConfiguration;
import software.wings.beans.SettingAttribute;
import software.wings.core.queue.Queue;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.helpers.ext.mail.Mailer;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.Collections;
import java.util.List;

/**
 * Created by peeyushaggarwal on 5/23/16.
 */
@Singleton
public class EmailNotificationServiceImpl implements EmailNotificationService {
  @Inject private Mailer mailer;

  @Inject private SettingsService settingsService;

  @Inject private Queue<EmailData> emailEventQueue;

  @Inject private MainConfiguration mainConfiguration;

  @Inject private SecretManager secretManager;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.EmailNotificationService#send(java.lang.Object)
   */
  @Override
  public void send(EmailData emailData) {
    SmtpConfig config =
        emailData.isSystem() ? mainConfiguration.getSmtpConfig() : getSmtpConfig(emailData.getAccountId());

    List<EncryptedDataDetail> encryptionDetails = emailData.isSystem()
        ? Collections.emptyList()
        : secretManager.getEncryptionDetails(config, emailData.getAppId(), emailData.getWorkflowExecutionId());
    mailer.send(config, encryptionDetails, emailData);
  }

  @Override
  public void sendAsync(EmailData emailData) {
    emailEventQueue.send(emailData);
  }

  private SmtpConfig getSmtpConfig(String accountId) {
    SettingAttribute settings =
        settingsService.getGlobalSettingAttributesByType(accountId, SettingVariableTypes.SMTP.name()).get(0);
    return (SmtpConfig) settings.getValue();
  }
}
