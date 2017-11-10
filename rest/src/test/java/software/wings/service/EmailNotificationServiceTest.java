package software.wings.service;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Verifier;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.core.queue.Queue;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.helpers.ext.mail.Mailer;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.Collections;

/**
 * Created by peeyushaggarwal on 5/25/16.
 */
public class EmailNotificationServiceTest extends WingsBaseTest {
  private static final EmailData emailTemplateData = EmailData.builder()
                                                         .to(newArrayList("to"))
                                                         .cc(newArrayList("cc"))
                                                         .templateName("templateName")
                                                         .templateModel("templateModel")
                                                         .accountId(ACCOUNT_ID)
                                                         .build();
  private static final EmailData emailBodyData = EmailData.builder()
                                                     .to(newArrayList("to"))
                                                     .cc(newArrayList("cc"))
                                                     .body("body")
                                                     .subject("subject")
                                                     .accountId(ACCOUNT_ID)
                                                     .build();

  private static final SmtpConfig smtpConfig = SmtpConfig.builder().build();

  @Mock private Mailer mailer;

  @Mock private SettingsService settingsService;

  @Mock private Queue<EmailData> queue;

  @Mock private SecretManager secretManager;
  /**
   * The Verify.
   */
  @Rule
  public Verifier verify = new Verifier() {
    @Override
    protected void verify() throws Throwable {
      verifyNoMoreInteractions(queue, mailer, settingsService);
    }
  };
  @InjectMocks @Inject private EmailNotificationService emailDataNotificationService;

  /**
   * Setup mocks.
   */
  @Before
  public void setupMocks() {
    when(settingsService.getGlobalSettingAttributesByType(ACCOUNT_ID, SettingVariableTypes.SMTP.name()))
        .thenReturn(newArrayList(aSettingAttribute().withValue(smtpConfig).build()));
  }

  /**
   * Should send email with template.
   *
   * @throws Exception the exception
   */
  @Test
  public void shouldSendEmailWithTemplate() throws Exception {
    emailDataNotificationService.send(emailTemplateData);
    verify(mailer).send(smtpConfig, Collections.emptyList(), emailTemplateData);
    verify(settingsService).getGlobalSettingAttributesByType(ACCOUNT_ID, SettingVariableTypes.SMTP.name());
  }

  /**
   * Should send email with body.
   *
   * @throws Exception the exception
   */
  @Test
  public void shouldSendEmailWithBody() throws Exception {
    emailDataNotificationService.send(emailBodyData);
    verify(mailer).send(smtpConfig, Collections.emptyList(), emailBodyData);
    verify(settingsService).getGlobalSettingAttributesByType(ACCOUNT_ID, SettingVariableTypes.SMTP.name());
  }

  /**
   * Should send async email with template.
   *
   * @throws Exception the exception
   */
  @Test
  public void shouldSendAsyncEmailWithTemplate() throws Exception {
    emailDataNotificationService.sendAsync(emailTemplateData);
    verify(queue).send(emailTemplateData);
  }

  /**
   * Send async with body.
   *
   * @throws Exception the exception
   */
  @Test
  public void sendAsyncWithBody() throws Exception {
    emailDataNotificationService.sendAsync(emailBodyData);
    verify(queue).send(emailBodyData);
  }
}
