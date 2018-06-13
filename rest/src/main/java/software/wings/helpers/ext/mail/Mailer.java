package software.wings.helpers.ext.mail;

import static freemarker.template.Configuration.VERSION_2_3_23;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.common.Constants.HARNESS_NAME;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.SimpleEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
import software.wings.exception.WingsException;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.utils.Misc;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

/**
 * Created by peeyushaggarwal on 5/20/16.
 */
public class Mailer {
  private final Configuration cfg = new Configuration(VERSION_2_3_23);
  public static final Logger logger = LoggerFactory.getLogger(Mailer.class);

  @Inject private EncryptionService encryptionService;
  /**
   * Instantiates a new mailer.
   */
  public Mailer() {
    cfg.setTemplateLoader(new ClassTemplateLoader(getClass(), "/mailtemplates"));
  }

  /**
   * Send.
   *
   * @param smtpConfig the smtp config
   * @param emailData  the email data
   * @throws EmailException    the email exception
   * @throws IOException       Signals that an I/O exception has occurred.
   * @throws TemplateException the template exception
   */
  public void send(SmtpConfig smtpConfig, List<EncryptedDataDetail> encryptedDataDetails, EmailData emailData) {
    try {
      encryptionService.decrypt(smtpConfig, encryptedDataDetails);
      Email email = emailData.isHasHtml() ? new HtmlEmail() : new SimpleEmail();
      email.setHostName(smtpConfig.getHost());
      email.setSmtpPort(smtpConfig.getPort());
      if (isNotEmpty(smtpConfig.getPassword())) {
        email.setAuthenticator(
            new DefaultAuthenticator(smtpConfig.getUsername(), new String(smtpConfig.getPassword())));
      }
      email.setSSLOnConnect(smtpConfig.isUseSSL());
      if (smtpConfig.isUseSSL()) {
        email.setSslSmtpPort(Integer.toString(smtpConfig.getPort()));
      }

      try {
        email.setReplyTo(ImmutableList.of(new InternetAddress(smtpConfig.getFromAddress())));
      } catch (AddressException e) {
        logger.error(Misc.getMessage(e), e);
      }
      email.setFrom(smtpConfig.getFromAddress(), HARNESS_NAME);

      for (String to : emailData.getTo()) {
        email.addTo(to);
      }

      if (emailData.getCc() != null) {
        for (String cc : emailData.getCc()) {
          email.addCc(cc);
        }
      }

      String subject = emailData.getSubject();
      String body = emailData.getBody();
      if (isNotBlank(emailData.getTemplateName())) {
        Template subjectTemplate = cfg.getTemplate(emailData.getTemplateName() + "-subject.ftl");
        Template bodyTemplate = cfg.getTemplate(emailData.getTemplateName() + "-body.ftl");

        StringWriter subjectWriter = new StringWriter();
        subjectTemplate.process(emailData.getTemplateModel(), subjectWriter);

        subject = subjectWriter.toString();

        StringWriter bodyWriter = new StringWriter();
        bodyTemplate.process(emailData.getTemplateModel(), bodyWriter);

        body = bodyWriter.toString();
      }

      email.setSubject(subject);
      if (emailData.isHasHtml()) {
        ((HtmlEmail) email).setHtmlMsg(body);
      } else {
        email.setMsg(body);
      }

      email.send();
    } catch (EmailException | IOException e) {
      logger.warn("Failed to send email. Reason: " + e.getMessage());
      throw new WingsException(ErrorCode.EMAIL_FAILED, e);
    } catch (TemplateException e) {
      logger.warn("Failed to parse email template . Reason: " + e.getMessage());
      throw new WingsException(ErrorCode.EMAIL_FAILED, e);
    }
  }
}
