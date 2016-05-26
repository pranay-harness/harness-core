package software.wings.service.impl;

import static software.wings.helpers.ext.mail.EmailData.Builder.anEmailData;

import com.google.inject.Inject;

import freemarker.template.TemplateException;
import org.apache.commons.mail.EmailException;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingValue.SettingVariableTypes;
import software.wings.core.queue.Queue;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.helpers.ext.mail.Mailer;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.SettingsService;

import java.io.IOException;
import java.util.List;

/**
 * Created by peeyushaggarwal on 5/23/16.
 */
public class EmailNotificationServiceImpl implements NotificationService<EmailData> {
  @Inject private Mailer mailer;

  @Inject private SettingsService settingsService;

  @Inject private Queue<EmailData> emailEventQueue;

  @Override
  public void send(String from, List<String> to, String templateName, Object templateModel)
      throws EmailException, TemplateException, IOException {
    send(anEmailData()
             .withFrom(from)
             .withTo(to)
             .withTemplateName(templateName)
             .withTemplateModel(templateModel)
             .build());
  }

  @Override
  public void send(String from, List<String> to, String subject, String body)
      throws EmailException, TemplateException, IOException {
    send(anEmailData().withFrom(from).withTo(to).withSubject(subject).withBody(body).build());
  }

  @Override
  public void send(EmailData emailData) throws EmailException, TemplateException, IOException {
    SmtpConfig config = getSmtpConfig();

    mailer.send(config, emailData);
  }

  @Override
  public void sendAsync(String from, List<String> to, String subject, String body) {
    emailEventQueue.send(
        anEmailData().withFrom(from).withTo(to).withSubject(subject).withBody(body).withRetries(3).build());
  }

  @Override
  public void sendAsync(String from, List<String> to, String templateName, Object templateModel) {
    emailEventQueue.send(anEmailData()
                             .withFrom(from)
                             .withTo(to)
                             .withTemplateName(templateName)
                             .withTemplateModel(templateModel)
                             .withRetries(3)
                             .build());
  }

  private SmtpConfig getSmtpConfig() {
    SettingAttribute settings = settingsService.getGlobalSettingAttributesByType(SettingVariableTypes.SMTP).get(0);
    return (SmtpConfig) settings.getValue();
  }
}
