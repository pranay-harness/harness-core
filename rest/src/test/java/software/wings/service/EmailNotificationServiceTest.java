package software.wings.service;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
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
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.app.MainConfiguration;
import software.wings.beans.DelegateTask;
import software.wings.core.queue.Queue;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.helpers.ext.mail.Mailer;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.EmailHelperUtil;

import java.util.Collections;

/**
 * Created by peeyushaggarwal on 5/25/16.
 */
public class EmailNotificationServiceTest extends WingsBaseTest {
  private static final EmailData nonSystemEmailTemplateData = EmailData.builder()
                                                                  .to(newArrayList("to"))
                                                                  .cc(newArrayList("cc"))
                                                                  .templateName("templateName")
                                                                  .templateModel("templateModel")
                                                                  .accountId(ACCOUNT_ID)
                                                                  .build();
  private static final EmailData nonSystemEmailBodyData = EmailData.builder()
                                                              .to(newArrayList("to"))
                                                              .cc(newArrayList("cc"))
                                                              .body("body")
                                                              .subject("subject")
                                                              .accountId(ACCOUNT_ID)
                                                              .build();

  private static final EmailData systemEmailTemplateData = EmailData.builder()
                                                               .to(newArrayList("to"))
                                                               .cc(newArrayList("cc"))
                                                               .templateName("templateName")
                                                               .templateModel("templateModel")
                                                               .system(true)
                                                               .accountId(ACCOUNT_ID)
                                                               .build();
  private static final EmailData systemEmailBodyData = EmailData.builder()
                                                           .to(newArrayList("to"))
                                                           .cc(newArrayList("cc"))
                                                           .body("body")
                                                           .subject("subject")
                                                           .accountId(ACCOUNT_ID)
                                                           .system(true)
                                                           .build();

  private static final SmtpConfig smtpConfig = SmtpConfig.builder()
                                                   .host("testHost")
                                                   .username("testUser")
                                                   .password("testPassword".toCharArray())
                                                   .accountId("testAccount")
                                                   .build();

  @Mock private Mailer mailer;

  @Mock private SettingsService settingsService;

  @Mock private Queue<EmailData> queue;

  @Mock private SecretManager secretManager;

  @Mock private DelegateService delegateService;

  @Mock private MainConfiguration mainConfiguration;

  @Spy @InjectMocks private EmailHelperUtil emailHelperUtil;
  /**
   * The Verify.
   */
  @Rule
  public Verifier verify = new Verifier() {
    @Override
    protected void verify() throws Throwable {
      verifyNoMoreInteractions(queue, mailer, delegateService);
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

    SmtpConfig mock = mock(SmtpConfig.class);
    when(mock.valid()).thenReturn(true);
    when(mainConfiguration.getSmtpConfig()).thenReturn(mock);
  }

  /**
   * Should send email with template.
   *
   * @throws Exception the exception
   */
  @Test
  public void shouldSendEmailWithTemplate() {
    emailDataNotificationService.send(nonSystemEmailTemplateData);
    verify(delegateService).queueTask(any(DelegateTask.class));
    verify(settingsService).getGlobalSettingAttributesByType(ACCOUNT_ID, SettingVariableTypes.SMTP.name());
  }

  /**
   * Should send email with body.
   *
   * @throws Exception the exception
   */
  @Test
  public void shouldSendEmailWithBody() {
    emailDataNotificationService.send(nonSystemEmailBodyData);
    verify(delegateService).queueTask(any(DelegateTask.class));
    verify(settingsService).getGlobalSettingAttributesByType(ACCOUNT_ID, SettingVariableTypes.SMTP.name());
    verifyNoMoreInteractions(mailer);
  }

  /**
   * Should send email with template.
   *
   * @throws Exception the exception
   */
  @Test
  public void shouldSendSystemEmailWithTemplate() {
    emailDataNotificationService.send(systemEmailTemplateData);
    verify(mailer).send(mainConfiguration.getSmtpConfig(), Collections.emptyList(), systemEmailTemplateData);
    verifyNoMoreInteractions(delegateService);
    verifyNoMoreInteractions(settingsService);
  }

  /**
   * Should send email with body.
   *
   * @throws Exception the exception
   */
  @Test
  public void shouldSendSystemEmailWithBody() {
    emailDataNotificationService.send(systemEmailBodyData);
    verify(mailer).send(mainConfiguration.getSmtpConfig(), Collections.emptyList(), systemEmailBodyData);
    verifyNoMoreInteractions(settingsService);
    verifyNoMoreInteractions(delegateService);
  }

  /**
   * Should send async email with template.
   *
   * @throws Exception the exception
   */
  @Test
  public void shouldSendAsyncEmailWithTemplate() {
    emailDataNotificationService.sendAsync(nonSystemEmailTemplateData);
    verify(queue).send(nonSystemEmailTemplateData);
  }

  /**
   * Send async with body.
   *
   * @throws Exception the exception
   */
  @Test
  public void sendAsyncWithBody() {
    emailDataNotificationService.sendAsync(nonSystemEmailBodyData);
    verify(queue).send(nonSystemEmailBodyData);
  }
}
