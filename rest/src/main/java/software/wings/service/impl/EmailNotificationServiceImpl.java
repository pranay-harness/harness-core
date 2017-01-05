package software.wings.service.impl;

import static software.wings.helpers.ext.mail.EmailData.Builder.anEmailData;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import freemarker.template.TemplateException;
import org.apache.commons.mail.EmailException;
import software.wings.beans.SettingAttribute;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.core.queue.Queue;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.helpers.ext.mail.Mailer;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.service.intfc.SettingsService;

import java.io.IOException;
import java.util.List;

/**
 * Created by peeyushaggarwal on 5/23/16.
 */
@Singleton
public class EmailNotificationServiceImpl implements EmailNotificationService<EmailData> {
  @Inject private Mailer mailer;

  @Inject private SettingsService settingsService;

  @Inject private Queue<EmailData> emailEventQueue;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.EmailNotificationService#send(java.util.List, java.util.List, java.lang.String,
   * java.lang.Object)
   */
  @Override
  public void send(List<String> to, List<String> cc, String templateName, Object templateModel)
      throws EmailException, TemplateException, IOException {
    send(anEmailData().withTo(to).withCc(cc).withTemplateName(templateName).withTemplateModel(templateModel).build());
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.EmailNotificationService#send(java.util.List, java.util.List, java.lang.String,
   * java.lang.String)
   */
  @Override
  public void send(List<String> to, List<String> cc, String subject, String body)
      throws EmailException, TemplateException, IOException {
    send(anEmailData().withTo(to).withCc(cc).withSubject(subject).withBody(body).build());
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.EmailNotificationService#send(java.lang.Object)
   */
  @Override
  public void send(EmailData emailData) throws EmailException, TemplateException, IOException {
    SmtpConfig config = getSmtpConfig();

    mailer.send(config, emailData);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.EmailNotificationService#sendAsync(java.util.List, java.util.List,
   * java.lang.String, java.lang.String)
   */
  @Override
  public void sendAsync(List<String> to, List<String> cc, String subject, String body) {
    emailEventQueue.send(
        anEmailData().withTo(to).withCc(cc).withSubject(subject).withBody(body).withRetries(3).build());
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.EmailNotificationService#sendAsync(java.util.List, java.util.List,
   * java.lang.String, java.lang.Object)
   */
  @Override
  public void sendAsync(List<String> to, List<String> cc, String templateName, Object templateModel) {
    emailEventQueue.send(anEmailData()
                             .withTo(to)
                             .withCc(cc)
                             .withTemplateName(templateName)
                             .withTemplateModel(templateModel)
                             .withRetries(3)
                             .build());
  }

  private SmtpConfig getSmtpConfig() {
    SettingAttribute settings =
        settingsService.getGlobalSettingAttributesByType(SettingVariableTypes.SMTP.name()).get(0);
    return (SmtpConfig) settings.getValue();
  }
}
