package software.wings.service.impl;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.SettingAttributeBuilder.aSettingAttribute;
import static software.wings.helpers.ext.mail.EmailData.Builder.anEmailData;
import static software.wings.helpers.ext.mail.SmtpConfig.Builder.aSmtpConfig;

import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Verifier;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.SettingValue.SettingVariableTypes;
import software.wings.core.queue.Queue;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.helpers.ext.mail.Mailer;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.SettingsService;

/**
 * Created by peeyushaggarwal on 5/25/16.
 */
public class EmailNotificationServiceImplTest extends WingsBaseTest {
  private static final EmailData emailTemplateData = anEmailData()
                                                         .withFrom("from")
                                                         .withTo(newArrayList("to"))
                                                         .withTemplateName("templateName")
                                                         .withTemplateModel("templateModel")
                                                         .build();
  private static final EmailData emailBodyData =
      anEmailData().withFrom("from").withTo(newArrayList("to")).withBody("body").withSubject("subject").build();

  private static final SmtpConfig smtpConfig = aSmtpConfig().build();

  @Mock private Mailer mailer;

  @Mock private SettingsService settingsService;

  @Mock private Queue<EmailData> queue;
  @Rule
  public Verifier verify = new Verifier() {
    @Override
    protected void verify() throws Throwable {
      verifyNoMoreInteractions(queue, mailer, settingsService);
    }
  };
  @InjectMocks @Inject private NotificationService<EmailData> emailDataNotificationService;

  @Before
  public void setupMocks() {
    when(settingsService.getGlobalSettingAttributesByType(SettingVariableTypes.SMTP))
        .thenReturn(newArrayList(aSettingAttribute().withValue(smtpConfig).build()));
  }

  @Test
  public void shouldSendEmailWithTemplate() throws Exception {
    emailDataNotificationService.send(emailTemplateData.getFrom(), emailTemplateData.getTo(),
        emailTemplateData.getTemplateName(), emailTemplateData.getTemplateModel());
    verify(mailer).send(smtpConfig, emailTemplateData);
    verify(settingsService).getGlobalSettingAttributesByType(SettingVariableTypes.SMTP);
  }

  @Test
  public void shouldSendEmailWithBody() throws Exception {
    emailDataNotificationService.send(
        emailBodyData.getFrom(), emailBodyData.getTo(), emailBodyData.getSubject(), emailBodyData.getBody());
    verify(mailer).send(smtpConfig, emailBodyData);
    verify(settingsService).getGlobalSettingAttributesByType(SettingVariableTypes.SMTP);
  }

  @Test
  public void shouldSendEmailWithEmailData() throws Exception {
    emailDataNotificationService.send(emailBodyData);
    verify(mailer).send(smtpConfig, emailBodyData);
    verify(settingsService).getGlobalSettingAttributesByType(SettingVariableTypes.SMTP);
  }

  @Test
  public void shouldSendAsyncEmailWithTemplate() throws Exception {
    emailDataNotificationService.sendAsync(emailTemplateData.getFrom(), emailTemplateData.getTo(),
        emailTemplateData.getTemplateName(), emailTemplateData.getTemplateModel());
    verify(queue).send(emailTemplateData);
  }

  @Test
  public void sendAsyncWithBody() throws Exception {
    emailDataNotificationService.sendAsync(
        emailBodyData.getFrom(), emailBodyData.getTo(), emailBodyData.getSubject(), emailBodyData.getBody());
    verify(queue).send(emailBodyData);
  }
}
