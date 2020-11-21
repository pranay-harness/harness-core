package io.harness.ng.core.invites.ext.mail;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static freemarker.template.Configuration.VERSION_2_3_23;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArgumentsException;
import io.harness.queue.QueuePublisher;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.StringWriter;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.SimpleEmail;

@OwnedBy(PL)
@AllArgsConstructor(onConstructor = @__(@Inject))
@Slf4j
public class MailUtils {
  private static final Configuration cfg = new Configuration(VERSION_2_3_23);
  private static final String HARNESS_NAME = "Harness";
  private final QueuePublisher<EmailData> emailEventQueue;
  private final SmtpConfig smtpConfig;

  static {
    cfg.setTemplateLoader(new ClassTemplateLoader(MailUtils.class, "/mailtemplates"));
  }

  public boolean sendMail(EmailData emailData) {
    try {
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
        log.error(ExceptionUtils.getMessage(e), e);
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
      return true;
    } catch (EmailException e) {
      log.error("Failed to send email. Reason: " + ExceptionUtils.getMessage(e));
    } catch (IOException | TemplateException e) {
      throw new InvalidArgumentsException("Failed to send email.", e, null);
    }
    return false;
  }

  public void sendMailAsync(EmailData emailData) {
    try {
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
      emailData.setSubject(subject);
      emailData.setBody(body);
    } catch (TemplateException | IOException e) {
      throw new InvalidArgumentsException("Failed to send email.", e, null);
    }
    emailEventQueue.send(emailData);
  }

  public void sendMailAsyncConsumer(EmailData emailData) {
    try {
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
        log.error(ExceptionUtils.getMessage(e), e);
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

      email.setSubject(emailData.getSubject());
      if (emailData.isHasHtml()) {
        ((HtmlEmail) email).setHtmlMsg(emailData.getBody());
      } else {
        email.setMsg(emailData.getBody());
      }
      email.send();
    } catch (EmailException e) {
      log.error("Failed to send email. Reason: " + ExceptionUtils.getMessage(e));
    }
  }
}
