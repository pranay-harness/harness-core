package software.wings.helpers.ext.external.comm.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.rule.RepeatRule.Repeat;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.app.MainConfiguration;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.helpers.ext.external.comm.CollaborationProviderResponse;
import software.wings.helpers.ext.external.comm.EmailRequest;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.helpers.ext.mail.Mailer;
import software.wings.helpers.ext.mail.SmtpConfig;

import java.util.List;

public class EmailHandlerIntegrationTest extends WingsBaseTest {
  @Mock private Mailer mailer;
  @Inject @InjectMocks private EmailHandler emailHandler;
  @Inject MainConfiguration mainConfiguration;

  @Test
  public void testHandle() {
    EmailRequest emailRequest = Mockito.mock(EmailRequest.class);
    when(emailRequest.getEmailData()).thenReturn(mock(EmailData.class));
    CollaborationProviderResponse response = emailHandler.handle(emailRequest);
    assertThat(response.getStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    verify(mailer).send(emailRequest.getSmtpConfig(), emailRequest.getEncryptionDetails(), emailRequest.getEmailData());
  }

  @Test
  public void testErrorResponse() {
    EmailRequest emailRequest = Mockito.mock(EmailRequest.class);
    EmailData emailData = Mockito.mock(EmailData.class);
    when(emailRequest.getEmailData()).thenReturn(emailData);

    Mockito.doThrow(new WingsException(ErrorCode.UNKNOWN_ERROR))
        .when(mailer)
        .send(any(SmtpConfig.class), any(List.class), any(EmailData.class));

    CollaborationProviderResponse response = emailHandler.handle(emailRequest);
    assertThat(response.getStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
    verify(mailer).send(emailRequest.getSmtpConfig(), emailRequest.getEncryptionDetails(), emailRequest.getEmailData());
  }

  @Test
  @Ignore
  @Repeat(times = 3, successes = 1)
  public void testSMTPConnectivity() {
    SmtpConfig smtpConfig =
        SmtpConfig.builder()
            .host("smtp.sendgrid.net")
            .port(465)
            .username("apikey")
            .password("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".toCharArray())
            .useSSL(true)
            .build();
    EmailRequest emailRequest = EmailRequest.builder().smtpConfig(smtpConfig).build();
    assertThat(emailHandler.validateDelegateConnection(emailRequest)).isTrue();

    /**
     * Without SSL=true
     */
    smtpConfig = SmtpConfig.builder()
                     .host("smtp.sendgrid.net")
                     .port(25)
                     .username("apikey")
                     .password("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".toCharArray())
                     .build();
    emailRequest = EmailRequest.builder().smtpConfig(smtpConfig).build();
    assertThat(emailHandler.validateDelegateConnection(emailRequest)).isTrue();

    /**
     * wrong port
     */
    smtpConfig = SmtpConfig.builder()
                     .host("smtp.sendgrid.net")
                     .port(1020)
                     .username("apikey")
                     .password("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".toCharArray())
                     .useSSL(true)
                     .build();
    emailRequest = EmailRequest.builder().smtpConfig(smtpConfig).build();
    assertThat(emailHandler.validateDelegateConnection(emailRequest)).isFalse();

    smtpConfig = SmtpConfig.builder()
                     .host("smtp.sendgrid.net")
                     .port(465)
                     .username("apikey")
                     .password("fakePassword".toCharArray())
                     .useSSL(true)
                     .build();
    emailRequest = EmailRequest.builder().smtpConfig(smtpConfig).build();
    assertThat(emailHandler.validateDelegateConnection(emailRequest)).isFalse();

    smtpConfig = SmtpConfig.builder()
                     .host("fakewebsite.com")
                     .port(465)
                     .username("apikey")
                     .password("fakePassword".toCharArray())
                     .useSSL(true)
                     .build();
    emailRequest = EmailRequest.builder().smtpConfig(smtpConfig).build();
    assertThat(emailHandler.validateDelegateConnection(emailRequest)).isFalse();
  }
}
