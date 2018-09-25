package software.wings.service;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import com.google.inject.Inject;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.queue.Queue;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Verifier;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.app.MainConfiguration;
import software.wings.beans.Base;
import software.wings.beans.DelegateTask;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.EmailSendingFailedAlert;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.helpers.ext.mail.Mailer;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.EmailHelperUtil;
import software.wings.utils.EmailUtil;

import java.util.Collections;
import java.util.List;

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

  @Mock private AlertService alertService;

  @Inject EmailUtil emailUtil;

  @InjectMocks @Inject private EmailHelperUtil emailHelperUtil;
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
    verifyNoMoreInteractions(alertService);
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
    verifyNoMoreInteractions(alertService);
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
    verify(alertService).closeAlertsOfType(ACCOUNT_ID, Base.GLOBAL_APP_ID, AlertType.EMAIL_NOT_SENT_ALERT);
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
    verifyNoMoreInteractions(settingsService, delegateService);
    verify(alertService).closeAlertsOfType(ACCOUNT_ID, Base.GLOBAL_APP_ID, AlertType.EMAIL_NOT_SENT_ALERT);
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

  @Test
  public void testEmailAlerts() {
    doThrow(new WingsException(ErrorCode.EMAIL_FAILED))
        .when(mailer)
        .send(any(SmtpConfig.class), any(List.class), any(EmailData.class));
    emailDataNotificationService.send(systemEmailTemplateData);
    String errorMessage = emailUtil.getErrorString(systemEmailTemplateData);
    verify(alertService)
        .openAlert(ACCOUNT_ID, Base.GLOBAL_APP_ID, AlertType.EMAIL_NOT_SENT_ALERT,
            EmailSendingFailedAlert.builder().emailAlertData(errorMessage).build());
    verify(mailer).send(mainConfiguration.getSmtpConfig(), Collections.emptyList(), systemEmailTemplateData);
  }

  @Test
  public void testDefaultToMainConfiguration() {
    when(settingsService.getGlobalSettingAttributesByType(ACCOUNT_ID, SettingVariableTypes.SMTP.name()))
        .thenReturn(newArrayList());
    emailDataNotificationService.send(nonSystemEmailBodyData);
    verify(mailer).send(mainConfiguration.getSmtpConfig(), Collections.emptyList(), nonSystemEmailBodyData);
    verifyNoMoreInteractions(delegateService, queue);
    verify(alertService).closeAlertsOfType(ACCOUNT_ID, Base.GLOBAL_APP_ID, AlertType.EMAIL_NOT_SENT_ALERT);
  }

  @Test
  public void testInvalidSmtpConfiguration() {
    when(settingsService.getGlobalSettingAttributesByType(ACCOUNT_ID, SettingVariableTypes.SMTP.name()))
        .thenReturn(newArrayList());
    when(mainConfiguration.getSmtpConfig()).thenReturn(null);
    emailDataNotificationService.send(nonSystemEmailBodyData);
    String errorMessage = emailUtil.getErrorString(nonSystemEmailBodyData);
    verify(alertService)
        .openAlert(ACCOUNT_ID, Base.GLOBAL_APP_ID, AlertType.EMAIL_NOT_SENT_ALERT,
            EmailSendingFailedAlert.builder().emailAlertData(errorMessage).build());
    verifyNoMoreInteractions(mailer, delegateService, queue);
  }
}
