package io.harness.batch.processing.mail;

import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.helpers.ext.mail.SmtpConfig;

@Component
@Slf4j
public class CEMailNotificationService {
  @Autowired WingsPersistence wingsPersistence;
  @Autowired BatchMainConfig mainConfiguration;
  @Autowired CEMailer mailer;

  public boolean send(EmailData emailData) {
    SmtpConfig defaultSMTPConfig;
    defaultSMTPConfig = mainConfiguration.getSmtpConfig();
    boolean isDefaultSMTPConfigValid = isSmtpConfigValid(defaultSMTPConfig);

    boolean mailSentSuccessFully = false;
    if (!isDefaultSMTPConfigValid) {
      logger.warn("Mail not sent, : {}", getErrorString(emailData));
    } else {
      mailSentSuccessFully = true;
      if (!sendMail(defaultSMTPConfig, emailData)) {
        logger.warn("Mail not sent, : {}", getErrorString(emailData));
        mailSentSuccessFully = false;
      }
    }

    return mailSentSuccessFully;
  }

  private boolean sendMail(SmtpConfig config, EmailData emailData) {
    if (config.equals(mainConfiguration.getSmtpConfig())) {
      try {
        mailer.send(config, emailData);
        return true;
      } catch (WingsException e) {
        String errorString = getErrorString(emailData);
        logger.warn(errorString, e);
        return false;
      }
    } else {
      logger.warn("Mail not sent in sendMail");
      return false;
    }
  }

  private boolean isSmtpConfigValid(SmtpConfig config) {
    return config != null && config.valid();
  }

  private String getErrorString(EmailData emailData) {
    return String.format(
        "Failed to send an email with subject:[%s] , to:%s", emailData.getSubject(), emailData.getTo());
  }
}
