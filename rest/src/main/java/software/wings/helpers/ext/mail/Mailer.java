package software.wings.helpers.ext.mail;

import static freemarker.template.Configuration.VERSION_2_3_23;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

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

import java.io.IOException;
import java.io.StringWriter;

/**
 * Created by peeyushaggarwal on 5/20/16.
 */
public class Mailer {
  private final Configuration cfg = new Configuration(VERSION_2_3_23);

  private final static Logger logger = LoggerFactory.getLogger(Mailer.class);

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
  public void send(SmtpConfig smtpConfig, EmailData emailData) {
    Email email = emailData.isHasHtml() ? new HtmlEmail() : new SimpleEmail();
    email.setHostName(smtpConfig.getHost());
    email.setSmtpPort(smtpConfig.getPort());
    email.setAuthenticator(new DefaultAuthenticator(smtpConfig.getUsername(), new String(smtpConfig.getPassword())));
    email.setSSLOnConnect(smtpConfig.isUseSSL());
    if (smtpConfig.isUseSSL()) {
      email.setSslSmtpPort(Integer.toString(smtpConfig.getPort()));
    }

    try {
      email.setFrom(smtpConfig.getFromAddress(), "Harness Inc");
      for (String to : emailData.getTo()) {
        email.addTo(to);
      }
      for (String cc : emailData.getCc()) {
        email.addCc(cc);
      }

      String subject = emailData.getSubject();
      String body = emailData.getBody();
      if (isNotBlank(emailData.getTemplateName())) {
        Template subjectTemplate = null;
        Template bodyTemplate = cfg.getTemplate(emailData.getTemplateName() + "-body.ftl");

        StringWriter subjectWriter = new StringWriter();
        subjectTemplate.process(emailData.getTemplateModel(), subjectWriter);

        subject = subjectWriter.toString();

        StringWriter bodyWriter = new StringWriter();
        bodyTemplate.process(emailData.getTemplateModel(), bodyWriter);

        body = bodyWriter.toString();
      }

      email.setSubject(subject);
      email.setMsg(body);

      email.send();
    } catch (EmailException | IOException e) {
      logger.warn("Failed to send email. Reason: " + e.getMessage());
    } catch (TemplateException e) {
      logger.warn("Failed to parse email template . Reason: " + e.getMessage());
    }
  }
}
