package software.wings.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import freemarker.template.TemplateException;
import org.apache.commons.mail.EmailException;
import software.wings.app.MainConfiguration;
import software.wings.beans.SettingAttribute;
import software.wings.core.queue.Queue;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.helpers.ext.mail.Mailer;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.io.IOException;

/**
 * Created by peeyushaggarwal on 5/23/16.
 */
@Singleton
public class EmailNotificationServiceImpl implements EmailNotificationService {
  @Inject private Mailer mailer;

  @Inject private SettingsService settingsService;

  @Inject private Queue<EmailData> emailEventQueue;

  @Inject private MainConfiguration mainConfiguration;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.EmailNotificationService#send(java.lang.Object)
   */
  @Override
  public void send(EmailData emailData) throws EmailException, TemplateException, IOException {
    SmtpConfig config =
        emailData.isSystem() ? mainConfiguration.getSmtpConfig() : getSmtpConfig(emailData.getAccountId());

    mailer.send(config, emailData);
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
