package software.wings.helpers.ext.external.comm.handlers;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoUtils;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Transient;
import software.wings.helpers.ext.external.comm.CollaborationHandler;
import software.wings.helpers.ext.external.comm.CollaborationProviderRequest;
import software.wings.helpers.ext.external.comm.CollaborationProviderResponse;
import software.wings.helpers.ext.external.comm.EmailRequest;
import software.wings.helpers.ext.mail.Mailer;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.utils.EmailUtils;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;

@Singleton
@Slf4j
public class EmailHandler implements CollaborationHandler {
  @Inject private Mailer mailer;
  @Inject private EmailUtils emailHelperUtil;

  @Inject @Transient private transient EncryptionService encryptionService;
  private static final TimeLimiter timeLimiter = new SimpleTimeLimiter();

  @Override
  public CollaborationProviderResponse handle(CollaborationProviderRequest request) {
    EmailRequest emailRequest = (EmailRequest) request;
    try {
      mailer.send(emailRequest.getSmtpConfig(), emailRequest.getEncryptionDetails(), emailRequest.getEmailData());
      return CollaborationProviderResponse.builder()
          .accountId(emailRequest.getEmailData().getAccountId())
          .status(CommandExecutionStatus.SUCCESS)
          .build();
    } catch (Exception e) {
      String errorString = emailHelperUtil.getErrorString(emailRequest.getEmailData());
      logger.error(errorString, e);
      return CollaborationProviderResponse.builder()
          .status(CommandExecutionStatus.FAILURE)
          .errorMessage(errorString)
          .accountId(emailRequest.getEmailData().getAccountId())
          .build();
    }
  }

  @Override
  public boolean validateDelegateConnection(SmtpConfig smtpConfig, List<EncryptedDataDetail> encryptionDetails) {
    return validateDelegateConnectionInternal(smtpConfig, encryptionDetails);
  }

  @Override
  public boolean validateDelegateConnection(CollaborationProviderRequest request) {
    EmailRequest emailRequest = (EmailRequest) request;
    SmtpConfig smtpConfig = emailRequest.getSmtpConfig();
    List<EncryptedDataDetail> encryptionDetails = emailRequest.getEncryptionDetails();
    return validateDelegateConnectionInternal(smtpConfig, encryptionDetails);
  }

  private boolean validateDelegateConnectionInternal(
      SmtpConfig smtpConfig, List<EncryptedDataDetail> encryptionDetails) {
    try {
      return timeLimiter.callWithTimeout(() -> {
        boolean result = false;
        try {
          Properties props = new Properties();
          if (isNotEmpty(smtpConfig.getPassword())) {
            props.setProperty("mail.smtp.auth", "true");
          }
          SmtpConfig config = KryoUtils.clone(smtpConfig);
          encryptionService.decrypt(config, encryptionDetails);
          if (config.isUseSSL()) {
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.starttls.enable", "true");
          }
          if (config.isStartTLS()) {
            props.put("mail.smtp.starttls.enable", "true");
          }
          Session session = Session.getInstance(props, null);
          Transport transport = session.getTransport("smtp");
          transport.connect(config.getHost(), config.getPort(), config.getUsername(),
              isNotEmpty(config.getPassword()) ? new String(config.getPassword()) : null);
          transport.close();
          result = true;

          logger.info("Validated email delegate communication to {}.", transport.toString());
        } catch (AuthenticationFailedException e) {
          logger.warn("SMTP: Authentication Failed", e);

        } catch (MessagingException e) {
          logger.warn("SMTP: Messaging Exception Occurred", e);
        } catch (Exception e) {
          logger.warn("SMTP: Unknown Exception", e);
        }
        return result;
      }, 10000, TimeUnit.MILLISECONDS, true);
    } catch (Exception e) {
      logger.warn("Failed to validate email delegate communication", e);
    }
    return false;
  }
}